package net.johnceo.sparklingmutuals.safari.ui

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ProgressRowDisplay
import net.johnceo.sparklingmutuals.config.BiomeObjectiveConfig
import net.johnceo.sparklingmutuals.config.BiomeObjectiveRow
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.history.SafariSessionStats
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
import net.johnceo.sparklingmutuals.safari.completion.ScannableCritterChecks
import net.johnceo.sparklingmutuals.safari.structures.WallSummary

/** One collected total, with the v0.9.0 biome and critter rarity colors. */
object SafariPanels {
    private fun completedRow(text: String) = HudRow(text, color = 0xFF55FF55.toInt(), complete = true)
    private fun missingCompleteText(sparkling: Boolean) = if (sparkling) "All checked!" else "All caught and checked!"
    private val fixedChecks = mapOf("Scrappy" to 3, "Gemzie" to 3, "Troodon" to 3, "Gazer" to 4)
    fun fixedCounter(name: String, run: SafariRun?, mode: SafariMode, party: PartySparklingState): String? {
        val total = fixedChecks[name] ?: return null
        if (mode != SafariMode.FULL_CLEAR && !BiomeObjectiveConfig.needs(name, party, mode) && !ScannableCritterChecks.enabled(name)) return null
        val count = if (mode == SafariMode.FULL_CLEAR) run?.captureProgress(name) ?: 0 else run?.checkedProgress(name) ?: 0
        return "${count.coerceIn(0, total)}/$total " + if (mode == SafariMode.FULL_CLEAR) "captured" else "checked"
    }
    fun progress(run: SafariRun?, unique: Boolean, now: Long, party: PartySparklingState = SafariSparklingMode.state): HudPanel {
        val seconds = (run?.elapsed(now) ?: 0) / 1000
        val time = "%d:%02d".format(seconds / 60, seconds % 60)
        val title = when { run == null -> "Critter Safari (ready)"; run.endedAt != null -> "Last Safari  $time"; else -> "Critter Safari  $time" }
        val mode = SafariFullClear.mode
        fun bar(label: String, critters: List<SafariCritter>, color: Int, display: ProgressRowDisplay): HudRow {
            val areas = critters.map { it.biome }.distinct()
            val iconComplete = run?.let { current -> areas.all { area ->
                current.backendBiomeReportReceived(area) || if (current.manualReportReceived(area))
                    current.manualBiomeIconComplete(mode, area, party) ||
                        current.lastBiome == area && current.biomeComplete(mode, area, party)
                else current.biomeComplete(mode, area, party)
            } } == true
            if (mode == SafariMode.SPARKLING && !party.ready) return HudRow(label, "?", color, color,
                complete = iconComplete, completedIcon = net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(label)
                    ?: net.johnceo.sparklingmutuals.hud.UiIcons.Kind.CHECK)
            val targets = if (mode == SafariMode.SPARKLING) critters.filter { party.needs(it.name) } else critters
            val count = if (mode == SafariMode.UNIQUE) run?.uniqueBarProgress(targets) ?: 0
                else run?.completed(mode, targets, party) ?: 0
            val complete = iconComplete
            return HudRow(label, "$count/${targets.size}", color, color,
                when {
                    !ConfigManager.progressTracks || display == ProgressRowDisplay.COUNT_ONLY -> null
                    targets.isEmpty() -> 1f
                    else -> count.toFloat() / targets.size
                }, complete = complete,
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
        val biomes = SafariBiome.entries.filter { ConfigManager.progressRow(it) != ProgressRowDisplay.HIDDEN }
        return HudPanel(title, rows = buildList {
            val overallDisplay = ConfigManager.progressRow(null)
            if (overallDisplay != ProgressRowDisplay.HIDDEN) {
                add(bar(overall, SafariRoster.all, HudRow.WHITE, overallDisplay))
                if (biomes.isNotEmpty()) add(HudRow())
            }
            biomes.forEach { add(bar(it.label, it.critters, it.color, ConfigManager.progressRow(it))) }
            if (extras.isNotEmpty()) {
                if (isNotEmpty()) add(HudRow())
                addAll(extras)
            }
        })

    }
    /** Critters of this biome the current mode still needs; the missing panel and `!m` both read it. */
    fun missingCritters(run: SafariRun?, biome: SafariBiome, party: PartySparklingState = SafariSparklingMode.state): List<SafariCritter> {
        val sparkling = SafariFullClear.mode == SafariMode.SPARKLING
        return biome.critters.filter { if (sparkling) {
                (party.needs(it.name) || ScannableCritterChecks.enabled(it.name)) && run?.sparklingComplete(it.name, party) != true ||
                    run?.sparklingChecks?.wumpaPrerequisite(it.name, party) == true && run.captureProgress("Wumpa") == 0 && run.captureProgress(it.name) == 0
            }
            else if (SafariFullClear.mode == SafariMode.UNIQUE) run == null || run.captureProgress(it.name) == 0 && !run.sharedCritterComplete(it.name, SafariMode.UNIQUE) && !run.uniqueUnavailable(it.name)
            else run?.missing(it, ConfigManager.fullClearMode) != false }
    }
    fun missing(run: SafariRun?, biome: SafariBiome, unique: Boolean, nests: Int, mounds: Int = 0,
        walls: WallSummary = WallSummary(emptyList()), party: PartySparklingState = SafariSparklingMode.state): HudPanel {
        val mode = SafariFullClear.mode
        val sparkling = mode == SafariMode.SPARKLING
        val biomeComplete = run?.biomeComplete(mode, biome, party) == true
        if (run != null && SafariHelperRules.postClearActive(mode, run, biome, party, biomeComplete) &&
            biome.critters.any { SafariHelperRules.needsPostClearCheck(it.name, run, party) })
            return HudPanel("${biome.label} Biome — 0 left", biome.color, listOf(completedRow("All captured")))
        if(run != null && biomeComplete && !ScannableCritterChecks.pending(biome,run,party))
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
        val mode = SafariFullClear.mode
        val sparkling = mode == SafariMode.SPARKLING
        fun show(row: BiomeObjectiveRow) = BiomeObjectiveConfig.show(row)
        val biomeComplete = run?.biomeComplete(mode, biome, party) == true
        val afterClear = run != null && SafariHelperRules.postClearActive(mode, run, biome, party, biomeComplete)
        val postChecks = if (afterClear && run != null) biome.critters.filter {
            SafariHelperRules.needsPostClearCheck(it.name, run, party)
        }.mapTo(mutableSetOf()) { it.name } else emptySet()
        fun sourceNeeded(name: String) = if (afterClear) name in postChecks else SafariHelperRules.needed(name, mode, run, party)
        val birdNames = listOf("Bluebird", "Parakeet", "Macaw")
        val showSupplies = when (biome) {
            SafariBiome.FOREST -> show(BiomeObjectiveRow.STARTING_FOOD_SYNC)
            SafariBiome.CAVERN -> show(BiomeObjectiveRow.GEM_SYNC) && ("Gemzie" in postChecks || BiomeObjectiveConfig.allowsEsp("Gemzie", mode, run))
            else -> false
        }
        val playerSupplies = if (!showSupplies) emptyList() else
            net.johnceo.sparklingmutuals.sync.SafariPartySync.objectiveSupplies(run, biome, party, mode,
                afterClearBirds = afterClear && birdNames.any { it in postChecks }).map { (player, items) ->
            HudRow(player, icon = null, counters = items.map { (item, count) ->
                net.johnceo.sparklingmutuals.hud.HudIconCount(
                    net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(item.label)!!, count,
                    net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(item.label))
            })
        }
        val supplies = if (biome == SafariBiome.FOREST && playerSupplies.isNotEmpty())
            listOf(HudRow("Unused starting food", color = HudRow.GOLD, icon = null)) + playerSupplies
        else playerSupplies
        if(run != null && (biomeComplete || afterClear) && postChecks.isEmpty() && !ScannableCritterChecks.pending(biome,run,party))
            return HudPanel("Biome Objectives",biome.color,listOfNotNull(completedRow("All objectives complete!")
                .takeIf { show(BiomeObjectiveRow.COMPLETE) }) + supplies,net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(biome.label))
        if (afterClear && SafariSettings.standardEdition) {
            val checks = if (ConfigManager.objectiveShinyReminder) postChecks.map { HudRow("Check $it", icon = null) } else emptyList()
            return HudPanel("Biome Objectives", biome.color, supplies + checks, net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(biome.label))
        }
        val helpers = buildList {
            addAll(supplies)
            if(!afterClear && missingCritters(run,biome,party).isNotEmpty() &&
                (if(mode==SafariMode.SPARKLING) ConfigManager.objectiveShinyReminder else ConfigManager.objectiveCaptureReminder)) add(HudRow(when(mode) {
                SafariMode.UNIQUE -> "Capture all uniques"
                SafariMode.FULL_CLEAR -> "Capture all critters"
                SafariMode.SPARKLING -> "Check needed critters"
            },icon=null))
            if (ConfigManager.objectiveShinyReminder) {
                if (afterClear) {
                    val sourceRows = SafariHelperRules.species + birdNames + setOf("Gimmiegold", "Gazer", "Wumpa")
                    for (name in postChecks - sourceRows) add(HudRow("Check $name",
                        fixedCounter(name, run, SafariMode.SPARKLING, party), icon = null))
                } else if (run != null && mode == SafariMode.UNIQUE && biome.critters.any {
                    BiomeObjectiveConfig.requiresUniqueCheck(it.name, party) && !run.sparklingComplete(it.name, party)
                }) add(HudRow("Finish shiny checks", icon = null))
            }
            if (show(BiomeObjectiveRow.STARTING_ITEMS) && run?.hasUnusedStartingForBiome(biome, mode, party) == true)
                add(HudRow("Use starting items", icon = null))
            val requiredBirds=birdNames.filter {
                when {
                    afterClear -> it in postChecks
                    sparkling -> party.needs(it)
                    else -> BiomeObjectiveConfig.allowsEsp(it, mode, run)
                }
            }.map { SafariRoster.named(it)!! }
            val birdsDone=!afterClear && run!=null && run.completed(mode,requiredBirds,party)==requiredBirds.size
            val showBirds=biome==SafariBiome.FOREST && requiredBirds.isNotEmpty() && !birdsDone
            if (show(BiomeObjectiveRow.WUMPA) && biome == SafariBiome.ICY && (if (afterClear) "Wumpa" in postChecks else !sparkling || party.needs("Wumpa")) &&
                !net.johnceo.sparklingmutuals.sync.SafariPartySync.objectiveDone("Wumpa") &&
                run?.sparklingComplete("Wumpa", party, localOnly = true) != true)
                add(HudRow("Capture Wumpa", icon = net.johnceo.sparklingmutuals.hud.UiIcons.Kind.RAVAGER))
            if (show(BiomeObjectiveRow.NESTS) && biome == SafariBiome.FOREST &&
                (if (afterClear) "Honeybug" in postChecks else SafariHelperRules.showNestCounter(mode, run, party)))
                add(HudRow("Bee nests", if(nests>0) "$nests" else "?", HudRow.GOLD, HudRow.GRAY))
            if (show(BiomeObjectiveRow.FOOD_TOTAL) && showBirds && run?.birdSpawnsComplete()!=true)
                add(HudRow("Bird food left to use", run?.knownBirdFoodLeftToUse?.toString() ?: "?", HudRow.GOLD, HudRow.GRAY))
            if (showBirds && run != null) {
                val birds=run.birds
                val foods=listOf("Bag of Seeds","Wriggleworm","Yogi Berry")
                if(show(BiomeObjectiveRow.FOOD_TYPES) && foods.any { birds.remaining(it)>0 }) add(HudRow(counters=foods.filter { birds.remaining(it)>0 }.map { food ->
                    net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(food)!!,
                        birds.remaining(food),net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(food))
                }))
                if(show(BiomeObjectiveRow.FEEDER) && birds.feederCount>0) add(HudRow("Feeder",icon=null,counters=listOf(
                    net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(birds.feederFood ?: "Bird food")!!,
                        birds.feederCount,net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(birds.feederFood ?: "Feed in feeder")))))
                val spawned=birdNames.filter { birds.visibleCount(it)>0 }
                if(show(BiomeObjectiveRow.BIRDS) && spawned.isNotEmpty()) add(HudRow("Spawned",icon=null,counters=spawned.map { bird ->
                    net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.Kind.BIRD,
                        birds.visibleCount(bird), when (bird) {
                            "Bluebird" -> 0xFF5555FF.toInt()
                            "Parakeet" -> 0xFF55FF55.toInt()
                            else -> 0xFFFFAA00.toInt()
                        })
                }))
            }
            if (biome == SafariBiome.CAVERN) {
                if (show(BiomeObjectiveRow.PODIUMS) && run != null && sourceNeeded("Gemzie") &&
                    !run.gems.opened && !run.sparklingChecks.gemzieChamberOpened) {
                    val colors=mapOf(net.johnceo.sparklingmutuals.safari.automation.GemzieGem.ORANGE to 0xFFFF7733.toInt(),
                        net.johnceo.sparklingmutuals.safari.automation.GemzieGem.LIME to 0xFF99FF33.toInt(),
                        net.johnceo.sparklingmutuals.safari.automation.GemzieGem.PURPLE to 0xFFBB66FF.toInt())
                    add(HudRow("Podium gems",icon=net.johnceo.sparklingmutuals.hud.UiIcons.Kind.GEM,gems=colors.map { (gem,color) -> if(gem in run.gems.placed) color else HudRow.GRAY }))
                }
                if (show(BiomeObjectiveRow.WALLS) && sourceNeeded("Snoozle") && walls.states.isNotEmpty() && !walls.allBroken)
                    add(HudRow("Snoozle walls", walls.remaining, HudRow.GOLD, HudRow.GRAY))
                if (show(BiomeObjectiveRow.MOUNDS) && sourceNeeded("Rockmite") && mounds > 0)
                    add(HudRow("Mounds", "$mounds", HudRow.GOLD, HudRow.GRAY))
            }
            if (biome == SafariBiome.HAUNTED && (mode == SafariMode.UNIQUE || sparkling)) {
                if (show(BiomeObjectiveRow.COINS) && (if (afterClear) "Gimmiegold" in postChecks else
                    BiomeObjectiveConfig.needs("Gimmiegold", party, mode) && run?.sparklingComplete("Gimmiegold", party) != true))
                    add(HudRow("Coins left to use", run?.coinsLeftToUse?.toString() ?: "?", HudRow.GOLD, HudRow.GRAY))
                if (show(BiomeObjectiveRow.GAZERS) && (if (afterClear) "Gazer" in postChecks else
                    BiomeObjectiveConfig.needs("Gazer", party, mode) && run?.sparklingComplete("Gazer", party) != true))
                    add(HudRow("Gazers left", "${(4 - (run?.checkedProgress("Gazer") ?: 0)).coerceAtLeast(0)}",
                        HudRow.GOLD, HudRow.GRAY))
            }
        }
        return HudPanel("Biome Objectives", biome.color, helpers.filter { it.value!="0" }.map { row ->
            row.copy(color = net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(row.label),
                valueColor = if (row.value == "0") 0xFF55FF55.toInt() else HudRow.WHITE)
        }.ifEmpty { if(afterClear || !show(BiomeObjectiveRow.CONFIRM_CLEAR) || !ConfigManager.objectiveCaptureReminder || !ConfigManager.objectiveShinyReminder) emptyList()
            else listOf(HudRow("Confirm biome clear", icon = null)) },net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(biome.label))
    }
    private fun captureColor(complete: Boolean, verified: Boolean) = if (!ConfigManager.fullClearMode || !verified) HudRow.WHITE
        else if (complete) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
    fun captures(run: SafariRun?, biome: SafariBiome, party: PartySparklingState = SafariSparklingMode.state) = HudPanel("${biome.label} captures", biome.color, buildList {
        biome.critters.forEach { val count = run?.captureProgress(it.name) ?: 0
            val knownComplete = run?.knownCaptureQuotaMet(it.name) == true
            val locallyObserved = run?.visitedBiome(biome) == true && run.hasCaptureEvidence(biome)
            val status = when (SafariFullClear.mode) {
                SafariMode.FULL_CLEAR -> when {
                    knownComplete -> true
                    run?.sharedCritterComplete(it.name, SafariMode.FULL_CLEAR) == true -> true
                    run == null || run.capturesLeft(it.name, count) > 0 -> false
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
            add(HudRow(it.name, if (run?.inferredCapture(it.name) == true && count <= run.count(it.name)) "\u22651" else "$count", it.color, captureColor(knownComplete || run?.fullClearComplete(it.name) == true,
                run?.sharedCritterComplete(it.name, SafariMode.FULL_CLEAR) == true ||
                    run?.hasCaptureEvidence(biome) == true || run?.fullClearReportReceived(biome) == true ||
                    it.name == "Honeybug" && run?.beeNestCountKnown == true ||
                    it.name in SafariFullClear.maximumCaptures && count > 0),
                complete = status, icon = net.johnceo.sparklingmutuals.hud.UiIcons.Kind.QUESTION)) }
        add(HudRow()); add(HudRow("Total captures", "${biome.critters.sumOf { run?.captureProgress(it.name) ?: 0 }}", HudRow.GRAY))
        if (biome == SafariBiome.CAVERN && ConfigManager.showMoundStats) {
            add(HudRow())
            add(HudRow("Rockmite Mounds", "${run?.brokenMounds ?: 0}", HudRow.GOLD,
                captureColor(run?.moundsComplete() == true, run?.hasCaptureEvidence(biome) == true && run.inheritedMounds.not())))
            add(HudRow("Mounds with Rockmite", "${run?.rockmiteMounds ?: 0}", HudRow.GOLD))
        }
    })
    fun splits(run: SafariRun, mode: SafariMode) =
        splitsPanel(if (ConfigManager.splitsShowSession) RunHistory.currentSessionStats() else null) { run.splits.time(mode, it) }

    fun splitsPreview() = splitsPanel(if (ConfigManager.splitsShowSession) SafariSessionStats(100, 108_150L, 1) else null) {
        if (it == SafariBiome.FOREST) 73_245L else null
    }

    private fun splitsPanel(session: SafariSessionStats?, time: (SafariBiome) -> Long?): HudPanel {
        val format = ConfigManager.splitsTimeFormat
        val rows = SafariBiome.entries.map { biome ->
            val value = time(biome)?.let(format::format)
            HudRow(biome.label, value ?: "—", biome.color, if (value == null) HudRow.GRAY else HudRow.WHITE)
        }
        val footer = session?.let { stats ->
            val teal = 0xFF55FFFF.toInt()
            val runsPerSparkling = stats.formattedRunsPerSparkling?.let { " ($it r/s)" }.orEmpty()
            listOf(
                HudRow("Session avg", stats.averageRunMillis?.let(format::format) ?: "—", teal, icon = null),
                HudRow("Runs", stats.runs.toString(), teal, icon = null),
                HudRow("Sparklings", "${stats.sparklings}$runsPerSparkling", HudRow.GOLD, icon = null),
            )
        }.orEmpty()
        return HudPanel("Safari Splits", rows = rows, compactFooter = footer)
    }

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
