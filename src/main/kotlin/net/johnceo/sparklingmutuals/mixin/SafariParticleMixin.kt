package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ClientPacketListener::class)
class SafariParticleMixin {
    // TAIL runs on the client thread, after the packet handler's thread check.
    // Observe the packet origin even if vanilla's distance/particle settings suppress rendering.
    @Inject(method = ["handleParticleEvent"], at = [At("TAIL")])
    private fun sparklingParticle(packet: ClientboundLevelParticlesPacket, ci: CallbackInfo) {
        SafariAssist.observeParticle(packet)
    }
}
