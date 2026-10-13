package net.johnceo.sparklingmutuals.contest

import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

object ContestHud {

    const val WIDTH = 150
    const val HEIGHT = 42

    fun register() {
    }

    fun render(
        graphics: GuiGraphicsExtractor,
        _deltaTracker: DeltaTracker
    ) {
        val client = Minecraft.getInstance()

        if (client.player == null || client.level == null) return
        if (client.screen is ContestGui) return
        if (!ContestConfig.trackContest) return
        if (!ContestTracker.isActive()) return
        if (!SkyblockSidebar.inSkyblock(client)) return

        renderAt(
            graphics,
            ContestConfig.hudX,
            ContestConfig.hudY,
            false
        )
    }

    fun renderPreview(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int
    ) {
        renderAt(graphics, x, y, true)
    }

    private fun renderAt(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        preview: Boolean
    ) {
        val scale = ContestConfig.hudScale
        val client = Minecraft.getInstance()

        graphics.pose().pushMatrix()
        graphics.pose().translate(x.toFloat(), y.toFloat())
        graphics.pose().scale(scale, scale)

        AppearanceConfig.panels.getValue("miria").draw(graphics, WIDTH, HEIGHT)

        graphics.text(
            client.font,
            Component.literal("Safari Contest"),
            6,
            5,
            (0xFF000000.toInt() or net.johnceo.sparklingmutuals.config.SafariTheme.ACCENT),
            true
        )

        val statusText = if (preview) {
            "5:00 remaining"
        } else {
            "${ContestTracker.formatRemaining(ContestTracker.secondsToEnd())} remaining"
        }

        graphics.text(
            client.font,
            Component.literal(statusText),
            6,
            17,
            0xFFFFFF55.toInt(),
            true
        )

        val tier = ContestTracker.currentTier()
        val tierText = if (preview) {
            "Uncommon • 1,234"
        } else if (tier.isEmpty()) {
            "No tier yet"
        } else {
            "$tier • ${ContestTracker.currentPoints()}"
        }

        val tierColor = if (preview) {
            0xFF55FF55.toInt()
        } else if (tier.isEmpty()) {
            0xFFAAAAAA.toInt()
        } else {
            tierColor(tier)
        }

        graphics.text(
            client.font,
            Component.literal(tierText),
            6,
            29,
            tierColor,
            true
        )

        graphics.pose().popMatrix()
    }

    private fun tierColor(tier: String): Int =
        when (tier.uppercase()) {
            "COMMON" -> 0xFFFFFFFF.toInt()
            "UNCOMMON" -> 0xFF55FF55.toInt()
            "RARE" -> 0xFF5555FF.toInt()
            "EPIC" -> 0xFFAA00AA.toInt()
            "LEGENDARY" -> 0xFFFFAA00.toInt()
            "MYTHIC" -> 0xFFFF55FF.toInt()
            "DIVINE" -> 0xFF55FFFF.toInt()
            "SPECIAL" -> 0xFFFF5555.toInt()
            else -> 0xFFAAAAAA.toInt()
        }
}
