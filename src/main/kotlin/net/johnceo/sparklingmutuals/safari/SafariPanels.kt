package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.config.ConfigManager

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
            val targets = if (SafariFullClear.mode == SafariMode.SPARKLING) critters.filter { party.needs(it.name) || run?.sparklePending(it.name) == true } else critters
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
        return biome.critters.filter { if (run?.sparklePending(it.name) == true) true else if (sparkling) {
                party.needs(it.name) && run?.sparklingComplete(it.name, party) != true ||
                    run?.sparklingChecks?.wumpaPrerequisite(it.name, party) == true && run.count(it.name) == 0
            }
            else if (SafariFullClear.mode == SafariMode.UNIQUE) (run?.count(it.name) ?: 0) == 0 && run?.uniqueComplete(it.name, party) != true
            else run?.missing(it, ConfigManager.fullClearMode) != false }
    }
    fun missing(run: SafariRun?, biome: SafariBiome, unique: Boolean, nests: Int, mounds: Int = 0,
        walls: WallSummary = WallSummary(emptyList()), party: PartySparklingState = SafariSparklingMode.state): HudPanel {
        val sparkling = SafariFullClear.mode == SafariMode.SPARKLING
        val missing = missingCritters(run, biome, party)
        // Needed checking stays visible after a Unique catch, using the same plain species row.
        val listed = biome.critters.filter { critter ->
            critter in missing || SafariFullClear.mode == SafariMode.UNIQUE && run != null &&
                run.count(critter.name) > 0 && party.needs(critter.name) && !run.uniqueComplete(critter.name, party)
        }
        val rows = listed.map { critter ->
            HudRow(critter.name, fixedCounter(critter.name, run, SafariFullClear.mode, party), critter.color, HudRow.GRAY)
        }.ifEmpty { listOf(HudRow(if (sparkling) "All checked!" else "All caught and checked!", color = biome.color)) }
        val helpers = buildList {
            val mode = SafariFullClear.mode
            if (biome == SafariBiome.FOREST && SafariHelperRules.showNestCounter(mode, run, party))
                add(HudRow("Bee nests to punch", "$nests", HudRow.GOLD, HudRow.GRAY))
            if (biome == SafariBiome.CAVERN) {
                if (SafariHelperRules.needed("Snoozle", mode, run, party) && walls.states.isNotEmpty() && !walls.allBroken)
                    add(HudRow("Snooper walls to break", walls.remaining, HudRow.GOLD, HudRow.GRAY))
                if (SafariHelperRules.needed("Rockmite", mode, run, party) && mounds > 0)
                    add(HudRow("Mounds to break", "$mounds", HudRow.GOLD, HudRow.GRAY))
            }
        }
        return HudPanel("${biome.label} Biome — ${listed.size} left", biome.color,
            rows + if (helpers.isEmpty()) emptyList() else listOf(HudRow()) + helpers)
    }
    private fun captureColor(complete: Boolean, verified: Boolean) = if (!ConfigManager.fullClearMode || !verified) HudRow.WHITE
        else if (complete) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
    fun captures(run: SafariRun?, biome: SafariBiome) = HudPanel("${biome.label} captures", biome.color, buildList {
        biome.critters.forEach { val count = run?.count(it.name) ?: 0
            add(HudRow(it.name, "$count", it.color, captureColor(run?.captureComplete(it.name) == true, run?.hasCaptureEvidence(biome) == true))) }
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
