package net.johnceo.sparklingmutuals.config

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.compat.smSetScreen
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

internal class HideyhoKeySetupScreen(private val parent: Screen?, private val binding: KeyMapping) :
    Screen(Component.literal("Accept Hideyho Quest")) {
    private var listening = false
    private var conflict: Component? = null
    private var panelWidth = 0
    private var panelTop = 0

    override fun init() {
        panelWidth = (width - 24).coerceAtMost(360)
        panelTop = height / 2 - 68
        val buttonWidth = (panelWidth - 38) / 2
        val center = width / 2
        val bindLabel = when {
            listening -> "Press a key or mouse button"
            binding.isUnbound -> "Bind a key"
            else -> "Change key"
        }
        addRenderableWidget(ThemedButton.builder(Component.literal(bindLabel)) {
            listening = true
            conflict = null
            rebuildWidgets()
        }.primary().bounds(center - buttonWidth - 4, panelTop + 102, buttonWidth, 20).build())
        addRenderableWidget(ThemedButton.builder(Component.literal(if (binding.isUnbound) "Skip for now" else "Done")) {
            onClose()
        }.bounds(center + 4, panelTop + 102, buttonWidth, 20).build())
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        g.fill(0, 0, width, height, SettingsPalette.background)
        val x = (width - panelWidth) / 2
        g.fill(x, panelTop, x + panelWidth, panelTop + 136, SettingsPalette.surface)
        g.fill(x, panelTop, x + panelWidth, panelTop + 2, SettingsPalette.accent)
        g.centeredText(font, title, width / 2, panelTop + 15, SettingsPalette.text)
        val description = Component.literal("Talk to Hideyho first. Then press your key when the quest choice appears in chat.")
        font.split(description, panelWidth - 24).forEachIndexed { i, line ->
            g.centeredText(font, line, width / 2, panelTop + 36 + 11 * i, SettingsPalette.muted)
        }
        val status = conflict ?: when {
            listening -> Component.literal("Press Esc to cancel")
            binding.isUnbound -> Component.literal("No key assigned")
            else -> Component.literal("Assigned key ").append(binding.translatedKeyMessage)
        }
        g.centeredText(font, status, width / 2, panelTop + 77,
            if (conflict != null) SettingsPalette.red else SettingsPalette.accent)
        super.extractRenderState(g, mouseX, mouseY, delta)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!listening) return super.keyPressed(event)
        if (event.isEscape) finishCapture() else bind(InputConstants.getKey(event))
        return true
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!listening) return super.mouseClicked(event, doubleClick)
        bind(InputConstants.Type.MOUSE.getOrCreate(event.button()))
        return true
    }

    private fun bind(key: InputConstants.Key) {
        if (key == InputConstants.UNKNOWN) return
        val other = minecraft.options.keyMappings.firstOrNull { it !== binding && it.saveString() == key.name }
        if (other != null) {
            conflict = Component.literal("Already used by ").append(Component.translatable(other.name))
            return
        }
        binding.setKey(key)
        KeyMapping.resetMapping()
        minecraft.options.save()
        finishCapture()
    }

    private fun finishCapture() {
        listening = false
        conflict = null
        rebuildWidgets()
    }

    override fun onClose() { minecraft.smSetScreen(parent) }
    override fun isPauseScreen() = false
}
