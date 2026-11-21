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
    fun consume(run: SafariRun, mode: SafariMode): Boolean {
        if (announced) return false
        val cleared = when (mode) {
            SafariMode.FULL_CLEAR -> species.all { run.captureComplete(it, personalOnly = true) }
            SafariMode.UNIQUE, SafariMode.SPARKLING -> species.all { run.personalCount(it) > 0 }
        }
        if (!cleared) return false
        announced = true
        return true
    }
    companion object { val species = listOf("Tepid", "Nozzlenose", "Mantis Shrimp") }
}

object LakeClearAlert {
    private var owner: SafariRun? = null
    private var expires = 0L
    private fun now() = System.nanoTime() / 1_000_000
    fun check(run: SafariRun, mode: SafariMode) {
        if (!ConfigManager.lakeClearAlert || !run.lakeCatchMilestone.consume(run, mode)) return
        owner = run
        expires = now() + 4000
    }
    fun preview() = HudPanel("Lake Cleared", 0xFF55FF55.toInt(), emptyList(), titleIcon = null)
    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (!ConfigManager.lakeClearAlert || !SafariAssist.inSafari || owner !== SafariTracking.ledger.current ||
            owner?.endedAt != null || now() >= expires) { owner = null; return }
        if (client.player == null || client.options.smHideGui || client.smScreen is ContestGui) return
        preview().draw(graphics, SafariHud.LAKE_CLEARED)
    }
}
