package net.johnceo.sparklingmutuals.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence

class ApiKeyScreen(private val parent: Screen?) : Screen(Component.literal("Hypixel API key")) {
    private var draft = ConfigManager.apiKey
    private var reveal = false
    private var status = "Get a key from developer.hypixel.net"
    private lateinit var key: EditBox
    override fun init() {
        val panelWidth = (width - 24).coerceAtMost(400)
        val x = (width - panelWidth) / 2
        val y = height / 2 - 40
        key = EditBox(font, x + 4, y, panelWidth - 8, 24, Component.literal("Hypixel API key")).also {
            it.setMaxLength(128)
            it.value = draft
            it.setResponder { value -> draft = value }
            if (!reveal) it.addFormatter { value, _ -> FormattedCharSequence.forward("*".repeat(value.length), Style.EMPTY) }
            addRenderableWidget(it)
        }
        val buttonWidth = (panelWidth - 12) / 3
        fun control(index: Int, label: String, action: () -> Unit) {
            addRenderableWidget(Button.builder(Component.literal(label)) { action() }
                .bounds(x + index * (buttonWidth + 6), y + 36, buttonWidth, 20).build())
        }
        control(0, if (reveal) "Hide key" else "Show key") { reveal = !reveal; rebuildWidgets() }
        control(1, "Save key") {
            ConfigManager.apiKey = draft.trim()
            ConfigManager.save()
            status = if (draft.isBlank()) "API key cleared" else "API key saved"
        }
        control(2, "Back") { onClose() }
    }
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0xF016211A.toInt())
        graphics.centeredText(font, title, width / 2, height / 2 - 66, 0xFFE3BB67.toInt())
        graphics.centeredText(font, Component.literal(status), width / 2, height / 2 + 31, 0xFFF0E1BE.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
    override fun onClose() { minecraft.setScreen(parent) }
    override fun isPauseScreen() = false
}
