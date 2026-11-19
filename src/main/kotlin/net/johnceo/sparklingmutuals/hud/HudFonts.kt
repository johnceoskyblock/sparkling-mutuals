package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.mixin.HudFontAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier

/** Shares Minecraft's reload-aware glyph provider; measurement and drawing use the same font. */
object HudFonts {
    private val standard = FontDescription.Resource(Identifier.fromNamespaceAndPath("sparkling-mutuals", "hud"))
    private var source: Font? = null
    private var fallback: Font? = null
    val font: Font get() {
        val current = Minecraft.getInstance().font
        if (AppearanceConfig.resourcePackFont) return current
        if (source !== current) {
            val provider = (current as HudFontAccessor).`sm$getProvider`()
            fallback = Font(object : Font.Provider {
                override fun glyphs(description: FontDescription) =
                    provider.glyphs(if (description == FontDescription.DEFAULT) standard else description)
                override fun effect() = provider.effect()
            })
            source = current
        }
        return fallback!!
    }
}
