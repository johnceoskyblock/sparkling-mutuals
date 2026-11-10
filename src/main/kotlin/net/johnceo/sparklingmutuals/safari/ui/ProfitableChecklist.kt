package net.johnceo.sparklingmutuals.safari.ui

import net.johnceo.sparklingmutuals.commands.ProfitableCritters
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.safari.*

/** Exact /profit selection, filtered after ranking; never a separate top ten per biome. */
object ProfitableChecklist {
    fun targets(biome: SafariBiome, ranked: List<String>) = ranked.distinct().take(10)
        .mapNotNull(SafariRoster::named).filter { it.biome == biome }
    fun panel(run: SafariRun?, biome: SafariBiome, ranked: List<String> = ProfitableCritters.cachedTop().map { it.name }): HudPanel {
        val selected = targets(biome, ranked)
        val rows = selected.map { critter ->
            val left = run?.profitableCapturesLeft(critter.name)
            val pending = run?.profitableSourcesPending(critter.name) == true
            val value = when {
                pending && (left == null || left == 0) -> if (critter.name == "Snoozle") "Check walls" else "Use bird food"
                pending -> "$left+ left"
                left == 0 -> "All caught"
                else -> left?.let { "$it left" } ?: "? left"
            }
            HudRow(critter.name, value, critter.color,
                if (left == 0 && !pending) 0xFF55FF55.toInt() else HudRow.WHITE, icon = null)
        }
        return HudPanel("Profitable ${biome.label} mobs", biome.color, rows.ifEmpty {
            listOf(HudRow(if (ranked.isEmpty()) "Prices loading…" else "No top-ten mobs here", color = HudRow.GRAY, icon = null))
        })
    }
    fun preview() = panel(SafariRun(0), SafariBiome.FOREST, listOf("Foxtrot", "Bluebird", "Honeybug"))
}
