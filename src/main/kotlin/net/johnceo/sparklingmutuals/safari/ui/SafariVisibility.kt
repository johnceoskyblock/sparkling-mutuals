package net.johnceo.sparklingmutuals.safari.ui

import net.johnceo.sparklingmutuals.safari.SafariBiome

object SafariVisibility {
    fun visible(where: Int, inSafari: Boolean, entrance: Boolean) = where == 2 || inSafari || (where == 1 && entrance)
    fun biomePanels(where: Int, inSafari: Boolean, activeRun: Boolean, biome: SafariBiome?) =
        visible(where, inSafari, false) && inSafari && activeRun && biome != null
}
