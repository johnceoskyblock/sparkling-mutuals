package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.skyblock.market.ShardSaleGuidance
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Passive render hooks only. No input changes, server stacks, requests, scanning or disk work. */
@Mixin(AbstractContainerScreen::class)
abstract class ShardSaleContainerMixin {
    // Shadow the field only: Kotlin property accessors collide with other mods' public accessors.
    @Shadow @JvmField protected var hoveredSlot: Slot?=null
    @Inject(method=["extractSlot"],at=[At("RETURN")])
    private fun rangeOutline(graphics: GuiGraphicsExtractor,slot: Slot,mouseX: Int,mouseY: Int,ci: CallbackInfo) {
        ShardSaleGuidance.draw(this as AbstractContainerScreen<*>,graphics,slot)
    }
    @Inject(method=["extractTooltip"],at=[At("HEAD")])
    private fun rangeTooltipBegin(graphics: GuiGraphicsExtractor,mouseX: Int,mouseY: Int,ci: CallbackInfo) {
        net.johnceo.sparklingmutuals.skyblock.market.ShardSaleSidebar.render(this as AbstractContainerScreen<*>,graphics,mouseX,mouseY)
        ShardSaleGuidance.beginTooltip(this as AbstractContainerScreen<*>,hoveredSlot)
    }
    @Inject(method=["extractTooltip"],at=[At("RETURN")])
    private fun rangeTooltipEnd(graphics: GuiGraphicsExtractor,mouseX: Int,mouseY: Int,ci: CallbackInfo) {ShardSaleGuidance.endTooltip()}
}
