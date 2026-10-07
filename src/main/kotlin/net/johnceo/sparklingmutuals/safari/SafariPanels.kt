package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.config.ConfigManager

/** One collected total, with the v0.9.0 biome and critter rarity colors. */
object SafariPanels {
    fun progress(run: SafariRun?, unique: Boolean, now: Long): HudPanel {
        val seconds = (run?.elapsed(now) ?: 0) / 1000
        val time = "%d:%02d".format(seconds / 60, seconds % 60)
        val title = when { run == null -> "Critter Safari (ready)"; run.endedAt != null -> "Last Safari  $time"; else -> "Critter Safari  $time" }
        fun bar(label: String, critters: List<SafariCritter>, color: Int): HudRow {
            val count = run?.progress(critters, true) ?: 0
            return HudRow(label, "$count/${critters.size}", color, color, count.toFloat() / critters.size)
        }
        return HudPanel(title, rows = listOf(bar("Collected", SafariRoster.all, HudRow.WHITE), HudRow()) +
            SafariBiome.entries.map { bar(it.label, it.critters, it.color) })
    }
    fun missing(run: SafariRun?, biome: SafariBiome, unique: Boolean, nests: Int, mounds: Int = 0,
        walls: WallSummary = WallSummary(emptyList())): HudPanel {
        val missing = biome.critters.filter { run?.complete(it, !ConfigManager.fullClearMode) != true }
        val rows = missing.map { critter ->
            HudRow(critter.name, if (ConfigManager.fullClearMode && critter.quota > 1) "${run?.count(critter.name) ?: 0}/${critter.quota}" else null,
                critter.color, HudRow.GRAY)
        }.ifEmpty { listOf(HudRow("All caught!", color = biome.color)) }
        val footer = buildList {
            if (biome == SafariBiome.FOREST && ConfigManager.showBeeNests) {
                add(HudRow()); add(HudRow("Bee nests to punch", "$nests", HudRow.GOLD, HudRow.GRAY))
            }
            if (biome == SafariBiome.CAVERN) {
                if (ConfigManager.showSnooperWalls && walls.states.isNotEmpty()) {
                    val row = when {
                        !walls.allBroken -> HudRow("Snooper walls to break", walls.remaining, HudRow.GOLD, HudRow.GRAY)
                        run != null && !run.encountered("Snoozle") -> HudRow("No snoozles this run", color = HudRow.GRAY)
                        else -> null
                    }
                    if (row != null) { add(HudRow()); add(row) }
                }
                if (ConfigManager.showMoundCount && mounds > 0) {
                    add(HudRow()); add(HudRow("Mounds to break", "$mounds", HudRow.GOLD, HudRow.GRAY))
                } else if (ConfigManager.showMoundCount && run?.allMoundsBroken == true && !run.encountered("Rockmite")) {
                    add(HudRow()); add(HudRow("No rockmites this run", color = HudRow.GRAY))
                }
            }
        }
        return HudPanel("${biome.label} Biome — ${missing.size} left", biome.color, rows + footer)
    }
    private fun captureColor(count: Int, minimum: Int) = if (!ConfigManager.fullClearMode) HudRow.WHITE
        else if (count >= minimum) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
    fun captures(run: SafariRun?, biome: SafariBiome) = HudPanel("${biome.label} captures", biome.color, buildList {
        biome.critters.forEach { val count = run?.count(it.name) ?: 0
            add(HudRow(it.name, "$count", it.color, if (!ConfigManager.fullClearMode) HudRow.WHITE
                else if (run?.captureComplete(it.name) == true) 0xFF55FF55.toInt() else 0xFFFF5555.toInt())) }
        add(HudRow()); add(HudRow("Total captures", "${biome.critters.sumOf { run?.count(it.name) ?: 0 }}", HudRow.GRAY))
        if (biome == SafariBiome.CAVERN && ConfigManager.showMoundStats) {
            add(HudRow())
            add(HudRow("Rockmite Mounds", "${run?.brokenMounds ?: 0}", HudRow.GOLD,
                captureColor(run?.brokenMounds ?: 0, SafariFullClear.MOUND_MINIMUM)))
            add(HudRow("Mounds with Rockmite", "${run?.rockmiteMounds ?: 0}", HudRow.GOLD))
        }
    })
    fun previewRun() = SafariRun(0).apply {
        record(SafariCatch(SafariRoster.named("Treefrog")!!)); lastBiome = SafariBiome.FOREST
    }
}
