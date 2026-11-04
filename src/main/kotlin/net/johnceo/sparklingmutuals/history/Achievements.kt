package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.party.PartyManager
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
import net.johnceo.sparklingmutuals.ui.AlertSounds

/** Rarity, lowest first, with its chat colour and ARGB. */
enum class AchievementTier(val code: String, val argb: Int) {
    COMMON("§f", 0xFFFFFFFF.toInt()), UNCOMMON("§a", 0xFF55FF55.toInt()), RARE("§9", 0xFF5555FF.toInt()),
    EPIC("§5", 0xFFAA00AA.toInt()), LEGENDARY("§6", 0xFFFFAA00.toInt()), MYTHIC("§d", 0xFFFF55FF.toInt()),
    DIVINE("§b", 0xFF55FFFF.toInt()), SPECIAL("§c", 0xFFFF5555.toInt())
}

/** What an achievement is measured against; anything but EVENT gets a progress bar. */
enum class Measure { EVENT, RUNS, HOURS, CAUGHT, COINS, OWN_SPARKLINGS, UNIQUES, DRY_SPARKLING, DRY_UNIQUE, RUNS_SINCE_MACAW, CONTESTS, DUPLICATES, DEX_SETS, DYES, DAILY_RUNS }

data class Achievement(val id: String, val name: String, val description: String, val tier: AchievementTier,
    val measure: Measure = Measure.EVENT, val goal: Long = 1, val secret: Boolean = false) {
    val hasProgress get() = measure != Measure.EVENT && goal > 1
}

/**
 * Achievements in the style of SkyBlock Overhaul's: each has a rarity, unlocks once with a chat line and a sound (the
 * challenge fanfare for Mythic and Divine), and some stay secret until earned. Most are worked out from the run and
 * sparkling history, topped up from your own Hypixel profile ([OwnStats]); the rest are chat events (an Emerald Dye). The first time it runs it unlocks whatever
 * the existing history already earns, quietly, in one line.
 */
object Achievements {
    private val tiers = AchievementTier.entries
    internal val revampedIds=setOf("first_sparkle","dry_runs_10","dry_runs_20","dry_runs_30","dry_runs_40","dry_runs_50","no_unique_runs_5","no_unique_runs_10","no_unique_runs_25","no_unique_runs_50","no_unique_runs_250","macaw_3","macaw_4","no_macaw_3","no_macaw_5","no_macaw_8","run_10","run_50","run_100","run_500","hours_10","hours_50","party_caught_1000","party_caught_5000","party_caught_10000","party_caught_25000","party_caught_50000","coins_100m","coins_1b","speed_3","speed_230","ten_of_37","contests_10","contests_50","contests_100","dedication","instant")
    val definitions: List<Achievement> = listOf(
        Achievement("first_sparkle", "First Sparkling", "Catch your first sparkling critter.", AchievementTier.COMMON),
        Achievement("party_double", "Shared Shine", "Your party catches 2 sparklings in one run.", AchievementTier.MYTHIC, Measure.EVENT, 2L),
        Achievement("party_triple", "Sparkle Storm", "Your party catches 3 sparklings in one run.", AchievementTier.DIVINE, Measure.EVENT, 3L),
        Achievement("own_triple", "Hat Trick", "Catch 3 sparklings yourself in one run.", AchievementTier.SPECIAL, Measure.EVENT, 3L),
        Achievement("sparkle_10", "Glimmer", "Catch 10 of your own sparklings.", AchievementTier.EPIC, Measure.OWN_SPARKLINGS, 10L),
        Achievement("sparkle_25", "Gleam", "Catch 25 of your own sparklings.", AchievementTier.LEGENDARY, Measure.OWN_SPARKLINGS, 25L),
        Achievement("sparkle_50", "Radiance", "Catch 50 of your own sparklings.", AchievementTier.MYTHIC, Measure.OWN_SPARKLINGS, 50L),
        Achievement("sparkle_100", "Supernova", "Catch 100 of your own sparklings.", AchievementTier.DIVINE, Measure.OWN_SPARKLINGS, 100L),
        Achievement("unique_10", "Collector", "Unlock 10 sparkling critter types in your sparkling dex.", AchievementTier.EPIC, Measure.UNIQUES, 10L),
        Achievement("unique_20", "Curator", "Unlock 20 sparkling critter types in your sparkling dex.", AchievementTier.LEGENDARY, Measure.UNIQUES, 20L),
        Achievement("unique_30", "Archivist", "Unlock 30 sparkling critter types in your sparkling dex.", AchievementTier.MYTHIC, Measure.UNIQUES, 30L),
        Achievement("max_sparklings", "Max Sparklings", "Unlock all 37 sparkling types in your sparkling dex.", AchievementTier.DIVINE, Measure.UNIQUES, 37L),
        Achievement("legendary_sparkle", "Legend in Lights", "Catch a sparkling Macaw, Wumpa or Doomspiral yourself.", AchievementTier.MYTHIC),
        Achievement("gemzie_sparkle", "Gem of a Gemzie", "Catch a sparkling Gemzie yourself.", AchievementTier.LEGENDARY),
        Achievement("set_forest", "Forest Glitter", "Unlock every Forest sparkling in your sparkling dex.", AchievementTier.MYTHIC),
        Achievement("set_cavern", "Cavern Glitter", "Unlock every Cavern sparkling in your sparkling dex.", AchievementTier.MYTHIC),
        Achievement("set_icy", "Icy Glitter", "Unlock every Icy sparkling in your sparkling dex.", AchievementTier.MYTHIC),
        Achievement("set_haunted", "Haunted Glitter", "Unlock every Haunted sparkling in your sparkling dex.", AchievementTier.MYTHIC),
        Achievement("dry_runs_10", "Unlucky", "25 runs in a row without a sparkling.", AchievementTier.RARE, Measure.DRY_SPARKLING, 25L),
        Achievement("dry_runs_20", "Very Unlucky", "50 runs in a row without a sparkling.", AchievementTier.EPIC, Measure.DRY_SPARKLING, 50L),
        Achievement("dry_runs_30", "Cursed", "75 runs in a row without a sparkling.", AchievementTier.LEGENDARY, Measure.DRY_SPARKLING, 75L),
        Achievement("dry_runs_40", "Forsaken", "100 runs in a row without a sparkling.", AchievementTier.MYTHIC, Measure.DRY_SPARKLING, 100L),
        Achievement("dry_runs_50", "Doomed", "150 runs in a row without a sparkling.", AchievementTier.DIVINE, Measure.DRY_SPARKLING, 150L),
        Achievement("no_unique_runs_5", "Same Old Sparkle", "Get 10 duplicate sparkling critters without a new unique sparkling critter.", AchievementTier.RARE, Measure.DUPLICATES, 10L),
        Achievement("no_unique_runs_10", "Deja Vu", "Get 25 duplicate sparkling critters without a new unique sparkling critter.", AchievementTier.EPIC, Measure.DUPLICATES, 25L),
        Achievement("no_unique_runs_25", "Groundhog Day", "Get 50 duplicate sparkling critters without a new unique sparkling critter.", AchievementTier.LEGENDARY, Measure.DUPLICATES, 50L),
        Achievement("no_unique_runs_50", "Broken Record", "Get 75 duplicate sparkling critters without a new unique sparkling critter.", AchievementTier.MYTHIC, Measure.DUPLICATES, 75L),
        Achievement("no_unique_runs_250", "Eternal Rerun", "Get 100 duplicate sparkling critters without a new unique sparkling critter.", AchievementTier.DIVINE, Measure.DUPLICATES, 100L),
        Achievement("macaw_3", "Bird Watcher", "Your party catches 4 Macaws in one run.", AchievementTier.LEGENDARY, Measure.EVENT, 4L),
        Achievement("macaw_4", "Flock Together", "Your party catches 6 Macaws in one run.", AchievementTier.MYTHIC, Measure.EVENT, 6L),
        Achievement("macaw_8", "Macaw Mania", "Your party catches 8 Macaws in one run.", AchievementTier.DIVINE, Measure.EVENT, 8L),
        Achievement("no_macaw_3", "Where's the Macaw?", "10 runs in a row without a Macaw.", AchievementTier.LEGENDARY, Measure.RUNS_SINCE_MACAW, 10L),
        Achievement("no_macaw_5", "Macaw Drought", "20 runs in a row without a Macaw.", AchievementTier.MYTHIC, Measure.RUNS_SINCE_MACAW, 20L),
        Achievement("no_macaw_8", "Extinct", "30 runs in a row without a Macaw.", AchievementTier.DIVINE, Measure.RUNS_SINCE_MACAW, 30L),
        Achievement("emerald_dye", "Emerald Dye", "Find an Emerald Dye.", AchievementTier.LEGENDARY, Measure.DYES, 1L),
        Achievement("run_10", "Safari Rookie", "Complete 50 Safari runs.", AchievementTier.RARE, Measure.RUNS, 50L),
        Achievement("run_50", "Regular", "Complete 250 Safari runs.", AchievementTier.EPIC, Measure.RUNS, 250L),
        Achievement("run_100", "Ranger", "Complete 500 Safari runs.", AchievementTier.LEGENDARY, Measure.RUNS, 500L),
        Achievement("run_500", "Warden of the Wilds", "Complete 2000 Safari runs.", AchievementTier.MYTHIC, Measure.RUNS, 2000L),
        Achievement("hours_10", "Time Flies", "Spend 25 hours in Safari.", AchievementTier.RARE, Measure.HOURS, 25L),
        Achievement("hours_50", "No Life", "Spend 200 hours in Safari.", AchievementTier.MYTHIC, Measure.HOURS, 200L),
        Achievement("party_caught_1000", "Catcher", "10,000 critter catches.", AchievementTier.RARE, Measure.CAUGHT, 10000L),
        Achievement("party_caught_5000", "Trapper", "50,000 critter catches.", AchievementTier.EPIC, Measure.CAUGHT, 50000L),
        Achievement("party_caught_10000", "Exterminator", "100,000 critter catches.", AchievementTier.LEGENDARY, Measure.CAUGHT, 100000L),
        Achievement("party_caught_25000", "Critter Apocalypse", "250,000 critter catches.", AchievementTier.MYTHIC, Measure.CAUGHT, 250000L),
        Achievement("party_caught_50000", "Extinction Event", "500,000 critter catches.", AchievementTier.DIVINE, Measure.CAUGHT, 500000L),
        Achievement("coins_100m", "Shard Shark", "Reach 1B in profit.", AchievementTier.MYTHIC, Measure.COINS, 1000000000L),
        Achievement("coins_1b", "Shard Tycoon", "Reach 10B in profit.", AchievementTier.DIVINE, Measure.COINS, 10000000000L),
        Achievement("full_house", "Full House", "Your party catches all 37 critters in one run.", AchievementTier.RARE, Measure.EVENT, 37L),
        Achievement("speed_3", "Speedrunner", "Your party catches all 37 critters in under 2:30.", AchievementTier.EPIC, Measure.EVENT, 150000L),
        Achievement("speed_230", "Any%", "Your party catches all 37 critters in under 2:00.", AchievementTier.LEGENDARY, Measure.EVENT, 120000L),
        Achievement("ten_of_37", "Pulling Your Weight", "Catch 20 unique critters yourself in a run.", AchievementTier.RARE, Measure.EVENT, 20L),
        Achievement("contests_10", "Contestant", "Complete 100 Miria's contests.", AchievementTier.EPIC, Measure.CONTESTS, 100L),
        Achievement("contests_50", "Honey Hoarder", "Complete 500 Miria's contests.", AchievementTier.LEGENDARY, Measure.CONTESTS, 500L),
        Achievement("contests_100", "Miria's Favourite", "Complete 2000 Miria's contests.", AchievementTier.MYTHIC, Measure.CONTESTS, 2000L),
        Achievement("night_owl", "Night Owl", "Finish a run between 3 and 5 in the morning.", AchievementTier.UNCOMMON),
        Achievement("dedication", "Dedication", "Finish 75 (>2m) runs in one day.", AchievementTier.EPIC, Measure.DAILY_RUNS, 75L),
        Achievement("back_to_back", "Back to Back", "Catch a sparkling critter two runs in a row.", AchievementTier.MYTHIC, Measure.EVENT, 2L),
        Achievement("instant", "Instant Gratification", "Catch a sparkling within 5 runs of your last catch.", AchievementTier.LEGENDARY, Measure.EVENT, 5L),
        Achievement("cj_luck", "CJ Luck", "Get 200 duplicate sparkling critters without a new unique sparkling critter.", AchievementTier.SPECIAL, Measure.DUPLICATES, 200L, secret = true),
        Achievement("dyes_3", "Dripped Out", "Find 3 Emerald Dyes.", AchievementTier.MYTHIC, Measure.DYES, 3L),
        Achievement("dyes_8", "Dye Hotbar", "Find 8 Emerald Dyes.", AchievementTier.DIVINE, Measure.DYES, 8L),
        Achievement("run_4000", "Gaias Envoy", "Complete 4000 Safari runs.", AchievementTier.DIVINE, Measure.RUNS, 4000L),
        Achievement("hours_50_locked", "Locked In", "Spend 50 hours in Safari.", AchievementTier.EPIC, Measure.HOURS, 50L),
        Achievement("hours_100", "Only Down From Here", "100 hours in Safari.", AchievementTier.LEGENDARY, Measure.HOURS, 100L),
        Achievement("hours_400", "Do You Even Know What Grass Is?", "Spend 400 hours in Safari.", AchievementTier.DIVINE, Measure.HOURS, 400L),
        Achievement("hours_1000", "This Is Water… Use It.", "Spend 1000 hours in Safari.", AchievementTier.SPECIAL, Measure.HOURS, 1000L, secret = true),
        Achievement("speed_130", "G.O.A.T", "Your party catches all 37 critters in under 1:30.", AchievementTier.MYTHIC, Measure.EVENT, 90000L),
        Achievement("daily_150", "No Life²", "Finish 150 (>2m) runs in one day.", AchievementTier.LEGENDARY, Measure.DAILY_RUNS, 150L),
        Achievement("dex_2", "Dupedex", "2 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 2L),
        Achievement("dex_3", "Tripdex", "3 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 3L),
        Achievement("dex_4", "Quaddex", "4 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 4L),
        Achievement("dex_5", "Quindex", "5 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 5L),
        Achievement("dex_6", "Sexdex (nice)", "6 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 6L),
        Achievement("dex_7", "Septdex", "7 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 7L),
        Achievement("dex_8", "Octodex", "8 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 8L),
        Achievement("dex_9", "Nuevadex", "9 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 9L),
        Achievement("dex_10", "Decadex", "10 of every sparkling critter type caught.", AchievementTier.DIVINE, Measure.DEX_SETS, 10L),
        Achievement("double_trouble", "Double Trouble", "Catch 2 of the same sparkling critter type in 1 run.", AchievementTier.SPECIAL, Measure.EVENT, 2L, secret = true),
    )
    fun all(): List<Achievement> = definitions
    fun explanation(achievement: Achievement): String = if(achievement.measure==Measure.RUNS_SINCE_MACAW)
        "Must use all 9 bird food." else ""

    class Data(var initialised: Boolean = false, var unlocked: MutableMap<String, Long> = linkedMapOf(),
        var emblems: MutableMap<String, Long> = linkedMapOf(), var runsSinceOwnSparkling: Int = 0,
        var longestRunsWithoutSparkling: Int = 0, var runsSinceOwnUnique: Int = 0, var longestRunsWithoutUnique: Int = 0,
        var uniqueSinceLastRun: Boolean = false, var runsSinceMacaw: Int = 0, var longestRunsWithoutMacaw: Int = 0,
        var lastRunHadOwnSparkling: Boolean = false, var ticketsSinceOwnSparkling: Int = 0,
        var contestsCompleted: Int = 0, var lastContestWindow: Long = -1, var partyStreakVersion: Int = 0,
        var catalogVersion: Int = 0,var emeraldDyes: Int = 0)
    private var data = Data()
    private const val CATALOG_VERSION=2
    private var pendingHistoryCheck=true
    private val dyeProofs=DyeDropProofs()
    private var sparklingRevision=-1L
    private var cachedDiscoveries: Set<String> = emptySet()
    private var counts: Map<String,Int> = emptyMap()
    private var duplicates=AchievementRules.Streak(0,0)
    private fun refreshSparklingSummary() {
        if(sparklingRevision==SparklingLog.revision && cachedDiscoveries==OwnStats.dex) return
        sparklingRevision=SparklingLog.revision;cachedDiscoveries=OwnStats.dex
        counts=AchievementRules.dexCounts(SparklingLog.entries,cachedDiscoveries)
        duplicates=AchievementRules.duplicateStreak(SparklingLog.entries)
    }
    private var ticks = 0

    private val emeraldDrop = Regex("^(?:[A-Z!' ]*DROP!|WOW!|RNG METER|NICE!)", RegexOption.IGNORE_CASE)

    fun register() {
        data = HistoryStore.load("achievements", Data::class.java, ::Data)
        pendingHistoryCheck=true
        if(data.catalogVersion<CATALOG_VERSION) {
            if(data.unlocked.isNotEmpty()) runCatching { HistoryStore.backup("achievements","pre-requirement-revamp") }
                .onFailure { org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/achievements").warn("Couldn't back up old achievement requirements",it) }
            migrateRequirements(data)
            save()
        }
        // Achievements that were removed (emblems, the 10-run Macaw drought) no longer count.
        data.unlocked.keys.retainAll(definitions.map { it.id }.toSet())
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { SafariMessages.lines(message.string).forEach(::chat) }
        }
    }

    internal fun migrateRequirements(saved: Data): Boolean {
        if(saved.catalogVersion>=CATALOG_VERSION) return false
        if("emerald_dye" in saved.unlocked) saved.emeraldDyes=maxOf(saved.emeraldDyes,1)
        saved.unlocked.keys.removeAll(revampedIds)
        saved.catalogVersion=CATALOG_VERSION
        return true
    }
    private fun goal(id: String)=definitions.first { it.id==id }.goal

    // ------------------------------------------------------------------ events

    private val localAnnouncements=DelayedAchievementChat<Component>()
    private var announcementOwner: java.util.UUID?=null

    fun resetAnnouncements() {
        localAnnouncements.clear()
        announcementOwner=null
    }

    fun sparklingChatReceived() { localAnnouncements.postpone(System.currentTimeMillis()) }

    fun queueLocal(vararg messages: Component) {
        val player=Minecraft.getInstance().player ?: return
        if(!ConfigManager.achievements) return
        if(announcementOwner!=player.uuid) resetAnnouncements()
        announcementOwner=player.uuid
        localAnnouncements.add(messages.toList(),System.currentTimeMillis())
    }

    fun tick(client: Minecraft) {
        val player=client.player
        if (player == null || !ConfigManager.achievements) { resetAnnouncements();return }
        if(announcementOwner!=player.uuid) resetAnnouncements()
        localAnnouncements.drain(System.currentTimeMillis()).forEach(player::sendSystemMessage)
        if (!data.initialised) { pendingHistoryCheck=false;initialise(); return }
        if(pendingHistoryCheck) { pendingHistoryCheck=false;afterImport() }
        if (data.partyStreakVersion < 1) { refreshPartyStreaks(); save() }
        // A contest is counted once per window, however many times the sidebar shows it done. The window comes from
        // the tracker with its completion, so a completion from just before a boundary can't count for the next one.
        ContestTracker.completedWindow()?.let { window ->
            if (window != data.lastContestWindow) { data.lastContestWindow = window; data.contestsCompleted++; save(); checkAll(true) }
        }
        if (++ticks % 200 == 0) checkAll(true)
    }

    fun onSparkling(entry: SparklingEntry, own: Boolean) {
        data.runsSinceOwnSparkling = 0
        if (own) {
            if(personalRunGaps().any { it.toLong() in 0..goal("instant") }) unlock("instant",true)
            val current=net.johnceo.sparklingmutuals.safari.SafariTracking.ledger.current
            if(current!=null && ownEntries().filter { it.at in current.startedAt..(current.endedAt ?: entry.at) }
                    .groupingBy { it.critter }.eachCount().values.any { it>=goal("double_trouble") }) unlock("double_trouble",true)
            data.ticketsSinceOwnSparkling = 0; data.runsSinceOwnSparkling = 0
            if (entry.critter in setOf("Macaw", "Wumpa", "Doomspiral")) unlock("legendary_sparkle", true)
            if (entry.critter == "Gemzie") unlock("gemzie_sparkle", true)
        }
        save()
        checkAll(true)
    }
    fun onUnique(@Suppress("UNUSED_PARAMETER") own: Boolean) {
        data.runsSinceOwnUnique = 0; data.uniqueSinceLastRun = true; save()
        checkAll(true)
    }
    fun onTicket() { data.ticketsSinceOwnSparkling++; save() }

    fun onRunEnd(run: RunRecord) {
        data.runsSinceMacaw = if (run.macaws > 0) 0 else data.runsSinceMacaw + 1
        data.longestRunsWithoutMacaw = maxOf(data.longestRunsWithoutMacaw, data.runsSinceMacaw)
        val own = run.ownSparklings > 0
        refreshPartyStreaks()
        data.uniqueSinceLastRun = false
        if (own && data.lastRunHadOwnSparkling) unlock("back_to_back", true)
        data.lastRunHadOwnSparkling = own
        save()
        checkRun(run, true)
        checkAll(true)
    }

    private fun chat(text: String) {
        if (!ConfigManager.achievements) return
        if ("Emerald Dye" in text && ownDrop(text) && HistoryServerPolicy.allows()) {
            val now=System.currentTimeMillis()
            if(dyeProofs.accept(text,now)) { data.emeraldDyes++;save();checkAll(true) }
        }
    }
    /** Your drop, not a broadcast of someone else's ("WOW! Name found Emerald Dye"). */
    fun ownDrop(text: String, self: String = Minecraft.getInstance()?.player?.name?.string.orEmpty()): Boolean {
        if (!emeraldDrop.containsMatchIn(text) && "You found" !in text) return false
        val found = text.indexOf(" found ")
        if (found < 0) return true
        val before = text.substring(0, found)
        val finder=before.substringAfterLast(' ').trim()
        return finder.equals("You",true) || self.isNotEmpty() && finder.equals(self,true)
    }

    // ------------------------------------------------------------------ checking

    private fun checkAll(announce: Boolean) {
        if (!data.initialised) return
        for (achievement in definitions)
            if (achievement.measure != Measure.EVENT && !isUnlocked(achievement) && progress(achievement) >= achievement.goal) unlock(achievement.id, announce)
        val dex = ownDex()
        if (ownEntries().isNotEmpty()) unlock("first_sparkle", announce)
        for (biome in SafariBiome.entries) if (dex.containsAll(biome.critters.map { it.name }))
            unlock("set_${biome.name.lowercase()}", announce)
    }

    fun onAllTypes(elapsed: Long) {
        if (!data.initialised) return
        unlock("full_house", true)
        speedAchievements(elapsed, true)
    }
    private fun speedAchievements(elapsed: Long, announce: Boolean) {
        for(id in listOf("speed_3","speed_230","speed_130"))
            if(elapsed in 0 until goal(id)) unlock(id,announce)
    }
    private fun refreshPartyStreaks() {
        val streaks = HistoryQueries.partyStreaks(RunHistory.runs, SparklingLog.entries)
        data.runsSinceOwnSparkling = streaks.current; data.longestRunsWithoutSparkling = streaks.longest
        data.runsSinceOwnUnique = streaks.currentUnique; data.longestRunsWithoutUnique = streaks.longestUnique
        data.partyStreakVersion = 1
    }
    private fun checkRun(run: RunRecord, announce: Boolean) {
        if (run.sparklings.size >= goal("party_double")) unlock("party_double", announce)
        if (run.sparklings.size >= goal("party_triple")) unlock("party_triple", announce)
        if (run.ownSparklings >= goal("own_triple")) unlock("own_triple", announce)
        if (run.macaws >= goal("macaw_3")) unlock("macaw_3", announce)
        if (run.macaws >= goal("macaw_4")) unlock("macaw_4", announce)
        if (run.macaws >= goal("macaw_8")) unlock("macaw_8", announce)
        val self=Minecraft.getInstance()?.player?.name?.string.orEmpty()
        if(AchievementRules.doubleTrouble(run.sparklings,self)) unlock("double_trouble",announce)
        val all = SafariRoster.all.size
        if (run.party >= all) unlock("full_house", announce)
        HistoryQueries.allTypesTime(run)?.let { speedAchievements(it, announce) }
        if (run.you >= goal("ten_of_37")) unlock("ten_of_37", announce)
        val zone = ZoneId.systemDefault()
        if (Instant.ofEpochMilli(run.start + run.length).atZone(zone).hour in 3..4) unlock("night_owl", announce)

    }

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
        Measure.RUNS_SINCE_MACAW -> AchievementRules.macawDrought(RunHistory.runs).longest.toLong()
        Measure.DUPLICATES -> { refreshSparklingSummary();duplicates.longest.toLong() }
        Measure.DEX_SETS -> AchievementRules.fullDexCount(sparklingCounts()).toLong()
        Measure.DYES -> data.emeraldDyes.toLong()
        Measure.DAILY_RUNS -> AchievementRules.mostDailyRuns(RunHistory.runs).toLong()
        Measure.CONTESTS -> data.contestsCompleted.toLong()
    }

    fun isUnlocked(achievement: Achievement) = achievement.id in data.unlocked
    fun unlockedAt(achievement: Achievement) = data.unlocked[achievement.id] ?: 0L

    /** With achievements off, progress still counts but nothing unlocks or is announced; it catches up when turned on. */
    private fun unlock(id: String, announce: Boolean) {
        if (!ConfigManager.achievements || id in data.unlocked) return
        val achievement = definitions.firstOrNull { it.id == id } ?: return
        data.unlocked[id] = System.currentTimeMillis()
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
        queueLocal(LocalChat.component("[SM] §6§lACHIEVEMENT UNLOCKED! ${tier.code}§l${achievement.name}").copy()
            .withStyle { it.withHoverEvent(HoverEvent.ShowText(hover)) },
            LocalChat.component("[SM]   §7${achievement.description} §8(${tier.code}${tier.name}§8)"))
        val showTitle = tier.ordinal >= AchievementTier.LEGENDARY.ordinal
        if (ConfigManager.achievementSound || showTitle) {
            net.johnceo.sparklingmutuals.hud.SafariAlertSequence.queue.submit(this, if (showTitle) 3_500 else 1_000,
                valid = { ConfigManager.achievements && client.player != null }, minimumTicks = if (showTitle) 70 else 0) {
                if (ConfigManager.achievementSound) {
                    if (tier.ordinal >= AchievementTier.MYTHIC.ordinal) AlertSounds.play(client, 0, 1f, 1f)
                    else AlertSounds.play(client, 1, .6f, 1.6f)
                }
                if (showTitle) {
                    client.gui.smSetTimes(5, 50, 15)
                    client.gui.smSetSubtitle(Component.literal("${tier.code}${achievement.name}"))
                    client.gui.smSetTitle(Component.literal("§6§lACHIEVEMENT"))
                }
            }
        }
        if (ConfigManager.achievementsToParty) PartyManager.sendPartyChat(Minecraft.getInstance(), "Achievement unlocked: ${net.johnceo.sparklingmutuals.commands.PartyWording.shiny(achievement.name)}!")
    }

    /** First run: unlock what the history already earns, quietly, and say how many in one line. */
    private fun initialise() {
        data.initialised = true
        val before = data.unlocked.size
        afterImport()
        val earned = data.unlocked.size - before
        if (earned > 0) queueLocal(LocalChat.component(
            "[SM] Achievements are here! Your history already earns §e$earned§r of them. See /sm achievements."))
    }

    /**
     * After history is imported: streaks are worked out again from the whole run history (they only grow), and every
     * run and catch is checked, quietly. Returns how many unlocked.
     */
    fun afterImport(): Int {
        if (!data.initialised) return 0
        val before = data.unlocked.size
        var noMacaw = 0; var lastOwn = false
        for (run in RunHistory.runs.sortedBy { it.start }) {
            noMacaw = if (run.macaws > 0) 0 else noMacaw + 1
            data.longestRunsWithoutMacaw = maxOf(data.longestRunsWithoutMacaw, noMacaw)
            if (run.ownSparklings > 0 && lastOwn) unlock("back_to_back", false)
            lastOwn = run.ownSparklings > 0
            checkRun(run, false)
        }
        if (RunHistory.runs.isNotEmpty()) { data.runsSinceMacaw = noMacaw; data.lastRunHadOwnSparkling = lastOwn }
        refreshPartyStreaks()
        for (entry in ownEntries()) {
            if (entry.critter in setOf("Macaw", "Wumpa", "Doomspiral")) unlock("legendary_sparkle", false)
            if (entry.critter == "Gemzie") unlock("gemzie_sparkle", false)
        }
        if(personalRunGaps().any { it.toLong() in 0..goal("instant") }) unlock("instant",false)
        checkAll(false)
        save()
        return data.unlocked.size - before
    }

    // ------------------------------------------------------------------ reading

    fun ownEntries(): List<SparklingEntry> {
        val self = Minecraft.getInstance()?.player?.name?.string ?: return emptyList()
        return SparklingLog.entries.filter { it.player.equals(self, true) }
    }
    /** Credited party history supplies per-type counts; cached API discoveries establish a one-catch floor. */
    fun sparklingCounts(): Map<String,Int> { refreshSparklingSummary();return counts }
    fun ownDex(): Set<String> = AchievementRules.dex(sparklingCounts(),1)
    private fun personalRunGaps(): List<Int> {
        val current=net.johnceo.sparklingmutuals.safari.SafariTracking.ledger.current
        return AchievementRules.personalSparklingGaps(RunHistory.runs,ownEntries(),current?.let { it.startedAt to (it.endedAt ?: System.currentTimeMillis()) })
    }

    /** Your profile was read from the API: whatever its runs, catches and dex earn is unlocked quietly. Returns how many. */
    fun afterApiStats(): Int {
        if (!data.initialised || !ConfigManager.achievements) return 0
        val before = data.unlocked.size
        checkAll(false)
        save()
        return data.unlocked.size - before
    }
    val longestDrySparkling get() = maxOf(data.longestRunsWithoutSparkling, data.runsSinceOwnSparkling)
    val longestMacawDrought get() = maxOf(data.longestRunsWithoutMacaw, data.runsSinceMacaw)
    val runsSinceMacaw get() = data.runsSinceMacaw
    val contestsCompleted get() = data.contestsCompleted
    fun save() = HistoryStore.save("achievements", data)
}
