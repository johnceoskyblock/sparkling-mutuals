package net.johnceo.sparklingmutuals

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ContestGuiScreen : Screen(
    Component.literal("Sparkling Mutuals HUD")
) {

    private var dragging = false

    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun init() {
        super.init()

    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {

        graphics.fill(
            0,
            0,
            width,
            height,
            0x88000000.toInt()
        )

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

        super.extractRenderState(
            graphics,
            mouseX,
            mouseY,
            delta
        )
    }

    override fun mouseClicked(
        event: MouseButtonEvent,
        doubleClick: Boolean
    ): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()

        if (event.button() == 0 && isInsideHud(mouseX, mouseY)) {
            dragging = true

            dragOffsetX =
                mouseX.toInt() - ContestConfig.hudX

            dragOffsetY =
                mouseY.toInt() - ContestConfig.hudY

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
            val newX =
                event.x().toInt() - dragOffsetX

            val newY =
                event.y().toInt() - dragOffsetY

            val scaledWidth =
                (ContestHud.WIDTH * ContestConfig.hudScale).toInt()

            val scaledHeight =
                (ContestHud.HEIGHT * ContestConfig.hudScale).toInt()

            ContestConfig.hudX =
                newX.coerceIn(0, width - scaledWidth)

            ContestConfig.hudY =
                newY.coerceIn(0, height - scaledHeight)

            return true
        }

        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseReleased(
        event: MouseButtonEvent
    ): Boolean {
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
            val change = if (verticalAmount > 0) {
                0.1f
            } else {
                -0.1f
            }

            ContestConfig.hudScale =
                (ContestConfig.hudScale + change)
                    .coerceIn(0.5f, 2.0f)

            ContestConfig.save()

            return true
        }

        return super.mouseScrolled(
            mouseX,
            mouseY,
            horizontalAmount,
            verticalAmount
        )
    }

    override fun onClose() {
        ContestConfig.save()
        minecraft?.setScreen(null)
    }

    private fun isInsideHud(
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        val scaledWidth =
            ContestHud.WIDTH * ContestConfig.hudScale

        val scaledHeight =
            ContestHud.HEIGHT * ContestConfig.hudScale

        return mouseX >= ContestConfig.hudX &&
                mouseX <= ContestConfig.hudX + scaledWidth &&
                mouseY >= ContestConfig.hudY &&
                mouseY <= ContestConfig.hudY + scaledHeight
    }
}