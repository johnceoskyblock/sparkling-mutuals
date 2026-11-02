package net.johnceo.sparklingmutuals.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("hoveredSlot") Slot sm$getHoveredSlot();
    @Accessor("leftPos") int sm$getLeftPos();
    @Accessor("topPos") int sm$getTopPos();
    @Accessor("imageWidth") int sm$getImageWidth();
}
