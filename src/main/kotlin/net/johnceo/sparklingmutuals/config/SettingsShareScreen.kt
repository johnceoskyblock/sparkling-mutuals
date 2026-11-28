package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.hud.HudFonts
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

internal class SettingsShareScreen(private val parent: SafariConfigScreen) : Screen(Component.literal("Share Config")) {
    private val panelWidth get() = (width - 24).coerceIn(16, 344)
    private val left get() = (width - panelWidth) / 2
    private val top get() = (height - 130) / 2
    private var pending: SettingsShare.Snapshot? = null
    private var status = ""
    private var error = false

    override fun init() {
        val buttonWidth = ((panelWidth - 36) / 3).coerceAtLeast(1)
        fun button(index: Int, text: String, action: () -> Unit) {
            addRenderableWidget(ThemedButton.builder(Component.literal(text)) { action() }
                .bounds(left + 12 + index * (buttonWidth + 6), top + 94, buttonWidth, 22).build())
        }
        button(0, "Copy code") {
            pending = null
            runCatching { minecraft.keyboardHandler.clipboard = SettingsShare.exportCode() }
                .onSuccess { setStatus("Settings code copied.") }
                .onFailure { setStatus("Could not copy the settings code.", true) }
            rebuildWidgets()
        }
        button(1, if (pending == null) "Paste code" else "Apply settings") {
            val preview = pending
            if (preview == null) {
                runCatching { SettingsShare.preview(minecraft.keyboardHandler.clipboard) }
                    .onSuccess {
                        pending = it
                        setStatus("${it.size} settings ready. Apply to replace matching settings.")
                    }.onFailure { setStatus("Clipboard doesn't contain a compatible Mutuals settings code.", true) }
                rebuildWidgets()
            } else {
                runCatching { SettingsShare.apply(preview) }
                    .onSuccess { minecraft.smSetScreen(parent.freshForReturn()) }
                    .onFailure {
                        pending = null
                        setStatus("Could not apply the settings code.", true)
                        rebuildWidgets()
                    }
            }
        }
        button(2, "Back", ::onClose)
    }

    private fun setStatus(message: String, failed: Boolean = false) {
        status = message
        error = failed
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        parent.extractRenderState(g, -1, -1, delta)
        g.fill(0, 0, width, height, 0xB8000000.toInt())
        g.fill(left, top, left + panelWidth, top + 130, SettingsPalette.surface)
        g.outline(left, top, panelWidth, 130, SettingsPalette.border)
        val font = HudFonts.font
        g.text(font, title, left + 12, top + 11, SettingsPalette.accent, false)
        fun lines(text: String, y: Int, color: Int) {
            font.split(Component.literal(text), (panelWidth - 24).coerceAtLeast(1)).take(3).forEachIndexed { index, line ->
                g.text(font, line, left + 12, y + index * 11, color, false)
            }
        }
        lines("Share settings and HUD layouts across instances. Account details and saved Safari records are excluded.",
            top + 30, SettingsPalette.muted)
        lines(status, top + 67, if (error) SettingsPalette.red else SettingsPalette.text)
        super.extractRenderState(g, mx, my, delta)
    }

    override fun onClose() = minecraft.smSetScreen(parent)
    override fun isPauseScreen() = false
}
