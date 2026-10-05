package net.johnceo.sparklingmutuals.contest

import net.johnceo.sparklingmutuals.config.ContestConfig
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ContestGui(private val parent: Screen? = null) : Screen(Component.literal("Sparkling Mutuals HUD")) {

    private var dragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        graphics.fill(0, 0, width, height, 0x88000000.toInt())

        ContestHud.renderPreview(
            graphics,
            ContestConfig.hudX,
            ContestConfig.hudY
        )

        graphics.text(
            font,
            Component.literal("Drag the HUD to move it"),
            10,
            10,
            0xFFFFFFFF.toInt(),
            true
        )

        graphics.text(
            font,
            Component.literal("Press ESC when finished"),
            10,
            24,
            0xFFAAAAAA.toInt(),
            true
        )

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(
        event: MouseButtonEvent,
        doubleClick: Boolean
    ): Boolean {
        if (event.button() == 0 && isInsideHud(event.x(), event.y())) {
            dragging = true
            dragOffsetX = event.x().toInt() - ContestConfig.hudX
            dragOffsetY = event.y().toInt() - ContestConfig.hudY
            return true
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dx: Double,
        dy: Double
    ): Boolean {
        if (dragging && event.button() == 0) {
            val scaledWidth = (ContestHud.WIDTH * ContestConfig.hudScale).toInt()
            val scaledHeight = (ContestHud.HEIGHT * ContestConfig.hudScale).toInt()

            ContestConfig.hudX = (event.x().toInt() - dragOffsetX)
                .coerceIn(0, width - scaledWidth)

            ContestConfig.hudY = (event.y().toInt() - dragOffsetY)
                .coerceIn(0, height - scaledHeight)

            return true
        }

        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && dragging) {
            dragging = false
            ContestConfig.save()
            return true
        }

        return super.mouseReleased(event)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        if (isInsideHud(mouseX, mouseY)) {
            ContestConfig.hudScale = (
                    ContestConfig.hudScale + if (verticalAmount > 0) 0.1f else -0.1f
                    ).coerceIn(0.5f, 2.0f)

            ContestConfig.save()
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun onClose() {
        ContestConfig.save()
        minecraft.setScreen(parent)
    }

    private fun isInsideHud(mouseX: Double, mouseY: Double): Boolean {
        val scaledWidth = ContestHud.WIDTH * ContestConfig.hudScale.toDouble()
        val scaledHeight = ContestHud.HEIGHT * ContestConfig.hudScale.toDouble()

        return mouseX in ContestConfig.hudX.toDouble()..(ContestConfig.hudX + scaledWidth) &&
                mouseY in ContestConfig.hudY.toDouble()..(ContestConfig.hudY + scaledHeight)
    }
}
