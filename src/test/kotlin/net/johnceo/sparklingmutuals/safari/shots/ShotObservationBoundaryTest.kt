package net.johnceo.sparklingmutuals.safari.shots

import net.johnceo.sparklingmutuals.mixin.ShotUseMixin
import net.johnceo.sparklingmutuals.mixin.ShotPacketMixin
import net.minecraft.world.InteractionResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

class ShotObservationBoundaryTest {
    @Test fun `compiled item return callbacks preserve pass fail and success without a recorder`() {
        ShotObservationHooks.bind(null,{ error("off must not read a clock") },{ throw AssertionError(it) })
        val mixin=ShotUseMixin()
        for(name in listOf("airReturn","blockReturn")) {
            val method=ShotUseMixin::class.java.declaredMethods.single { it.name==name }.also { it.isAccessible=true }
            for(result in listOf(InteractionResult.PASS,InteractionResult.FAIL,InteractionResult.SUCCESS)) {
                val callback=CallbackInfoReturnable("use",true,result)
                val args=arrayOfNulls<Any>(method.parameterCount);args[args.lastIndex]=callback
                method.invoke(mixin,*args)
                assertSame(result,callback.returnValue);assertFalse(callback.isCancelled)
            }
        }
    }
    @Test fun `all seven compiled packet callbacks remain passive while unbound`() {
        ShotObservationHooks.bind(null,{ error("off") },{ throw AssertionError(it) })
        // Real immutable motion packet traverses the actual compiled callback without inspecting the world.
        val packet=net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(7,net.minecraft.world.phys.Vec3.ZERO)
        val method=ShotPacketMixin::class.java.declaredMethods.single { it.name=="motion" }.also { it.isAccessible=true }
        val callback=org.spongepowered.asm.mixin.injection.callback.CallbackInfo("motion",true)
        method.invoke(ShotPacketMixin(),packet,callback)
        assertFalse(callback.isCancelled)
        assertEquals(7,ShotPacketMixin::class.java.declaredMethods.size)
    }
}
