package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.history.achievements.*

import net.johnceo.sparklingmutuals.compat.*
import java.time.Instant
import java.time.ZoneId
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestTracker
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.sounds.SoundEvents

/** Serialized tier names with explicit presentation policy; SPECIAL is a novelty category. */
enum class AchievementTier(val code: String, val argb: Int, val displayRank: Int, val fanfare: Boolean = false, val title: Boolean = false) {
    COMMON("§f", 0xFFFFFFFF.toInt(), 0), UNCOMMON("§a", 0xFF55FF55.toInt(), 1), RARE("§9", 0xFF5555FF.toInt(), 2),
    EPIC("§5", 0xFFAA00AA.toInt(), 3), LEGENDARY("§6", 0xFFFFAA00.toInt(), 4, false, true), MYTHIC("§d", 0xFFFF55FF.toInt(), 5, true, true),
    DIVINE("§b", 0xFF55FFFF.toInt(), 6, true, true), SPECIAL("§c", 0xFFFF5555.toInt(), 0)
}

/** Units are rendered through typed progress; unknown coverage never receives a bar. */
enum class Measure { EVENT, RUNS, HOURS, CAUGHT, COINS, OWN_SPARKLINGS, UNIQUES, DRY_SPARKLING, DRY_UNIQUE, RUNS_SINCE_MACAW, CONTESTS, MODELED_SPARKLING, MODELED_UNIQUE, VERIFIED_TICKETS, VERIFIED_CONTESTS }

data class Achievement(val id: String, val name: String, val description: String, val tier: AchievementTier,
    val measure: Measure = Measure.EVENT, val goal: Long = 1, val secret: Boolean = false, val modeledHazardGoal: Double? = null) {
    val hasProgress get() = modeledHazardGoal != null || measure != Measure.EVENT && goal > 1
}

/**
 * Achievements in the style of SkyBlock Overhaul's: each has a rarity, unlocks once with a chat line and a sound (the
 * challenge fanfare for Mythic and Divine), and some stay secret until earned. Most are worked out from the run and
 * sparkling history, topped up from your own Hypixel profile ([OwnStats]); the rest are chat events (an Emerald Dye). The first time it runs it unlocks whatever
 * the existing history already earns, quietly, in one line.
 */
object Achievements {
    val definitions: List<Achievement> = AchievementDefinitions.all
    fun all(): List<Achievement> = definitions

    class Data(var initialised: Boolean = false, var unlocked: MutableMap<String, Long> = linkedMapOf(),
        var emblems: MutableMap<String, Long> = linkedMapOf(), var runsSinceOwnSparkling: Int = 0,
        var longestRunsWithoutSparkling: Int = 0, var runsSinceOwnUnique: Int = 0, var longestRunsWithoutUnique: Int = 0,
        var uniqueSinceLastRun: Boolean = false, var runsSinceMacaw: Int = 0, var longestRunsWithoutMacaw: Int = 0,
        var lastRunHadOwnSparkling: Boolean = false, var ticketsSinceOwnSparkling: Int = 0,
        var contestsCompleted: Int = 0, var lastContestWindow: Long = -1,
        var activity: ActivityEvidence? = ActivityEvidence(), var state: AchievementStateV2? = null)
    private var data = Data()
    val state: AchievementStateV2 get() = data.state ?: AchievementMigration.migrate(data)
    val activity: ActivityEvidence get() = data.state?.activity ?: data.activity ?: ActivityEvidence().also { data.activity = it }
    private var activityScopeKey: String? = null
    private var ticks = 0

    fun currentActivityScope(playerUuid: String): AchievementScope? = OwnStats.dexSnapshot?.scope
        ?.takeIf { normalizePlayer(it.playerUuid) == normalizePlayer(playerUuid) }

    /** Scope changes and offline gaps end prospective streak continuity. */
    fun refreshDex(snapshot: DexSnapshot?) {
        ContestTracker.selectScope(snapshot?.scope?.key)
        val previous = activityScopeKey
        if (snapshot == null || previous != snapshot.scope.key) {
            state.droughtByScope.values.forEach { it.invalidateScope() }
            invalidateActivityIntervals()
        }
        activityScopeKey = snapshot?.scope?.key
        snapshot?.let { state.droughtByScope.getOrPut(it.scope.key) { DroughtEvidence(it.scope) }.refreshDex(it) }
        net.johnceo.sparklingmutuals.safari.SafariTracking.ledger.current?.refreshAchievementDex(snapshot)
        save()
    }

    fun invalidateActivityIntervals() { activity.invalidateIntervals(); activityScopeKey = null; save() }

    fun collectAdmission(runId: String, scope: AchievementScope?, at: Long): Boolean {
        val added = if (scope == null) activity.recordUnscopedAdmission(runId, at)
            else activity.recordAdmission(scope, runId, at)
        if (added) save()
        return added
    }
    fun collectContest(playerUuid: String, window: Long, scope: AchievementScope?, at: Long): Boolean {
        if (!activity.recordContest(playerUuid, window, scope, at)) return false
        data.lastContestWindow = window
        data.contestsCompleted++
        save()
        return true
    }

    private val emeraldDrop = Regex("^(?:[A-Z!' ]*DROP!|WOW!|RNG METER|NICE!)", RegexOption.IGNORE_CASE)

    fun register() {
        data = AchievementPersistence.load().getOrElse { Data() }
        // Offline gaps cannot prove an uninterrupted personal ticket interval.
        invalidateActivityIntervals()
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { SafariMessages.lines(message.string).forEach(::chat) }
        }
    }

    // ------------------------------------------------------------------ events

    fun tick(client: Minecraft) {
        val player = client.player ?: return
        val scope = currentActivityScope(player.uuid.toString())
        if (scope?.key != activityScopeKey) {
            invalidateActivityIntervals()
            activityScopeKey = scope?.key
        }
        val newContest = ContestTracker.completedWindow()?.let {
            collectContest(player.uuid.toString(), it, scope, System.currentTimeMillis())
        } ?: false
        if (!ConfigManager.achievements) return
        if (!data.initialised) { initialise(client); return }
        if (newContest) checkAll(true)
        if (++ticks % 200 == 0) checkAll(true)
    }

    fun collectOwnSparkling(scope: AchievementScope?, at: Long, announce: Boolean) {
        if (scope == null) activity.invalidateIntervals()
        else if (activity.recordOwnSparkling(scope, at)) {
            state.eligibleEvents.putIfAbsent("instant_v2", at)
            unlock("instant_v2", announce)
        }
        save()
    }

    fun onSparkling(entry: SparklingEntry, own: Boolean) {
        if (own) {
            val scope = Minecraft.getInstance().player?.uuid?.toString()?.let(::currentActivityScope)
            collectOwnSparkling(scope, entry.at, true)
            data.ticketsSinceOwnSparkling = 0; data.runsSinceOwnSparkling = 0
            if (entry.critter in setOf("Macaw", "Wumpa", "Doomspiral")) unlock("legendary_sparkle", true)
            if (entry.critter == "Gemzie") unlock("gemzie_sparkle", true)
            save()
        }
        checkAll(true)
    }
    fun onUnique(own: Boolean) {
        if (own) { data.runsSinceOwnUnique = 0; data.uniqueSinceLastRun = true; save() }
        checkAll(true)
    }
    fun onTicket() { data.ticketsSinceOwnSparkling++; save() }

    fun onRunEnd(run: RunRecord) {
        run.achievementEvidence?.let { evidence ->
            state.droughtByScope.getOrPut(evidence.scope.key) { DroughtEvidence(evidence.scope) }
                .recordRun(evidence)
        }
        data.runsSinceMacaw = if (run.macaws > 0) 0 else data.runsSinceMacaw + 1
        data.longestRunsWithoutMacaw = maxOf(data.longestRunsWithoutMacaw, data.runsSinceMacaw)
        val own = run.ownSparklings > 0
        if (!own) data.runsSinceOwnSparkling++
        data.longestRunsWithoutSparkling = maxOf(data.longestRunsWithoutSparkling, data.runsSinceOwnSparkling)
        if (!data.uniqueSinceLastRun) data.runsSinceOwnUnique++
        data.longestRunsWithoutUnique = maxOf(data.longestRunsWithoutUnique, data.runsSinceOwnUnique)
        data.uniqueSinceLastRun = false
        if (own && data.lastRunHadOwnSparkling) unlock("back_to_back", true)
        data.lastRunHadOwnSparkling = own
        save()
        checkRun(run, true)
        checkAll(true)
    }

    private fun chat(text: String) {
        if ("Emerald Dye" in text && ownDrop(text)) unlock("emerald_dye", true)
    }
    /** Your drop, not a broadcast of someone else's ("WOW! Name found Emerald Dye"). */
    fun ownDrop(text: String, self: String = Minecraft.getInstance()?.player?.name?.string.orEmpty()): Boolean {
        if (!emeraldDrop.containsMatchIn(text) && "You found" !in text) return false
        val found = text.indexOf(" found ")
        if (found < 0) return true
        val before = text.substring(0, found)
        return before.endsWith("You") || self.isNotEmpty() && before.endsWith(self)
    }

    // ------------------------------------------------------------------ checking

    private fun checkAll(announce: Boolean) {
        if (!data.initialised) return
        state.eligibleEvents.keys.toList().forEach { unlock(it, false) }
        for (achievement in definitions)
            if (achievement.measure != Measure.EVENT && !isUnlocked(achievement) && progressState(achievement).reached) unlock(achievement.id, announce)
        val dex = ownDex()
        if (dex.isNotEmpty()) unlock("first_sparkle", announce)
        for (biome in SafariBiome.entries) if (dex.containsAll(biome.critters.map { it.name }))
            unlock("set_${biome.name.lowercase()}", announce)
    }

    private fun checkRun(run: RunRecord, announce: Boolean) {
        if (run.sparklings.size >= 2) unlock("party_double", announce)
        if (run.sparklings.size >= 3) unlock("party_triple", announce)
        if (run.ownSparklings >= 3) unlock("own_triple", announce)
        if (run.macaws >= 3) unlock("macaw_3", announce)
        if (run.macaws >= 4) unlock("macaw_4", announce)
        if (run.macaws >= 8) unlock("macaw_8", announce)
        val all = SafariRoster.all.size
        if (run.party >= all) unlock("full_house", announce)
        if (run.party >= all && run.length < 180_000) unlock("speed_3", announce)
        if (run.party >= all && run.length < 150_000) unlock("speed_230", announce)
        if (run.you >= 10) unlock("ten_of_37", announce)
        val zone = ZoneId.systemDefault()
        if (Instant.ofEpochMilli(run.start + run.length).atZone(zone).hour in 3..4) unlock("night_owl", announce)
        val day = Instant.ofEpochMilli(run.start).atZone(zone).toLocalDate()
        if (RunHistory.runs.count { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() == day } >= 50) unlock("dedication", announce)
    }

    fun legacyAwards(): List<LegacyAward> = state.legacyAwards.values.toList()

    fun progressState(achievement: Achievement): AchievementProgress {
        val scope = OwnStats.dexSnapshot?.scope
        val goal = achievement.modeledHazardGoal ?: achievement.goal.toDouble()
        return when (achievement.measure) {
            Measure.MODELED_SPARKLING, Measure.MODELED_UNIQUE -> {
                val evidence = scope?.let { state.droughtByScope[it.key] }
                val start = evidence?.coverageStart
                val value = if (start == null) null else if (achievement.measure == Measure.MODELED_UNIQUE)
                    evidence.bestUniqueHazard else evidence.bestOrdinaryHazard
                AchievementProgress(value, goal, "modeled hazard", if (start == null) "Unknown coverage" else "Recorded since ${coverageDate(start)}; personal observations only", true)
            }
            Measure.VERIFIED_TICKETS, Measure.VERIFIED_CONTESTS -> {
                val start = scope?.let(activity::coverageStart)
                val value = if (scope == null || start == null) null else if (achievement.measure == Measure.VERIFIED_TICKETS)
                    activity.admissions(scope).toDouble() else activity.contests(scope).toDouble()
                val unit = if (achievement.measure == Measure.VERIFIED_TICKETS) "personal tickets" else "qualifying contests"
                AchievementProgress(value, goal, unit, if (value == null) "Unknown coverage" else "Recorded lower bound since ${coverageDate(start!!)}", false)
            }
            else -> {
                val unit = when (achievement.measure) {
                    Measure.RUNS, Measure.RUNS_SINCE_MACAW, Measure.DRY_SPARKLING, Measure.DRY_UNIQUE -> "recorded runs"
                    Measure.HOURS -> "hours"
                    Measure.CAUGHT -> "captures"
                    Measure.COINS -> "estimated coins"
                    Measure.OWN_SPARKLINGS -> "own sparklings"
                    Measure.UNIQUES -> "species"
                    else -> "events"
                }
                val coverage = if (achievement.measure == Measure.CAUGHT) "Mixed lower bound: max(shared logs, own API); never summed" else "Available history"
                AchievementProgress(progress(achievement).toDouble(), goal, unit, coverage, false)
            }
        }
    }
    private fun coverageDate(at: Long) = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    /** Historical integer measures only; coverage-aware/model consumers must use progressState. */
    fun progress(achievement: Achievement): Long = when (achievement.measure) {
        Measure.EVENT -> if (isUnlocked(achievement)) 1 else 0
        // Hypixel's bestiary also counts runs and catches from before the mod was installed.
        Measure.RUNS -> maxOf(RunHistory.runs.size.toLong(), OwnStats.runs)
        Measure.HOURS -> PlaytimeReader.safariMillis() / 3_600_000L
        Measure.CAUGHT -> maxOf(RunHistory.runs.sumOf { (it.caught + it.lootShares).toLong() }, OwnStats.caught)
        Measure.COINS -> RunHistory.runs.sumOf { maxOf(0L, it.coins) }
        Measure.OWN_SPARKLINGS -> ownEntries().size.toLong()
        Measure.UNIQUES -> ownDex().size.toLong()
        Measure.DRY_SPARKLING -> maxOf(data.longestRunsWithoutSparkling, data.runsSinceOwnSparkling).toLong()
        Measure.DRY_UNIQUE -> maxOf(data.longestRunsWithoutUnique, data.runsSinceOwnUnique).toLong()
        Measure.RUNS_SINCE_MACAW -> maxOf(data.longestRunsWithoutMacaw, data.runsSinceMacaw).toLong()
        Measure.CONTESTS -> data.contestsCompleted.toLong()
        else -> error("Use typed progress for modeled or coverage-aware achievements")
    }

    fun isUnlocked(achievement: Achievement) = achievement.id in state.currentUnlocks
    fun unlockedAt(achievement: Achievement) = state.currentUnlocks[achievement.id] ?: 0L

    /** With achievements off, progress still counts but nothing unlocks or is announced; it catches up when turned on. */
    private fun unlock(id: String, announce: Boolean) {
        if (id in state.currentUnlocks || id in LegacyAchievementMetadata.replacedIds) return
        if (!ConfigManager.achievements) {
            if (definitions.any { it.id == id }) state.eligibleEvents.putIfAbsent(id, System.currentTimeMillis())
            save()
            return
        }
        val achievement = definitions.firstOrNull { it.id == id } ?: return
        state.currentUnlocks[id] = System.currentTimeMillis()
        save()
        if (announce) announce(achievement)
    }

    /** "ACHIEVEMENT UNLOCKED! Macaw Mania" in the tier's colour, the description on hover. */
    private fun announce(achievement: Achievement) {
        if (!ConfigManager.achievements) return
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val tier = achievement.tier
        val hover = Component.literal("${tier.code}§l${achievement.name}\n§7${achievement.description}\n\n${tier.code}§l${tier.name} ACHIEVEMENT")
        player.sendSystemMessage(LocalChat.component("[SM] §6§lACHIEVEMENT UNLOCKED! ${tier.code}§l${achievement.name}").copy()
            .withStyle { it.withHoverEvent(HoverEvent.ShowText(hover)) })
        player.sendSystemMessage(LocalChat.component("[SM]   §7${achievement.description} §8(${tier.code}${tier.name}§8)"))
        if (ConfigManager.achievementSound) {
            // Mythic and Divine get the challenge-advancement fanfare; the rest a lighter chime.
            if (tier.fanfare) player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
            else player.playSound(SoundEvents.PLAYER_LEVELUP, .6f, 1.6f)
        }
        if (tier.title) {
            client.gui.smSetTimes(5, 50, 15)
            client.gui.smSetSubtitle(Component.literal("${tier.code}${achievement.name}"))
            client.gui.smSetTitle(Component.literal("§6§lACHIEVEMENT"))
        }
        if (ConfigManager.achievementsToParty) player.connection.sendCommand("pc Achievement unlocked: ${achievement.name}!")
    }

    /** First run: unlock what the history already earns, quietly, and say how many in one line. */
    private fun initialise(client: Minecraft) {
        data.initialised = true
        val before = state.currentUnlocks.size
        afterImport()
        val earned = state.currentUnlocks.size - before
        if (earned > 0) client.player?.sendSystemMessage(LocalChat.component(
            "[SM] Achievements are here! Your history already earns §e$earned§r of them. See /sm achievements."))
    }

    /**
     * After history is imported: streaks are worked out again from the whole run history (they only grow), and every
     * run and catch is checked, quietly. Returns how many unlocked.
     */
    fun afterImport(): Int {
        if (!data.initialised) return 0
        val before = state.currentUnlocks.size
        var noMacaw = 0; var noSparkle = 0; var lastOwn = false
        for (run in RunHistory.runs.sortedBy { it.start }) {
            noMacaw = if (run.macaws > 0) 0 else noMacaw + 1
            data.longestRunsWithoutMacaw = maxOf(data.longestRunsWithoutMacaw, noMacaw)
            noSparkle = if (run.ownSparklings > 0) 0 else noSparkle + 1
            data.longestRunsWithoutSparkling = maxOf(data.longestRunsWithoutSparkling, noSparkle)
            if (run.ownSparklings > 0 && lastOwn) unlock("back_to_back", false)
            lastOwn = run.ownSparklings > 0
            checkRun(run, false)
        }
        if (RunHistory.runs.isNotEmpty()) { data.runsSinceMacaw = noMacaw; data.runsSinceOwnSparkling = noSparkle; data.lastRunHadOwnSparkling = lastOwn }
        for (entry in ownEntries()) {
            if (entry.critter in setOf("Macaw", "Wumpa", "Doomspiral")) unlock("legendary_sparkle", false)
            if (entry.critter == "Gemzie") unlock("gemzie_sparkle", false)
        }
        checkAll(false)
        save()
        return state.currentUnlocks.size - before
    }

    // ------------------------------------------------------------------ reading

    fun ownEntries(): List<SparklingEntry> {
        val self = Minecraft.getInstance()?.player?.name?.string ?: return emptyList()
        return SparklingLog.entries.filter { it.player.equals(self, true) }
    }
    /** Your sparkling dex: your history, plus your Sparkling Critterdex from the API. */
    fun ownDex(): Set<String> = ownEntries().mapNotNull { SafariRoster.named(it.critter)?.name }.toSet() + OwnStats.dex

    /** Your profile was read from the API: whatever its runs, catches and dex earn is unlocked quietly. Returns how many. */
    fun afterApiStats(): Int {
        if (!data.initialised || !ConfigManager.achievements) return 0
        val before = state.currentUnlocks.size
        checkAll(false)
        save()
        return state.currentUnlocks.size - before
    }
    val longestDrySparkling get() = maxOf(data.longestRunsWithoutSparkling, data.runsSinceOwnSparkling)
    val longestMacawDrought get() = maxOf(data.longestRunsWithoutMacaw, data.runsSinceMacaw)
    val runsSinceMacaw get() = data.runsSinceMacaw
    val contestsCompleted get() = data.contestsCompleted
    fun save() = AchievementPersistence.save(data)
}
