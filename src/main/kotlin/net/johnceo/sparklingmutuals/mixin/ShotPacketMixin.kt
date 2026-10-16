package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.shots.ShotObservationHooks
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.*
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** TAIL runs after the game's thread handoff and typed entity update. */
@Mixin(ClientPacketListener::class)
class ShotPacketMixin {
    @Inject(method=["handleAddEntity"],at=[At("TAIL")])
    private fun spawn(packet:ClientboundAddEntityPacket,info:CallbackInfo) { ShotObservationHooks.packet(packet) }
    @Inject(method=["handleMoveEntity"],at=[At("TAIL")])
    private fun move(packet:ClientboundMoveEntityPacket,info:CallbackInfo) { ShotObservationHooks.packet(packet) }
    @Inject(method=["handleEntityPositionSync"],at=[At("TAIL")])
    private fun sync(packet:ClientboundEntityPositionSyncPacket,info:CallbackInfo) { ShotObservationHooks.packet(packet) }
    @Inject(method=["handleTeleportEntity"],at=[At("TAIL")])
    private fun teleport(packet:ClientboundTeleportEntityPacket,info:CallbackInfo) { ShotObservationHooks.packet(packet) }
    @Inject(method=["handleSetEntityMotion"],at=[At("TAIL")])
    private fun motion(packet:ClientboundSetEntityMotionPacket,info:CallbackInfo) { ShotObservationHooks.packet(packet) }
    @Inject(method=["handleSetEntityData"],at=[At("TAIL")])
    private fun metadata(packet:ClientboundSetEntityDataPacket,info:CallbackInfo) { ShotObservationHooks.packet(packet) }
    @Inject(method=["handleRemoveEntities"],at=[At("TAIL")])
    private fun removed(packet:ClientboundRemoveEntitiesPacket,info:CallbackInfo) { ShotObservationHooks.packet(packet) }
}
