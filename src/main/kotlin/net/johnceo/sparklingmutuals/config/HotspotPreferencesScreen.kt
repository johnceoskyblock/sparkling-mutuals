package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.hud.HotspotGrid
import net.johnceo.sparklingmutuals.hud.HudFonts
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.Collections

internal class HotspotPreferencesScreen(private val parent: SafariConfigScreen? = null) :
    Screen(Component.literal("Biome Hotspot Preferences")) {
    private val order = HotspotPreferences.order.toMutableList()
    private val panelWidth get() = (width - 24).coerceAtMost(300)
    private val left get() = (width - panelWidth) / 2
    private val top get() = (height - PANEL_HEIGHT) / 2
    private var saveFailed = false

    override fun init() {
        order.forEachIndexed { rank, biome ->
            val y = rowY(rank)
            arrow("↑", "Move ${biome.label} up", left + panelWidth - 62, y, rank, -1)
            arrow("↓", "Move ${biome.label} down", left + panelWidth - 38, y, rank, 1)
        }
        addRenderableWidget(ThemedButton.builder(Component.literal("Cancel")) { onClose() }
            .bounds(left + panelWidth - 128, top + 162, 55, 20).build())
        addRenderableWidget(ThemedButton.builder(Component.literal("Save")) {
            try {
                HotspotPreferences.update(order)
                onClose()
            } catch (_: java.io.IOException) {
                saveFailed = true
            }
        }.bounds(left + panelWidth - 67, top + 162, 55, 20).primary().build())
    }

    private fun rowY(rank: Int) = top + 47 + rank * 25

    private fun arrow(symbol: String, label: String, x: Int, y: Int, rank: Int, direction: Int) {
        val destination = rank + direction
        val button = ThemedButton.builder(Component.literal(symbol)) {
            Collections.swap(order, rank, destination)
            rebuildWidgets()
        }.bounds(x, y + 1, 20, 20).build()
        button.active = destination in order.indices
        button.setTooltip(Tooltip.create(Component.literal(label)))
        addRenderableWidget(button)
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        parent?.extractRenderState(g, -1, -1, delta)
        g.fill(0, 0, width, height, 0xB8000000.toInt())
        g.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, SettingsPalette.border)
        g.fill(left + 1, top + 1, left + panelWidth - 1, top + PANEL_HEIGHT - 1, SettingsPalette.surface)
        val font = HudFonts.font
        g.text(font, title, left + 12, top + 11, SettingsPalette.accent, false)
        g.text(font, "Higher-ranked biomes take priority in ties.", left + 12, top + 28, SettingsPalette.muted, false)
        order.forEachIndexed { rank, biome ->
            val y = rowY(rank)
            g.fill(left + 12, y, left + panelWidth - 12, y + 22, SettingsPalette.card)
            g.text(font, "${rank + 1}", left + 20, y + 7, SettingsPalette.muted, false)
            g.text(font, biome.label, left + 38, y + 7, HotspotGrid.biomeColor(biome), false)
        }
        if (saveFailed) g.text(font, "Could not save.", left + 12, top + 168, SettingsPalette.red, false)
        super.extractRenderState(g, mouseX, mouseY, delta)
    }

    override fun onClose() { minecraft.smSetScreen(parent) }
    override fun isPauseScreen() = false

    private companion object {
        const val PANEL_HEIGHT = 194
    }
}
