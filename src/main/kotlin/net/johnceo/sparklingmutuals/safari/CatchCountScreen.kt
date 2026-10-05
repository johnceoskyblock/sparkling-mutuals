package net.johnceo.sparklingmutuals.safari

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class CatchCountScreen(private val parent: Screen? = null) : Screen(Component.literal("Safari captures")) {
    private var selected = SafariAssist.biome ?: SafariBiome.FOREST
    override fun init() {
        SafariBiome.entries.forEachIndexed { i, biome ->
            addRenderableWidget(Button.builder(Component.literal(biome.label)) { selected = biome }
                .bounds(width / 2 - 164 + i * 83, 28, 80, 20).build())
        }
        addRenderableWidget(Button.builder(Component.literal("Back")) { onClose() }
            .bounds(width / 2 - 50, height - 30, 100, 20).build())
    }
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0xF016211A.toInt())
        val biome = selected
        SafariTracking.panel(graphics, width / 2 - 128, 58, "${biome.label} · ${if (SafariTracking.ledger.current != null) "Current run" else "Last run"}", SafariTracking.countRows(biome))
        graphics.centeredText(font, Component.literal("One capture per message · resets each run"), width / 2, height - 46, 0xFFE3BB67.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
    override fun onClose() { minecraft.setScreen(parent) }
    override fun isPauseScreen() = false
}
