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
        fun bar(label: String, critters: List<SafariCritter>, color: Int): HudRow {
            if (SafariFullClear.mode == SafariMode.SPARKLING && !party.ready) return HudRow(label, "?", color, color)
            val targets = if (SafariFullClear.mode == SafariMode.SPARKLING) critters.filter { party.needs(it.name) } else critters
            val count = run?.completed(SafariFullClear.mode, targets, party) ?: 0
            return HudRow(label, "$count/${targets.size}", color, color,
                if (targets.isEmpty()) 1f else count.toFloat() / targets.size)
        }
        return HudPanel(title, rows = listOf(bar(if (SafariFullClear.mode == SafariMode.SPARKLING) { if (party.ready) "Checked" else "Loading discoveries" } else "Collected", SafariRoster.all, HudRow.WHITE), HudRow()) +
            SafariBiome.entries.map { bar(it.label, it.critters, it.color) })
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
        val missing = missingCritters(run, biome, party)
        val listed = missing
        val rows = listed.map { critter ->
            HudRow(critter.name, fixedCounter(critter.name, run, SafariFullClear.mode, party), critter.color, HudRow.GRAY)
        }.ifEmpty { listOf(HudRow(if (sparkling) "All checked!"
            else if (SafariFullClear.mode == SafariMode.UNIQUE &&
                run?.completed(SafariMode.UNIQUE, biome.critters, party) != biome.critters.size) "All caught!"
            else "All caught and checked!", color = biome.color)) }
        val helpers = buildList {
            val mode = SafariFullClear.mode
            if (biome == SafariBiome.FOREST && SafariHelperRules.showNestCounter(mode, run, party))
                add(HudRow("Bee nests to punch", "$nests", HudRow.GOLD, HudRow.GRAY))
            if (biome == SafariBiome.FOREST && (mode == SafariMode.UNIQUE || sparkling &&
                    listOf("Bluebird", "Parakeet", "Macaw").any(party::needs) && run?.birdSpawnsComplete() != true))
                add(HudRow("Bird food left to use", run?.knownBirdFoodLeftToUse?.toString() ?: "?", HudRow.GOLD, HudRow.GRAY))
            if (biome == SafariBiome.CAVERN) {
                if (SafariHelperRules.needed("Snoozle", mode, run, party) && walls.states.isNotEmpty() && !walls.allBroken)
                    add(HudRow("Snooper walls to break", walls.remaining, HudRow.GOLD, HudRow.GRAY))
                if (SafariHelperRules.needed("Rockmite", mode, run, party) && mounds > 0)
                    add(HudRow("Mounds to break", "$mounds", HudRow.GOLD, HudRow.GRAY))
            }
            if (biome == SafariBiome.HAUNTED && (mode == SafariMode.UNIQUE || sparkling)) {
                if (party.needs("Gimmiegold") && run?.sparklingComplete("Gimmiegold", party, localOnly = true) != true)
                    add(HudRow("Coins left to use", run?.coinsLeftToUse?.toString() ?: "?", HudRow.GOLD, HudRow.GRAY))
                if (party.needs("Gazer") && run?.sparklingComplete("Gazer", party, localOnly = true) != true)
                    add(HudRow("Gazers left to check", "${(4 - (run?.sparklingChecks?.checkCount("Gazer") ?: 0)).coerceAtLeast(0)}",
                        HudRow.GOLD, HudRow.GRAY))
            }
        }
        return HudPanel("${biome.label} ${if (sparkling) "Unchecked" else "Biome"} — ${listed.size} left", biome.color,
            rows + if (helpers.isEmpty()) emptyList() else listOf(HudRow()) + helpers)
    }
    private fun captureColor(complete: Boolean, verified: Boolean) = if (!ConfigManager.fullClearMode || !verified) HudRow.WHITE
        else if (complete) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
    fun captures(run: SafariRun?, biome: SafariBiome) = HudPanel("${biome.label} captures", biome.color, buildList {
        biome.critters.forEach { val count = run?.count(it.name) ?: 0
            add(HudRow(it.name, if (run?.inferredCapture(it.name) == true) "\u22651" else "$count", it.color, captureColor(run?.fullClearComplete(it.name) == true,
                run?.hasCaptureEvidence(biome) == true || run?.fullClearReportReceived(biome) == true ||
                    it.name == "Honeybug" && run?.beeNestCountKnown == true ||
                    it.name in SafariFullClear.fixedCaptureTargets && count > 0))) }
        add(HudRow()); add(HudRow("Total captures", "${biome.critters.sumOf { run?.count(it.name) ?: 0 }}", HudRow.GRAY))
        if (biome == SafariBiome.CAVERN && ConfigManager.showMoundStats) {
            add(HudRow())
            add(HudRow("Rockmite Mounds", "${run?.brokenMounds ?: 0}", HudRow.GOLD,
                captureColor(run?.moundsComplete() == true, run?.hasCaptureEvidence(biome) == true && run.inheritedMounds.not())))
            add(HudRow("Mounds with Rockmite", "${run?.rockmiteMounds ?: 0}", HudRow.GOLD))
        }
    })
    fun previewRun() = SafariRun(0).apply {
        record(SafariCatch(SafariRoster.named("Treefrog")!!)); lastBiome = SafariBiome.FOREST
    }
}
