package net.johnceo.sparklingmutuals.mixin

import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

/** Invoke the ordinary attack directly, without leaving synthetic key presses queued. */
@Mixin(Minecraft::class)
interface SafariAttackKeyAccessor {
    @Invoker("startAttack") fun sparklingAttack(): Boolean
}
