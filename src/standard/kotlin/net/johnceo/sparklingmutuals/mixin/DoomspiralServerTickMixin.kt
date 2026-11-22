package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl
import net.minecraft.network.protocol.common.ClientboundPingPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Passive observation: vanilla handles thread handoff and sends its own pong unchanged. */
@Mixin(ClientCommonPacketListenerImpl::class)
class DoomspiralServerTickMixin {
    @Inject(method = ["handlePing"], at = [At("TAIL")])
    private fun doomspiralServerTick(packet: ClientboundPingPacket, ci: CallbackInfo) {
        DoomspiralThrowTimer.serverTick(this, packet.id)
    }
}
