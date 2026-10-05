package net.johnceo.sparklingmutuals.mixin

import net.minecraft.client.KeyMapping
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(KeyMapping::class)
interface SafariAttackKeyAccessor {
    @Accessor("clickCount") fun sparklingQueueAttack(count: Int)
}
