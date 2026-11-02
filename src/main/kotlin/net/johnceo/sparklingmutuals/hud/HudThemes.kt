// Live biome/ticket borders adapted from SafariUtils HudBorderStyle (MIT).
package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.config.BannerConfig
import net.johnceo.sparklingmutuals.contest.ContestTracker
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

object HudThemes {
    fun border(hud: SafariHud): Int? {
        if(!BannerConfig.biomeBorders) return null
        val biome=SafariAssist.biome ?: return null
        if(hud !in setOf(SafariHud.PROGRESS,SafariHud.MISSING,SafariHud.CAPTURES,SafariHud.OBJECTIVES,SafariHud.SPARKLINGS)) return null
        if(hud==SafariHud.MISSING) {
            val run=SafariTracking.run
            val party=SafariSparklingMode.state
            val targets=biome.critters.filter { SafariFullClear.mode!=SafariMode.SPARKLING || party.needs(it.name) }
            if((SafariFullClear.mode!=SafariMode.SPARKLING || party.ready) && run?.completed(SafariFullClear.mode,targets,party)==targets.size) return 0xFF55FF55.toInt()
        }
        return biome.color
    }
    fun ticketColor(earned: Boolean,inactive: Boolean,tier: String): Int = when {
        inactive -> 0xFFE0B845.toInt()
        !earned -> 0xFFFF5555.toInt()
        else -> tierColor(tier)
    }
    fun tierColor(tier: String): Int=when(tier.uppercase()) {
        "COMMON" -> -1;"UNCOMMON" -> 0xFF55FF55.toInt();"RARE" -> 0xFF5555FF.toInt()
        "EPIC" -> 0xFFAA00AA.toInt();"LEGENDARY" -> 0xFFFFAA00.toInt();else -> 0xFF55FF55.toInt()
    }
}
