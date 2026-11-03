package net.johnceo.sparklingmutuals.mixin

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

/** Uniquely named read-only accessors; never generate another mod's generic getter names. */
@Mixin(AbstractContainerScreen::class)
interface ShardSaleScreenBounds {
    @Accessor("leftPos") fun sparklingMutualsShardPanelLeft(): Int
    @Accessor("imageWidth") fun sparklingMutualsShardPanelWidth(): Int
}
