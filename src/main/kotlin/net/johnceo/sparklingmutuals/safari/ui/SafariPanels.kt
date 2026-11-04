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
        val count = if (mode == SafariMode.FULL_CLEAR) run?.count(name) ?: 0 else run?.sparklingChecks?.checkCount(name) ?: 0
        return "${count.coerceIn(0, total)}/$total " + if (mode == SafariMode.FULL_CLEAR) "captured" else "checked"
    }
    fun progress(run: SafariRun?, unique: Boolean, now: Long, party: PartySparklingState = SafariSparklingMode.state): HudPanel {
        val seconds = (run?.elapsed(now) ?: 0) / 1000
        val time = "%d:%02d".format(seconds / 60, seconds % 60)
        val title = when { run == null -> "Critter Safari (ready)"; run.endedAt != null -> "Last Safari  $time"; else -> "Critter Safari  $time" }
        val mode = SafariFullClear.mode
        fun bar(label: String, critters: List<SafariCritter>, color: Int): HudRow {
            if (mode == SafariMode.SPARKLING && !party.ready) return HudRow(label, "?", color, color)
            val targets = if (mode == SafariMode.SPARKLING) critters.filter { party.needs(it.name) } else critters
            val count = if (mode == SafariMode.UNIQUE) run?.uniqueBarProgress(targets) ?: 0
                else run?.completed(mode, targets, party) ?: 0
            val areas=critters.map { it.biome }.distinct()
            val complete=run?.let { areas.all { area -> it.biomeComplete(SafariFullClear.mode,area,party) } } ?: false
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
                    run?.sparklingChecks?.wumpaPrerequisite(it.name, party) == true && run.count("Wumpa") == 0 && run.count(it.name) == 0
            }
            else if (SafariFullClear.mode == SafariMode.UNIQUE) run == null || run.count(it.name) == 0 && !run.uniqueUnavailable(it.name)
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
        val supplies = net.johnceo.sparklingmutuals.sync.SafariPartySync.objectiveSupplies(run, biome).map { (player, items) ->
            HudRow(player, icon = null, counters = items.map { (item, count) ->
                net.johnceo.sparklingmutuals.hud.HudIconCount(
                    net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(item.label)!!, count,
                    net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(item.label))
            })
        }
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
                val spawned=birdNames.filter { birds.spawnedCount(it)>0 }
                if(spawned.isNotEmpty()) add(HudRow("Spawned",icon=null,counters=spawned.map { bird ->
                    net.johnceo.sparklingmutuals.hud.HudIconCount(net.johnceo.sparklingmutuals.hud.UiIcons.Kind.BIRD,
                        birds.spawnedCount(bird), when (bird) {
                            "Bluebird" -> 0xFF5555FF.toInt()
                            "Parakeet" -> 0xFF55FF55.toInt()
                            else -> SafariRoster.named(bird)!!.color
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
                if (party.needs("Gimmiegold") && run?.sparklingComplete("Gimmiegold", party, localOnly = true) != true)
                    add(HudRow("Coins left to use", run?.coinsLeftToUse?.toString() ?: "?", HudRow.GOLD, HudRow.GRAY))
                if (party.needs("Gazer") && run?.sparklingComplete("Gazer", party, localOnly = true) != true)
                    add(HudRow("Gazers left", "${(4 - (run?.sparklingChecks?.checkCount("Gazer") ?: 0)).coerceAtLeast(0)}",
                        HudRow.GOLD, HudRow.GRAY))
            }
        }
        return HudPanel("Biome Objectives", biome.color, helpers.filter { it.value!="0" }.map { row ->
            row.copy(color = net.johnceo.sparklingmutuals.hud.ObjectivePalette.color(row.label),
                valueColor = if (row.value == "0") 0xFF55FF55.toInt() else HudRow.WHITE)
        }.ifEmpty { listOf(completedRow("All objectives complete!")) },net.johnceo.sparklingmutuals.hud.UiIcons.forLabel(biome.label))
    }
    private fun captureColor(complete: Boolean, verified: Boolean) = if (!ConfigManager.fullClearMode || !verified) HudRow.WHITE
        else if (complete) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
    fun captures(run: SafariRun?, biome: SafariBiome) = HudPanel("${biome.label} captures", biome.color, buildList {
        biome.critters.forEach { val count = run?.count(it.name) ?: 0
            add(HudRow(it.name, if (run?.inferredCapture(it.name) == true) "\u22651" else "$count", it.color, captureColor(run?.fullClearComplete(it.name) == true,
                run?.hasCaptureEvidence(biome) == true || run?.fullClearReportReceived(biome) == true ||
                    it.name == "Honeybug" && run?.beeNestCountKnown == true ||
                    it.name in SafariFullClear.fixedCaptureTargets && count > 0),
                complete = run?.let { current -> when(SafariFullClear.mode) {
                    SafariMode.FULL_CLEAR -> current.fullClearComplete(it.name)
                    SafariMode.UNIQUE -> count>0 || current.uniqueUnavailable(it.name)
                    SafariMode.SPARKLING -> !SafariSparklingMode.state.needs(it.name) || current.sparklingComplete(it.name,SafariSparklingMode.state)
                } })) }
        add(HudRow()); add(HudRow("Total captures", "${biome.critters.sumOf { run?.count(it.name) ?: 0 }}", HudRow.GRAY))
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
