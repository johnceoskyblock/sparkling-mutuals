package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.shots.preview.contacts.CapsuleContactUpdates
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.*
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** TAIL runs after the game's thread handoff and typed entity update. */
@Mixin(ClientPacketListener::class)
class CapsuleContactPacketMixin {
    @Inject(method=["handleAddEntity"],at=[At("TAIL")])
    private fun spawn(packet:ClientboundAddEntityPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleMoveEntity"],at=[At("TAIL")])
    private fun move(packet:ClientboundMoveEntityPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleEntityPositionSync"],at=[At("TAIL")])
    private fun sync(packet:ClientboundEntityPositionSyncPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleTeleportEntity"],at=[At("TAIL")])
    private fun teleport(packet:ClientboundTeleportEntityPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleSetEntityMotion"],at=[At("TAIL")])
    private fun motion(packet:ClientboundSetEntityMotionPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleSetEntityData"],at=[At("TAIL")])
    private fun metadata(packet:ClientboundSetEntityDataPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleSetEquipment"],at=[At("TAIL")])
    private fun equipment(packet:ClientboundSetEquipmentPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleSetEntityPassengersPacket"],at=[At("TAIL")])
    private fun passengers(packet:ClientboundSetPassengersPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
    @Inject(method=["handleRemoveEntities"],at=[At("TAIL")])
    private fun removed(packet:ClientboundRemoveEntitiesPacket,info:CallbackInfo) { CapsuleContactUpdates.packet(packet) }
}
