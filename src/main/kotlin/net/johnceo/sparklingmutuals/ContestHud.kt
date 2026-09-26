package net.johnceo.sparklingmutuals

import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

object ContestHud {

    const val WIDTH = 150
    const val HEIGHT = 42

    fun register() {
        // Registered from SparklingMutuals.onInitialize().
    }

    fun render(
        graphics: GuiGraphicsExtractor,
        deltaTracker: DeltaTracker
    ) {
        val client = Minecraft.getInstance()

        if (client.player == null || client.level == null) {
            return
        }

        if (!ContestTracker.isActive()) {
            return
        }

        if (!SkyblockSidebar.inSkyblock(client)) {
            return
        }

        val x = ContestConfig.hudX
        val y = ContestConfig.hudY

        renderAt(
            graphics,
            x,
            y,
            false
        )
    }

    fun renderPreview(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int
    ) {
        renderAt(
            graphics,
            x,
            y,
            true
        )
    }

    private fun renderAt(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        preview: Boolean
    ) {
        val scale = ContestConfig.hudScale

        graphics.pose().pushMatrix()
        graphics.pose().translate(x.toFloat(), y.toFloat())
        graphics.pose().scale(scale, scale)

        val backgroundColor = 0xCC000000.toInt()
        val borderColor = 0xFFFFFFFF.toInt()

        graphics.fill(
            0,
            0,
            WIDTH,
            HEIGHT,
            backgroundColor
        )

        graphics.fill(0, 0, WIDTH, 1, borderColor)
        graphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, borderColor)
        graphics.fill(0, 0, 1, HEIGHT, borderColor)
        graphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, borderColor)

        val client = Minecraft.getInstance()

        graphics.text(
            client.font,
            Component.literal("Safari Contest"),
            6,
            5,
            0xFFFFFFFF.toInt(),
            true
        )

        val statusText =
            if (preview) {
                "5:00 remaining"
            } else {
                ContestTracker.formatRemaining(
                    ContestTracker.secondsToEnd()
                ) + " remaining"
            }

        graphics.text(
            client.font,
            Component.literal(statusText),
            6,
            17,
            0xFFFFFF55.toInt(),
            true
        )

        val tierText =
            if (preview) {
                "Uncommon • 1,234"
            } else {
                val tier = ContestTracker.currentTier()

                if (tier.isEmpty()) {
                    "No tier yet"
                } else {
                    "$tier • ${ContestTracker.currentPoints()}"
                }
            }

        val tierColor =
            if (preview) {
                0xFF55FF55.toInt()
            } else {
                val tier = ContestTracker.currentTier()

                if (tier.isEmpty()) {
                    0xFFAAAAAA.toInt()
                } else {
                    tierColor(tier)
                }
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

    private fun tierColor(tier: String): Int {
        return when (tier.uppercase()) {
            "COMMON" -> 0xFFFFFFFF.toInt()       // White
            "UNCOMMON" -> 0xFF55FF55.toInt()     // Green
            "RARE" -> 0xFF5555FF.toInt()         // Blue
            "EPIC" -> 0xFFAA00AA.toInt()         // Dark Purple
            "LEGENDARY" -> 0xFFFFAA00.toInt()    // Gold
            "MYTHIC" -> 0xFFFF55FF.toInt()       // Light Purple
            "DIVINE" -> 0xFF55FFFF.toInt()       // Aqua
            "SPECIAL" -> 0xFFFF5555.toInt()      // Red
            else -> 0xFFAAAAAA.toInt()           // Gray fallback
        }
    }
}