package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

/** Collision cache updates only: no input interception, recording or exports. */
object CapsuleContactUpdates {
    fun packet(packet: Any) {
        // Refresh live collision targets after vanilla applies the packet.
        val scene=net.johnceo.sparklingmutuals.safari.shots.preview.contacts.CapsuleContactScene
        when(packet) {
            is net.minecraft.network.protocol.game.ClientboundAddEntityPacket,
            is net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket -> scene.invalidateDiscovery()
            is net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket -> scene.metadataChanged(packet.id())
            is net.minecraft.network.protocol.game.ClientboundMoveEntityPacket -> scene.relativeEntityChanged(packet)
            is net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket -> scene.entityChanged(packet.id())
            is net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket -> scene.entityChanged(packet.id())
            is net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket -> scene.entityChanged(packet.id())
            is net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket -> scene.metadataChanged(packet.entity)
            is net.minecraft.network.protocol.game.ClientboundSetPassengersPacket -> scene.metadataChanged(packet.vehicle)
        }
    }
}
