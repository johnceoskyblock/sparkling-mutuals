package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class StandaloneDescription

/** Full-width prose at normal font size; its height grows instead of shrinking the text. */
class StandaloneDescriptionEditor(option: ProcessedOption) : GuiOptionEditor(option) {
    private var contentWidth = 250
    override fun getHeight(): Int {
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val lines = font.splitText(option.description, (contentWidth - 16).coerceAtLeast(40))
        return 12 + font.height + 8 + lines.size * (font.height + 3) + 12
    }

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        contentWidth = width
        val font = context.minecraft.defaultFontRenderer
        context.drawString(font, option.name, x + 8, y + 8, SafariTheme.ACCENT, true)
        var lineY = y + 8 + font.height + 8
        for (line in font.splitText(option.description, (width - 16).coerceAtLeast(40))) {
            context.drawString(font, line, x + 8, lineY, SafariTheme.TEXT, false)
            lineY += font.height + 3
        }
    }
}
