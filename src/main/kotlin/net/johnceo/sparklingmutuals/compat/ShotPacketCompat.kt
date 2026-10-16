package net.johnceo.sparklingmutuals.compat

import net.johnceo.sparklingmutuals.safari.shots.*
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.protocol.game.*
import net.minecraft.world.phys.Vec3
//? if >=26.3 {
/*import net.minecraft.world.entity.PositionPath
*///? }

object ShotPacketCompat {
    fun vector(v:Vec3)=ShotVector(v.x,v.y,v.z)
    fun resolveDelta(base:ShotVector,delta:ShotVector):ShotVector {
        val codec=VecDeltaCodec().also { it.setBase(Vec3(base.x,base.y,base.z)) }
        return vector(codec.decode(Math.round(delta.x*4096),Math.round(delta.y*4096),Math.round(delta.z*4096)))
    }
    //? if >=26.3 {
    /*private fun path(p:PositionPath):List<ShotPathPoint> {
        val steps=(p as? PositionPath.Stepped)?.steps()
        if(steps==null || steps.isEmpty()) return listOf(ShotPathPoint(vector(p.endPosition())))
        val result=steps.take(63).map { ShotPathPoint(vector(it.position()),it.tickOffset()) }.toMutableList()
        if(steps.size>63) result.add(steps.last().let { ShotPathPoint(vector(it.position()),it.tickOffset()) })
        return result.toList()
    }
    *///? }
    fun copy(packet:Any,level:ClientLevel):List<ShotPacketUpdate> = when(packet) {
        is ClientboundAddEntityPacket -> listOf(ShotPacketUpdate.Spawn(packet.id,packet.uuid.toString(),ShotVector(packet.x,packet.y,packet.z)))
        is ClientboundMoveEntityPacket -> {
            val e=packet.getEntity(level)
            if(e==null) emptyList() else {
                // Decode against our own zero base to retain raw deltas. Never touch the entity codec.
                val codec=VecDeltaCodec().also { it.setBase(Vec3.ZERO) }
                //? if >=26.3 {
                /*val raw=packet.positionDelta.decode(codec)
                val points=if(packet.hasPosition()) path(raw) else emptyList()
                val truncated=(raw as? PositionPath.Stepped)?.steps()?.size?.let { it>64 }==true
                *///? } else {
                val points=if(packet.hasPosition()) listOf(ShotPathPoint(vector(codec.decode(packet.xa.toLong(),packet.ya.toLong(),packet.za.toLong())))) else emptyList()
                val truncated=false
                //? }
                listOf(ShotPacketUpdate.Move(e.id,points,packet.yRot.takeIf { packet.hasRotation() },packet.xRot.takeIf { packet.hasRotation() },truncated))
            }
        }
        is ClientboundEntityPositionSyncPacket -> {
            //? if >=26.3 {
            /*listOf(ShotPacketUpdate.PositionSync(packet.id(),path(packet.position()),packet.yRot(),packet.xRot(),
                (packet.position() as? PositionPath.Stepped)?.steps()?.size?.let { it>64 }==true))
            *///? } else {
            listOf(ShotPacketUpdate.PositionSync(packet.id(),listOf(ShotPathPoint(vector(packet.values().position()))),packet.values().yRot(),packet.values().xRot()))
            //? }
        }
        is ClientboundTeleportEntityPacket -> listOf(ShotPacketUpdate.Teleport(packet.id(),vector(packet.change().position()),
            packet.relatives().map { it.name }.sorted(),vector(packet.change().deltaMovement()),packet.change().yRot(),packet.change().xRot()))
        is ClientboundSetEntityMotionPacket -> listOf(ShotPacketUpdate.Motion(packet.id(),vector(packet.movement())))
        is ClientboundSetEntityDataPacket -> listOf(ShotPacketUpdate.MetadataChanged(packet.id()))
        is ClientboundRemoveEntitiesPacket -> packet.entityIds.take(256).map { ShotPacketUpdate.Removed(it) }
        else -> emptyList()
    }
}
