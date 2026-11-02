package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.hud.SpecialCatchAlert
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class CatchCelebrationPreviewScreen(private val parent: Screen) : Screen(Component.literal("Catch celebration preview")) {
    override fun init() {
        SpecialCatchAlert.preview()
        addRenderableWidget(ThemedButton.builder(Component.literal("Replay")) { SpecialCatchAlert.preview() }.bounds(width/2-104,height-30,100,20).build())
        addRenderableWidget(ThemedButton.builder(Component.literal("Back")) { onClose() }.bounds(width/2+4,height-30,100,20).build())
    }
    override fun extractRenderState(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        g.fill(0,0,width,height,SettingsPalette.background)
        SpecialCatchAlert.render(g)
        super.extractRenderState(g,mx,my,delta)
    }
    override fun onClose() { SpecialCatchAlert.reset();minecraft.smSetScreen(parent) }
    override fun removed() { SpecialCatchAlert.reset();super.removed() }
    override fun isPauseScreen()=false
}
