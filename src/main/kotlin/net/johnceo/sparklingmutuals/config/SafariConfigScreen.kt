package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

class SafariConfigScreen(private val settings: SafariSettings = SafariSettings()) :
    MoulConfigScreenComponent(Component.literal("Sparkling Mutuals settings"), context(settings), null) {
    override fun tick() { settings.apply(notifyMode = true) }
    override fun removed() { settings.apply(notifyMode = true); settings.saveTextFields(); super.removed() }
    override fun isPauseScreen() = false
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, 0xDA16211A.toInt())
        SafariTheme.begin()
        try { super.extractRenderState(graphics, mouseX, mouseY, delta) } finally { SafariTheme.end() }
    }
    companion object {
        private fun context(settings: SafariSettings): GuiContext {
            val processor = MoulConfigProcessor.withDefaults(settings)
            ConfigProcessorDriver(processor).apply { checkExpose = false }.processConfig(settings)
            return GuiContext(GuiElementComponent(MoulConfigEditor(processor)))
        }
    }
}
