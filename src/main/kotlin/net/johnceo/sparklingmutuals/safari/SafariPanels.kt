package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow

/** One collected total, with the v0.9.0 biome and critter rarity colors. */
object SafariPanels {
    fun progress(run: SafariRun?, unique: Boolean, now: Long): HudPanel {
        val seconds = (run?.elapsed(now) ?: 0) / 1000
        val time = "%d:%02d".format(seconds / 60, seconds % 60)
        val title = when { run == null -> "Critter Safari (ready)"; run.endedAt != null -> "Last Safari  $time"; else -> "Critter Safari  $time" }
        fun bar(label: String, critters: List<SafariCritter>, color: Int): HudRow {
            val count = run?.progress(critters, unique) ?: 0
            return HudRow(label, "$count/${critters.size}", color, color, count.toFloat() / critters.size)
        }
        return HudPanel(title, rows = listOf(bar("Collected", SafariRoster.all, HudRow.WHITE), HudRow()) +
            SafariBiome.entries.map { bar(it.label, it.critters, it.color) })
    }
    fun missing(run: SafariRun?, biome: SafariBiome, unique: Boolean, nests: Int): HudPanel {
        val missing = biome.critters.filter { run?.complete(it, unique) != true }
        val rows = missing.map { critter ->
            HudRow(critter.name, if (critter.required(unique) > 1) "${run?.count(critter.name) ?: 0}/${critter.required(unique)}" else null,
                critter.color, HudRow.GRAY)
        }.ifEmpty { listOf(HudRow("All caught!", color = biome.color)) }
        val footer = if (biome == SafariBiome.FOREST) listOf(HudRow(), HudRow("Bee nests to punch", "$nests", HudRow.GOLD, HudRow.GRAY)) else emptyList()
        return HudPanel("${biome.label} Biome — ${missing.size} left", biome.color, rows + footer)
    }
    fun captures(run: SafariRun?, biome: SafariBiome) = HudPanel("${biome.label} captures", biome.color,
        biome.critters.map { HudRow(it.name, "${run?.count(it.name) ?: 0}", it.color, HudRow.WHITE) } +
            listOf(HudRow(), HudRow("Total captures", "${biome.critters.sumOf { run?.count(it.name) ?: 0 }}", HudRow.GRAY)))
    fun previewRun() = SafariRun(0).apply {
        record(SafariCatch(SafariRoster.named("Treefrog")!!)); lastBiome = SafariBiome.FOREST
    }
}
