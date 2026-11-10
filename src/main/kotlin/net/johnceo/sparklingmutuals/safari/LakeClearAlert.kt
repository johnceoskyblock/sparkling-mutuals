package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Owned by one run; only confirmed personal catches qualify. */
class LakeCatchMilestone {
    private var announced = false
    fun consume(personalCount: (String) -> Int): Boolean {
        if (announced || species.any { personalCount(it) < 1 }) return false
        announced = true
        return true
    }
    companion object { val species = listOf("Tepid", "Nozzlenose", "Mantis Shrimp") }
}

object LakeClearAlert {
    private var owner: SafariRun? = null
    private var expires = 0L
    private fun now() = System.nanoTime() / 1_000_000
    fun show(run: SafariRun) { owner = run; expires = now() + 4000 }
    fun preview() = HudPanel("Lake Cleared", 0xFF55FF55.toInt(), emptyList(), titleIcon = null)
    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (!ConfigManager.lakeClearAlert || !SafariAssist.inSafari || owner !== SafariTracking.ledger.current ||
            owner?.endedAt != null || now() >= expires) { owner = null; return }
        if (client.player == null || client.options.smHideGui || client.smScreen is ContestGui) return
        preview().draw(graphics, SafariHud.LAKE_CLEARED)
    }
}
