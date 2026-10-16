package net.johnceo.sparklingmutuals.safari.shots

/** Immutable copies of allowlisted applied packet fields. No packet, entity or NBT object crosses this port. */
sealed interface ShotPacketUpdate {
    val id:Int
    data class Spawn(override val id:Int,val identity:String,val position:ShotVector):ShotPacketUpdate
    data class Move(override val id:Int,val points:List<ShotPathPoint>,val yaw:Float?=null,val pitch:Float?=null,val truncated:Boolean=false,
        val end:ShotVector?=null,val resolved:List<ShotPathPoint>?=null,val resolvedEnd:ShotVector?=null):ShotPacketUpdate
    data class PositionSync(override val id:Int,val points:List<ShotPathPoint>,val yaw:Float?=null,val pitch:Float?=null,val truncated:Boolean=false,val end:ShotVector?=null):ShotPacketUpdate
    data class Teleport(override val id:Int,val position:ShotVector,val flags:List<String>,val motion:ShotVector?=null,val yaw:Float?=null,val pitch:Float?=null):ShotPacketUpdate
    data class Motion(override val id:Int,val motion:ShotVector):ShotPacketUpdate
    data class MetadataChanged(override val id:Int):ShotPacketUpdate
    data class Removed(override val id:Int):ShotPacketUpdate
}
object ShotPacketAdapter {
    fun translate(e:ShotEntityCache.Entry,u:ShotPacketUpdate,origin:ShotVector,observedMicros:Long):ShotObservation.Packet {
        val old=e.base
        val points=when(u) {
            is ShotPacketUpdate.Spawn -> listOf(ShotPathPoint(u.position-origin))
            is ShotPacketUpdate.Move -> u.points.toList()
            is ShotPacketUpdate.PositionSync -> u.points.map { it.copy(position=it.position-origin) }
            is ShotPacketUpdate.Teleport -> listOf(ShotPathPoint(ShotVector(
                u.position.x-if("X" in u.flags) 0.0 else origin.x,
                u.position.y-if("Y" in u.flags) 0.0 else origin.y,
                u.position.z-if("Z" in u.flags) 0.0 else origin.z)))
            else -> emptyList()
        }
        e.base=when(u) {
            is ShotPacketUpdate.Spawn -> u.position
            is ShotPacketUpdate.PositionSync -> u.end ?: u.points.lastOrNull()?.position ?: old
            is ShotPacketUpdate.Move -> u.resolvedEnd ?: old?.let { p -> (u.end ?: u.points.lastOrNull()?.position)?.let { net.johnceo.sparklingmutuals.compat.ShotPacketCompat.resolveDelta(p,it) } ?: p }
            is ShotPacketUpdate.Teleport -> if(old==null && u.flags.any { it in listOf("X","Y","Z") }) null else ShotVector(
                u.position.x+if("X" in u.flags) old!!.x else 0.0,
                u.position.y+if("Y" in u.flags) old!!.y else 0.0,
                u.position.z+if("Z" in u.flags) old!!.z else 0.0)
            else -> old
        }
        val relative=u is ShotPacketUpdate.Move || u is ShotPacketUpdate.Teleport && u.flags.isNotEmpty()
        val resolved=when(u) {
            is ShotPacketUpdate.Move -> u.resolved?.map { it.copy(position=it.position-origin) } ?:
                old?.let { p -> u.points.map { it.copy(position=net.johnceo.sparklingmutuals.compat.ShotPacketCompat.resolveDelta(p,it.position)-origin) } } ?: emptyList()
            is ShotPacketUpdate.Teleport -> e.base?.let { listOf(ShotPathPoint(it-origin)) } ?: emptyList()
            else -> points
        }
        val endpoint=when(u) { is ShotPacketUpdate.Move -> u.end ?: u.points.lastOrNull()?.position
            is ShotPacketUpdate.PositionSync -> (u.end ?: u.points.lastOrNull()?.position)?.minus(origin)
            else -> points.lastOrNull()?.position }
        val positionUpdate=u is ShotPacketUpdate.Spawn || u is ShotPacketUpdate.Move && u.points.isNotEmpty() || u is ShotPacketUpdate.PositionSync || u is ShotPacketUpdate.Teleport
        return ShotObservation.Packet(e.token,when(u) {
            is ShotPacketUpdate.Spawn -> ShotUpdateKind.SPAWN
            is ShotPacketUpdate.Move -> ShotUpdateKind.MOVE
            is ShotPacketUpdate.PositionSync -> ShotUpdateKind.POSITION_SYNC
            is ShotPacketUpdate.Teleport -> ShotUpdateKind.TELEPORT
            is ShotPacketUpdate.Motion -> ShotUpdateKind.MOTION
            else -> ShotUpdateKind.METADATA
        },if(points.size>64) points.take(63)+points.last() else points,relative,!relative || old!=null,
            when(u) { is ShotPacketUpdate.Motion -> u.motion;is ShotPacketUpdate.Teleport -> u.motion;else -> null },
            when(u) { is ShotPacketUpdate.Move -> u.yaw;is ShotPacketUpdate.PositionSync -> u.yaw;is ShotPacketUpdate.Teleport -> u.yaw;else -> null },
            when(u) { is ShotPacketUpdate.Move -> u.pitch;is ShotPacketUpdate.PositionSync -> u.pitch;is ShotPacketUpdate.Teleport -> u.pitch;else -> null },
            (u as? ShotPacketUpdate.Teleport)?.flags?.toList() ?: emptyList(),observedMicros,old?.minus(origin),
            points.size>64 || (u as? ShotPacketUpdate.Move)?.truncated==true || (u as? ShotPacketUpdate.PositionSync)?.truncated==true,
            if(resolved.size>64) resolved.take(63)+resolved.last() else resolved,endpoint,if(positionUpdate) e.base?.minus(origin) else null)
    }
}
