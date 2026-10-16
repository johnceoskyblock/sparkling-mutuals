package net.johnceo.sparklingmutuals.safari.shots

import net.johnceo.sparklingmutuals.compat.ShotPacketCompat.vector
import net.johnceo.sparklingmutuals.compat.smCamera
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.*

/** Client-thread observer; no render callback, ESP target list, world mutation or unbounded per-frame scan. */
class ShotObservationAdapter(private val origin:ShotVector,private val cache:ShotEntityCache=ShotEntityCache(),
    private val emit:(ShotStamp,ShotObservation)->Unit) {
    private val targets=ShotTargetSampler()
    private var scan:Iterator<Entity>?=null
    private var nearbyPlayers=0
    private var scanningPlayers=0
    private var lastInput:Long?=null
    private var windowExpired=false
    private val routes=mutableMapOf<ShotInputToken,ShotUseRoute>()
    var scenario=ShotScenario.UNSPECIFIED
    var split=ShotSplit.UNASSIGNED
    var standalone=false
    fun packetBase(id:Int)=cache.packetBase(id)
    internal fun refreshTargetRange(pose:ShotPose,capsules:List<ShotVector>,position:(ShotEntityCache.Entry)->ShotVector?) {
        cache.entries().filter { it.species!=null }.forEach { entry ->
            val p=position(entry)
            if(p==null || !ShotTargetSampler.eligible(p,pose,capsules)) cache.remove(entry.id)
        }
    }
    internal fun packetObservation(entry:ShotEntityCache.Entry,update:ShotPacketUpdate,stamp:ShotStamp):ShotObservation.Packet? {
        if(entry.variant!=null) return ShotPacketAdapter.translate(entry,update,origin,stamp.micros)
        entry.base=ShotPacketAdapter.nextBase(entry.base,update)
        return null
    }
    fun reset() { targets.reset();cache.reset();scan=null;routes.clear();nearbyPlayers=0;scanningPlayers=0;lastInput=null;windowExpired=false }
    fun tick(client:Minecraft,stamp:ShotStamp) {
        val level=client.level ?: return
        if(!windowExpired && lastInput?.let { stamp.micros-it>8_000_000 }==true) {
            cache.expireWindow().forEach { e ->
                emit(stamp,ShotObservation.Capsule(e.token,e.generation,e.variant!!,ShotEntityAction.REMOVED,
                    (e.sample ?: e.base ?: origin)-origin,discovery=e.discovery,removal=ShotRemoval.WINDOW_EXPIRED,spawnMicros=e.spawnMicros))
            }
            windowExpired=true
        }
        cache.expire(stamp.micros)
        val currentPose=pose(client)
        currentPose?.let { current ->
            emit(stamp,ShotObservation.PlayerState(current))
            val capsules=cache.entries().filter { it.variant!=null }.mapNotNull { entry ->
                level.getEntity(entry.id)?.takeIf { !it.isRemoved && it.uuid.toString()==entry.identity }?.let { vector(it.position())-origin }
            }
            refreshTargetRange(current,capsules) { entry ->
                level.getEntity(entry.id)?.takeIf { !it.isRemoved && it.uuid.toString()==entry.identity }?.let { vector(it.position())-origin }
            }
        }
        if(scan==null) { scan=level.entitiesForRendering().iterator();scanningPlayers=0 }
        try {
            val iterator=scan!!
            if(!scanBatch(iterator,256-cache.entries().size,stamp) { entity ->
                if(entity is Player && entity!==client.player && client.player?.distanceToSqr(entity)?.let { it<=32.0*32 }==true) scanningPlayers++
                discover(entity,ShotDiscovery.BOOTSTRAP,stamp)
            }) { nearbyPlayers=scanningPlayers;scan=null }
        } catch(_:ConcurrentModificationException) { scan=null;cache.gap(ShotGap.CACHE_BACKLOG) }
        cache.entries().forEach { entry ->
            val e=level.getEntity(entry.id)
            if(e==null || e.isRemoved || e.uuid.toString()!=entry.identity) remove(entry.id,ShotRemoval.UNLOADED,stamp)
            else sample(client,e,entry,stamp)
        }
        currentPose?.let { targetSnapshot(client,it,stamp) }
        gaps(stamp)
    }
    internal fun <T> scanBatch(iterator:Iterator<T>,allowance:Int,stamp:ShotStamp,visit:(T)->Unit):Boolean {
        var count=0
        while(count<allowance && iterator.hasNext()) { visit(iterator.next());count++ }
        val pending=iterator.hasNext()
        if(pending) emit(stamp,ShotObservation.Gap(ShotGap.SCAN_PENDING))
        return pending
    }
    fun packet(client:Minecraft,update:ShotPacketUpdate,stamp:ShotStamp) {
        val level=client.level ?: return
        if(update is ShotPacketUpdate.Removed) { remove(update.id,ShotRemoval.SERVER_REMOVE,stamp);return }
        val entity=level.getEntity(update.id) ?: return
        val entry=packetEntry(update.id,entity.uuid.toString(),stamp) {
            if(update is ShotPacketUpdate.Spawn && entity is Display.ItemDisplay)
                cache.pendingSpawn(update.id,update.identity,update.position,stamp.micros)
            discover(entity,if(update is ShotPacketUpdate.Spawn) ShotDiscovery.SPAWN else ShotDiscovery.METADATA,stamp)
        }
        if(entry==null) { if(entity is Display.ItemDisplay) cache.remember(update,stamp.micros);gaps(stamp);return }
        val previous=entry.base
        val observation=packetObservation(entry,update,stamp)
        if(observation==null) { gaps(stamp);return }
        emit(stamp,observation)
        if(!observation.baseKnown) cache.gap(ShotGap.MISSING_BASE)
        if(observation.truncated) cache.gap(ShotGap.RAW_PATH_TRUNCATED)
        if(previous!=null && entry.base!=null && observation.points.isNotEmpty()) obstruction(client,null,entry.token,previous,entry.base!!,stamp)
        if(update is ShotPacketUpdate.MetadataChanged) display(entity,entry,stamp)
        gaps(stamp)
    }
    internal fun packetEntry(id:Int,identity:String,stamp:ShotStamp,discover:()->ShotEntityCache.Entry?):ShotEntityCache.Entry? {
        // Numeric IDs can be reused between recorder ticks. Retire before any new packet is attributed.
        cache.get(id)?.let { if(it.identity!=identity) remove(id,ShotRemoval.UNLOADED,stamp) }
        return cache.get(id) ?: discover()
    }
    private fun discover(entity:Entity,discovery:ShotDiscovery,stamp:ShotStamp):ShotEntityCache.Entry? {
        cache.get(entity.id)?.let { if(it.identity==entity.uuid.toString()) return it }
        val stack=when(entity) { is Display.ItemDisplay -> entity.itemStack;is ItemEntity -> entity.item;else -> null }
        val variant=ShotVariant.fromId(stack?.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id",""))
        val species=if(variant==null) SafariEspRules.identify(SafariEsp.describe(entity))?.name else null
        if(species!=null) {
            val client=Minecraft.getInstance();val p=vector(entity.position())
            if(client.player?.position()?.let { vector(it).distanceSquared(p)<=4096 }!=true &&
                cache.entries().none { it.variant!=null && (it.base ?: it.sample)?.distanceSquared(p)?.let { d -> d<=4096 }==true }) return null
        }
        val entry=cache.discover(entity.id,entity.uuid.toString(),variant,species,discovery,stamp.micros) ?: return null
        if(variant!=null) emit(stamp,ShotObservation.Capsule(entry.token,entry.generation,variant,ShotEntityAction.DISCOVERED,
            vector(entity.position())-origin,vector(entity.deltaMovement),discovery,entity is Display.ItemDisplay,spawnMicros=entry.spawnMicros))
        val pending=cache.takePending(entity.id)
        if(pending!=null && pending.identity==entry.identity) {
            packetObservation(entry,ShotPacketUpdate.Spawn(entity.id,entry.identity,pending.position),ShotStamp(pending.micros,stamp.tick))?.let { emit(stamp,it) }
            pending.updates.filterNot { it.second is ShotPacketUpdate.Spawn }.forEach { (time,u) -> packetObservation(entry,u,ShotStamp(time,stamp.tick))?.let { emit(stamp,it) } }
        }
        display(entity,entry,stamp)
        return entry
    }
    private fun sample(client:Minecraft,e:Entity,entry:ShotEntityCache.Entry,stamp:ShotStamp) {
        val p=vector(e.position())
        if(entry.variant!=null) emit(stamp,ShotObservation.Capsule(entry.token,entry.generation,entry.variant,ShotEntityAction.SAMPLE,p-origin,
            vector(e.deltaMovement),entry.discovery,e is Display.ItemDisplay,spawnMicros=entry.spawnMicros))
        entry.sample?.let { if(it!=p && entry.variant!=null) obstruction(client,null,entry.token,it,p,stamp) }
        entry.sample=p
        display(e,entry,stamp)
    }
    private fun remove(id:Int,reason:ShotRemoval,stamp:ShotStamp) {
        val e=cache.remove(id) ?: return
        if(e.variant!=null) emit(stamp,ShotObservation.Capsule(e.token,e.generation,e.variant,ShotEntityAction.REMOVED,
            (e.sample ?: e.base ?: origin)-origin,discovery=e.discovery,removal=reason,spawnMicros=e.spawnMicros))
    }
    private fun display(e:Entity,entry:ShotEntityCache.Entry,stamp:ShotStamp) {
        if(entry.variant==null || e !is Display) return
        val t=e.renderState()?.transformation()?.get(1f) ?: return
        val l=t.leftRotation();val r=t.rightRotation();val p=t.translation();val s=t.scale()
        val state=ShotObservation.DisplayState(entry.token,(e as? Display.ItemDisplay)?.itemTransform?.name?.lowercase() ?: "none",
            ShotVector(p.x().toDouble(),p.y().toDouble(),p.z().toDouble()),ShotVector(s.x().toDouble(),s.y().toDouble(),s.z().toDouble()),
            ShotQuaternion(l.x(),l.y(),l.z(),l.w()),ShotQuaternion(r.x(),r.y(),r.z(),r.w()),
            e.transformationInterpolationDelay,e.transformationInterpolationDuration,e.posRotInterpolationDuration)
        if(state!=entry.display) { emit(stamp,state);entry.display=state }
    }
    fun input(client:Minecraft,token:ShotInputToken,hand:ShotHand,variant:ShotVariant,route:ShotUseRoute,stamp:ShotStamp) {
        val p=client.player ?: return
        lastInput=stamp.micros;windowExpired=false;cache.newWindow()
        routes[token]=route
        val currentPose=pose(client) ?: return
        emit(stamp,ShotObservation.Input(token.value,hand,variant,route,currentPose,scenario,split,standalone))
        targetSnapshot(client,currentPose,stamp,force=true)
        val from=vector(p.eyePosition);val aim=vector(p.lookAngle)
        obstruction(client,token.value,null,from,ShotVector(from.x+aim.x*64,from.y+aim.y*64,from.z+aim.z*64),stamp)
    }
    private fun targetSnapshot(client:Minecraft,pose:ShotPose,stamp:ShotStamp,force:Boolean=false) {
        val level=client.level ?: return
        val entries=cache.entries()
        val observations=entries.mapNotNull { entry ->
            val species=entry.species ?: return@mapNotNull null
            val e=level.getEntity(entry.id)?.takeIf { !it.isRemoved && it.uuid.toString()==entry.identity } ?: return@mapNotNull null
            ShotObservation.Target(entry.token,entry.generation,species,vector(e.position())-origin,bounds(e.boundingBox),vector(e.deltaMovement))
        }
        val capsules=entries.filter { it.variant!=null }.mapNotNull { (it.sample ?: it.base)?.minus(origin) }
        targets.sample(stamp,pose,capsules,observations,force).forEach { emit(stamp,it) }
    }
    private fun pose(client:Minecraft):ShotPose? {
        val p=client.player ?: return null
        val latency=client.connection?.getPlayerInfo(p.uuid)?.latency
        val hit=client.hitResult
        return ShotPose(vector(p.position())-origin,
            vector(p.eyePosition)-origin,vector(client.gameRenderer.smCamera.position())-origin,p.yRot,p.xRot,vector(p.lookAngle),
            vector(p.deltaMovement),p.onGround(),p.isShiftKeyDown,p.isSprinting,!client.options.cameraType.isFirstPerson,
            latency,if(latency==null) ShotUnavailable.NOT_OBSERVED else null,
            hit?.let { ShotCrosshair(it.type.name.lowercase(),vector(it.location)-origin,(it as? EntityHitResult)?.entity?.id?.let { id -> cache.get(id)?.token }) },nearbyPlayers)
    }
    fun continuation(token:ShotInputToken,result:ShotUseResult,stamp:ShotStamp) {
        emit(stamp,ShotObservation.Continuation(token.value,result,routes[token] ?: ShotUseRoute.AIR))
    }
    fun route(token:ShotInputToken,route:ShotUseRoute) { routes[token]=route }
    private fun bounds(b:AABB)=ShotBounds(ShotVector(b.minX,b.minY,b.minZ)-origin,ShotVector(b.maxX,b.maxY,b.maxZ)-origin)
    private fun gaps(stamp:ShotStamp) { cache.takeGaps().forEach { emit(stamp,ShotObservation.Gap(it)) } }
    private fun obstruction(client:Minecraft,shot:String?,track:String?,from:ShotVector,to:ShotVector,stamp:ShotStamp) {
        val level=client.level ?: return
        // Every crossed chunk is checked before the game's clip traverses block shapes.
        val steps=kotlin.math.ceil(kotlin.math.sqrt(from.distanceSquared(to))*2).toInt().coerceIn(1,1024)
        if(from.distanceSquared(to)>512.0*512) {
            emit(stamp,ShotObservation.Obstruction(shot,track,false,from-origin,to-origin));return
        }
        for(i in 0..steps) {
            val f=i.toDouble()/steps
            if(!level.hasChunkAt(BlockPos.containing(from.x+(to.x-from.x)*f,from.y+(to.y-from.y)*f,from.z+(to.z-from.z)*f))) {
                emit(stamp,ShotObservation.Obstruction(shot,track,false,from-origin,to-origin));return
            }
        }
        val player=client.player ?: return
        val hit=level.clip(ClipContext(Vec3(from.x,from.y,from.z),Vec3(to.x,to.y,to.z),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player))
        if(hit.type==HitResult.Type.MISS) { emit(stamp,ShotObservation.Obstruction(shot,track,true,from-origin,to-origin));return }
        val state=level.getBlockState(hit.blockPos)
        val shapes=state.getCollisionShape(level,hit.blockPos).toAabbs()
        emit(stamp,ShotObservation.Obstruction(shot,track,true,from-origin,to-origin,vector(hit.location)-origin,hit.direction.name.lowercase(),
            BuiltInRegistries.BLOCK.getKey(state.block).toString(),shapes.take(32).map { bounds(it.move(hit.blockPos)) }))
        if(shapes.size>32) emit(stamp,ShotObservation.Gap(ShotGap.RAW_PATH_TRUNCATED))
    }
}
