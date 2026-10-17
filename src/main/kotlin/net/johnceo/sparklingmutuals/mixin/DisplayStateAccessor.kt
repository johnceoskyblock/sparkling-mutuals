package net.johnceo.sparklingmutuals.mixin

import net.minecraft.world.entity.Display
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

/** Never retain a mound surface while a metadata change awaits its client render-state update. */
@Mixin(Display::class)
interface DisplayStateAccessor {
    @Accessor("updateRenderState") fun sparklingRenderStatePending(): Boolean
}
