package net.johnceo.sparklingmutuals.safari.ui

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariCritter
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules
import net.johnceo.sparklingmutuals.safari.structures.WallSummary

/** One collected total, with the v0.9.0 biome and critter rarity colors. */
object SafariPanels {
    private fun completedRow(text: String) = HudRow(text, color = 0xFF55FF55.toInt(), complete = true)
    private fun missingCompleteText(sparkling: Boolean) = if (sparkling) "All checked!" else "All caught and checked!"
    private val fixedChecks = mapOf("Scrappy" to 3, "Gemzie" to 3, "Troodon" to 3, "Gazer" to 4)
    fun fixedCounter(name: String, run: SafariRun?, mode: SafariMode, party: PartySparklingState): String? {
        val total = fixedChecks[name] ?: return null
        if (mode != SafariMode.FULL_CLEAR && !party.needs(name)) return null
        val count = if (mode == SafariMode.FULL_CLEAR) run?.captureProgress(name) ?: 0 else run?.checkedProgress(name) ?: 0
        return "${count.coerceIn(0, total)}/$total " + if (mode == SafariMode.FULL_CLEAR) "captured" else "checked"
    }
    fun progress(run: SafariRun?, unique: Boolean, now: Long, party: PartySparklingState = SafariSparklingMode.state): HudPanel {
        val seconds = (run?.elapsed(now) ?: 0) / 1000
        val time = "%d:%02d".format(seconds / 60, seconds % 60)
        val title = when { run == null -> "Critter Safari (ready)"; run.endedAt != null -> "Last Safari  $time"; else -> "Critter Safari  $time" }
        val mode = SafariFullClear.mode
        fun bar(label: String, critters: List<SafariCritter>, color: Int): HudRow {
            val areas = critters.map { it.biome }.distinct()
            val iconComplete = run?.let { current -> areas.all { area ->
                current.backendBiomeReportReceived(area) || if (current.manualReportReceived(area))
                    current.manualBiomeIconComplete(mode, area, party) else current.biomeComplete(mode, area, party)
            } } == true
            if (mode == SafariMode.SPARKLING && !party.ready) return HudRow(label, "?", color, color,
                complete = iconComplete, completedIcon = net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(label)
                    ?: net.johnceo.sparklingmutuals.hud.UiIcons.Kind.CHECK)
            val targets = if (mode == SafariMode.SPARKLING) critters.filter { party.needs(it.name) } else critters
            val count = if (mode == SafariMode.UNIQUE) run?.uniqueBarProgress(targets) ?: 0
                else run?.completed(mode, targets, party) ?: 0
            val complete = iconComplete
            return HudRow(label, "$count/${targets.size}", color, color,
                if (targets.isEmpty()) 1f else count.toFloat() / targets.size, complete = complete,
                completedIcon = net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(label) ?: net.johnceo.sparklingmutuals.hud.UiIcons.Kind.CHECK)
        }
        val extras=buildList {
            if(ConfigManager.progressHotspot) run?.entry?.hotspot?.let { add(HudRow("Hotspot",it.label,it.color,it.color)) }
            if(ConfigManager.progressProfit && run!=null) {
                val coins=net.johnceo.sparklingmutuals.history.RunHistory.liveProfit(run)
                add(HudRow("Profit",if(coins<0) "Loading…" else net.johnceo.sparklingmutuals.history.BazaarPrices.format(coins),0xFFFFCC66.toInt()))
            }
        }
        val overall = when (mode) {
            SafariMode.SPARKLING -> if (party.ready) "Fully Checked" else "Loading discoveries"
            SafariMode.UNIQUE -> "Completed"
            SafariMode.FULL_CLEAR -> "Fully Collected"
        }
        return HudPanel(title, rows = listOf(bar(overall, SafariRoster.all, HudRow.WHITE), HudRow()) +
            SafariBiome.entries.map { bar(it.label, it.critters, it.color) } + if(extras.isEmpty()) emptyList() else listOf(HudRow())+extras)

    }
    /** Critters of this biome the current mode still needs; the missing panel and `!m` both read it. */
    fun missingCritters(run: SafariRun?, biome: SafariBiome, party: PartySparklingState = SafariSparklingMode.state): List<SafariCritter> {
        val sparkling = SafariFullClear.mode == SafariMode.SPARKLING
        return biome.critters.filter { if (sparkling) {
                party.needs(it.name) && run?.sparklingComplete(it.name, party) != true ||
                    run?.sparklingChecks?.wumpaPrerequisite(it.name, party) == true && run.captureProgress("Wumpa") == 0 && run.captureProgress(it.name) == 0
            }
            else if (SafariFullClear.mode == SafariMode.UNIQUE) run == null || run.captureProgress(it.name) == 0 && !run.sharedCritterComplete(it.name, SafariMode.UNIQUE) && !run.uniqueUnavailable(it.name)
            else run?.missing(it, ConfigManager.fullClearMode) != false }
    }
    fun missing(run: SafariRun?, biome: SafariBiome, unique: Boolean, nests: Int, mounds: Int = 0,
        walls: WallSummary = WallSummary(emptyList()), party: PartySparklingState = SafariSparklingMode.state): HudPanel {
        val sparkling = SafariFullClear.mode == SafariMode.SPARKLING
        if(run?.biomeComplete(SafariFullClear.mode,biome,party)==true)
            return HudPanel("${biome.label} ${if(sparkling) "Unchecked" else "Biome"} — 0 left",biome.color,listOf(completedRow(missingCompleteText(sparkling))))
        val missing = missingCritters(run, biome, party)
        val listed = missing
        val rows = listed.map { critter ->
            HudRow(critter.name, fixedCounter(critter.name, run, SafariFullClear.mode, party), critter.color, HudRow.GRAY, icon = null)
        }
        return HudPanel("${biome.label} ${if (sparkling) "Unchecked" else "Biome"} — ${listed.size} left", biome.color,
            rows.ifEmpty { listOf(completedRow(if (SafariFullClear.mode == SafariMode.UNIQUE) "All caught!" else missingCompleteText(sparkling))) })
    }
    /** Separate objectives layout and biome colors adapted from SafariUtils PartyObjectiveHud (MIT). */
    fun objectives(run: SafariRun?, biome: SafariBiome, unique: Boolean, nests: Int, mounds: Int = 0,
        walls: WallSummary = WallSummary(emptyList()), party: PartySparklingState = SafariSparklingMode.state): HudPanel {
        val sparkling = SafariFullClear.mode == SafariMode.SPARKLING
        val playerSupplies = net.johnceo.sparklingmutuals.sync.SafariPartySync.objectiveSupplies(run, biome, party).map { (player, items) ->
            HudRow(player, icon = null, counters = items.map { (item, count) ->
                net.johnceo.sparklingmutuals.hud.HudIconCount(
                    net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(item.label)!!, count,
                    net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(item.label))
            })
        }
        val supplies = if (biome == SafariBiome.FOREST && playerSupplies.isNotEmpty())
            listOf(HudRow("Unused starting food", color = HudRow.GOLD, icon = null)) + playerSupplies
        else playerSupplies
        if(run?.biomeComplete(SafariFullClear.mode,biome,party)==true)
            return HudPanel("Biome Objectives",biome.color,listOf(completedRow("All objectives complete!")) + supplies,net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(biome.label))
        val helpers = buildList {
            addAll(supplies)
            val mode = SafariFullClear.mode
            if(missingCritters(run,biome,party).isNotEmpty()) add(HudRow(when(mode) {
                SafariMode.UNIQUE -> "Capture all uniques"
                SafariMode.FULL_CLEAR -> "Capture all critters"
                SafariMode.SPARKLING -> "Check needed critters"
            },icon=null))
            val checksLeft = if (run != null && mode == SafariMode.UNIQUE)
                biome.critters.filter { party.needs(it.name) && !run.sparklingComplete(it.name, party, localOnly = true) }
            else emptyList()
            if (checksLeft.isNotEmpty()) add(HudRow("Finish shiny checks", icon = null))
            if (run?.sharedSuppliesPending(biome) == true) add(HudRow("Waiting for party items", icon = null))
            if (run?.hasUnusedStartingForBiome(biome) == true) add(HudRow("Use starting items", icon = null))
            val birdNames=listOf("Bluebird","Parakeet","Macaw")
            val requiredBirds=birdNames.filter { !sparkling || party.needs(it) }.map { SafariRoster.named(it)!! }
            val birdsDone=run!=null && run.completed(mode,requiredBirds,party,localOnly=true)==requiredBirds.size
            val showBirds=biome==SafariBiome.FOREST && requiredBirds.isNotEmpty() && !birdsDone
            if (biome == SafariBiome.ICY && (!sparkling || party.needs("Wumpa")) &&
                !net.johnceo.sparklingmutuals.sync.SafariPartySync.objectiveDone("Wumpa") &&
                run?.sparklingComplete("Wumpa", party, localOnly = true) != true)
                add(HudRow("Capture Wumpa", icon = net.johnceo.sparklingmutuals.hud.UiIcons.Kind.RAVAGER))
            if (biome == SafariBiome.FOREST && SafariHelperRules.showNestCounter(mode, run, party))
                add(HudRow("Bee nests", if(nests>0) "$nests" else "?", HudRow.GOLD, HudRow.GRAY))
            if (showBirds && run?.birdSpawnsComplete()!=true)
                add(HudRow("Bird food left to use", run?.knownBirdFoodLeftToUse?.toString() ?: "?", HudRow.GOLD, HudRow.GRAY))
            if (showBirds && run != null) {
                val birds=run.birds
                val foods=listOf("Bag of Seeds","Wriggleworm","Yogi Berry")
                if(foods.any { birds.remaining(it)>0 }) add(HudRow(counters=foods.filter { birds.remaining(it)>0 }.map { food ->
                    net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(food)!!,
                        birds.remaining(food),net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(food))
                }))
                if(birds.feederCount>0) add(HudRow("Feeder",icon=null,counters=listOf(
                    net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(birds.feederFood ?: "Bird food")!!,
                        birds.feederCount,net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(birds.feederFood ?: "Feed in feeder")))))
                val spawned=birdNames.filter { birds.visibleCount(it)>0 }
                if(spawned.isNotEmpty()) add(HudRow("Spawned",icon=null,counters=spawned.map { bird ->
                    net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.Kind.BIRD,
                        birds.visibleCount(bird), when (bird) {
                            "Bluebird" -> 0xFF5555FF.toInt()
                            "Parakeet" -> 0xFF55FF55.toInt()
                            else -> 0xFFFFAA00.toInt()
                        })
                }))
            }
            if (biome == SafariBiome.CAVERN) {
                if (run != null && SafariHelperRules.needed("Gemzie",mode,run,party) &&
                    !run.gems.opened && !run.sparklingChecks.gemzieChamberOpened) {
                    val colors=mapOf(net.johnceo.sparklingmutuals.safari.automation.GemzieGem.ORANGE to 0xFFFF7733.toInt(),
                        net.johnceo.sparklingmutuals.safari.automation.GemzieGem.LIME to 0xFF99FF33.toInt(),
                        net.johnceo.sparklingmutuals.safari.automation.GemzieGem.PURPLE to 0xFFBB66FF.toInt())
                    add(HudRow("Podium gems",icon=net.johnceo.sparklingmutuals.hud.UiIcons.Kind.GEM,gems=colors.map { (gem,color) -> if(gem in run.gems.placed) color else HudRow.GRAY }))
                }
                if (SafariHelperRules.needed("Snoozle", mode, run, party) && walls.states.isNotEmpty() && !walls.allBroken)
                    add(HudRow("Snoozle walls", walls.remaining, HudRow.GOLD, HudRow.GRAY))
                if (SafariHelperRules.needed("Rockmite", mode, run, party) && mounds > 0)
                    add(HudRow("Mounds", "$mounds", HudRow.GOLD, HudRow.GRAY))
            }
            if (biome == SafariBiome.HAUNTED && (mode == SafariMode.UNIQUE || sparkling)) {
                if (party.needs("Gimmiegold") && run?.sparklingComplete("Gimmiegold", party) != true)
                    add(HudRow("Coins left to use", run?.coinsLeftToUse?.toString() ?: "?", HudRow.GOLD, HudRow.GRAY))
                if (party.needs("Gazer") && run?.sparklingComplete("Gazer", party) != true)
                    add(HudRow("Gazers left", "${(4 - (run?.checkedProgress("Gazer") ?: 0)).coerceAtLeast(0)}",
                        HudRow.GOLD, HudRow.GRAY))
            }
        }
        return HudPanel("Biome Objectives", biome.color, helpers.filter { it.value!="0" }.map { row ->
            row.copy(color = net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(row.label),
                valueColor = if (row.value == "0") 0xFF55FF55.toInt() else HudRow.WHITE)
        }.ifEmpty { listOf(HudRow("Confirm biome clear", icon = null)) },net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(biome.label))
    }
    private fun captureColor(complete: Boolean, verified: Boolean) = if (!ConfigManager.fullClearMode || !verified) HudRow.WHITE
        else if (complete) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
    fun captures(run: SafariRun?, biome: SafariBiome, party: PartySparklingState = SafariSparklingMode.state) = HudPanel("${biome.label} captures", biome.color, buildList {
        biome.critters.forEach { val count = run?.captureProgress(it.name) ?: 0
            val locallyObserved = run?.visitedBiome(biome) == true && run.hasCaptureEvidence(biome)
            val status = when (SafariFullClear.mode) {
                SafariMode.FULL_CLEAR -> when {
                    run?.sharedCritterComplete(it.name, SafariMode.FULL_CLEAR) == true -> true
                    run == null || run.capturesLeft(it.name) > 0 -> false
                    locallyObserved && run.captureComplete(it.name) -> true
                    else -> null
                }
                SafariMode.UNIQUE -> count > 0
                SafariMode.SPARKLING -> when {
                    !party.needs(it.name) -> true
                    run?.sharedCritterComplete(it.name, SafariMode.SPARKLING) == true -> true
                    locallyObserved && run?.sparklingComplete(it.name, party, localOnly = true) == true -> true
                    else -> null
                }
            }
            add(HudRow(it.name, if (run?.inferredCapture(it.name) == true && count <= run.count(it.name)) "\u22651" else "$count", it.color, captureColor(run?.fullClearComplete(it.name) == true,
                run?.sharedCritterComplete(it.name, SafariMode.FULL_CLEAR) == true ||
                    run?.hasCaptureEvidence(biome) == true || run?.fullClearReportReceived(biome) == true ||
                    it.name == "Honeybug" && run?.beeNestCountKnown == true ||
                    it.name in SafariFullClear.fixedCaptureTargets && count > 0),
                complete = status, icon = net.johnceo.sparklingmutuals.hud.UiIcons.Kind.QUESTION)) }
        add(HudRow()); add(HudRow("Total captures", "${biome.critters.sumOf { run?.captureProgress(it.name) ?: 0 }}", HudRow.GRAY))
        if (biome == SafariBiome.CAVERN && ConfigManager.showMoundStats) {
            add(HudRow())
            add(HudRow("Rockmite Mounds", "${run?.brokenMounds ?: 0}", HudRow.GOLD,
                captureColor(run?.moundsComplete() == true, run?.hasCaptureEvidence(biome) == true && run.inheritedMounds.not())))
            add(HudRow("Mounds with Rockmite", "${run?.rockmiteMounds ?: 0}", HudRow.GOLD))
        }
    })
    fun objectivesPreview() = HudPanel("Biome Objectives", SafariBiome.FOREST.color, listOf(
        HudRow("Bee nests", "3", net.johnceo.sparklingmutuals.hud.ObjectivePalette.color("Bee nests"), HudRow.WHITE),
        HudRow("Bird food left to use", "4", net.johnceo.sparklingmutuals.hud.ObjectivePalette.color("Bird food"), HudRow.WHITE),
        HudRow(counters=listOf(net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.Kind.SEED_BAG,2,0xFFFFE294.toInt()),
            net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.Kind.WORM,1,0xFFF2C7A5.toInt()),
            net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.Kind.BERRY,1,0xFF75DCEB.toInt())))
    ),net.johnceo.sparklingmutuals.hud.UiIcons.Kind.LEAF)
    fun previewRun() = SafariRun(0).apply {
        record(SafariCatch(SafariRoster.named("Treefrog")!!)); lastBiome = SafariBiome.FOREST
    }
}
