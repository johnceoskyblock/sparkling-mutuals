package net.johnceo.sparklingmutuals.safari

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class CatchCountScreen(private val parent: Screen? = null) : Screen(Component.literal("Safari captures")) {
    private var selected = SafariTracking.displayBiome ?: SafariBiome.FOREST
    override fun init() {
        val buttonWidth = ((width - 20) / 4).coerceIn(1, 80)
        SafariBiome.entries.forEachIndexed { i, biome ->
            addRenderableWidget(Button.builder(Component.literal(biome.label)) { selected = biome }
                .bounds(width / 2 - 2 * (buttonWidth + 3) + i * (buttonWidth + 3), 28, buttonWidth, 20).build())
        }
        addRenderableWidget(Button.builder(Component.literal("Back")) { onClose() }.bounds(width / 2 - 50, height - 26, 100, 20).build())
    }
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, (0xCC000000.toInt() or net.johnceo.sparklingmutuals.config.SafariTheme.BACKGROUND))
        val panel = SafariPanels.captures(SafariTracking.run, selected)
        val panelWidth = panel.width(font::width)
        val scale = minOf(1f, (width - 12).toFloat() / panelWidth, (height - 100).coerceAtLeast(1).toFloat() / panel.height)
        panel.draw(graphics, ((width - panelWidth * scale) / 2).toInt(), 58, scale)
        val label = when { SafariTracking.ledger.current != null -> "Current run"; SafariTracking.run != null -> "Last run"; else -> "Waiting for a Safari run" }
        graphics.centeredText(font, Component.literal(label), width / 2, 10, selected.color)
        graphics.centeredText(font, Component.literal("Counts stay until the next Safari run"), width / 2, height - 40, 0xFFAAAAAA.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
    override fun onClose() { minecraft.setScreen(parent) }
    override fun isPauseScreen() = false
}
