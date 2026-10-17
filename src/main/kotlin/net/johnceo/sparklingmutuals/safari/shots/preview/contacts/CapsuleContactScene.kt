package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.esp.*
import net.johnceo.sparklingmutuals.safari.shots.*
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.*

data class ContactTerrainDetail(val fromParameter:Double,val toParameter:Double,val position:ShotVector?,val normal:ShotVector?,
    val block:String?,val shapes:List<ShotBounds>,val boundary:ContactStop?)
data class ContactSceneSnapshot(val trace:ContactTrace,val targets:List<ContactTarget>,val coverage:ContactCoverage,
    val sourceTick:Long,val terrain:List<ContactTerrainDetail> = emptyList())

/** Client-thread only. No recording dependency, file access, world mutation or chunk loading. */
object CapsuleContactScene {
    private var priority:ContactTargetPriority?=null
    private val cache=ContactTargetCache{priority?.score(it) ?: Double.POSITIVE_INFINITY}
    private var world:Any?=null
    private var slots:Iterator<ContactDiscovery?>?=null
    private var tick=0L
    private val validation=ContactTargetValidation()
    private var espRevision=0L
    var revision=0L
        private set
    private var unsupported=false
    private var entityCount=-1
    fun invalidateDiscovery() { if(world!=null) { slots=null;cache.invalidate();validation.invalidate();revision++ } }
    fun metadataChanged(entityId:Int) { if(world!=null) { cache.metadataChanged();entityChanged(entityId) } }
    fun entityChanged(entityId:Int) { if(world!=null) { validation.invalidate(entityId.toString());revision++ } }
    fun relativeEntityChanged(packet:net.minecraft.network.protocol.game.ClientboundMoveEntityPacket) {
        // O(1) ID routing only; full descriptor/body inspections retain their separate sixteen-check allowance.
        val level=world as? net.minecraft.client.multiplayer.ClientLevel ?: return
        packet.getEntity(level)?.let{entityChanged(it.id)}
    }
    private val collectionRules=net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreviewRules(allowExtendedPreview=false)
    fun probeEligible(client:Minecraft)=collectionRules.evaluate(net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreview.collectionState(client))==net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreviewAvailability.AVAILABLE
    fun reset() { cache.reset();world=null;slots=null;validation.reset();entityCount=-1;unsupported=false;priority=null;collectionRules.reset();revision++ }
    fun tick(client:Minecraft,enabled:Boolean) {
        val level=client.level
        if(!enabled || !SafariAssist.inSafari || level==null || client.player==null) { reset();return }
        if(world!==level) { reset();world=level }
        tick++;validation.beginTick();espRevision=SafariEsp.contactRevision;revision++
        val player=client.player!!
        priority=ContactTargetPriority(ContactCurveSampler.sample(vector(player.eyePosition),vector(player.lookAngle),player.xRot.toDouble()))
        if(entityCount!=level.entityCount) { invalidateDiscovery();entityCount=level.entityCount }
        collectionRules.tick(level,net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreview.collectionState(client))
        if(slots?.hasNext()!=true) {
            unsupported=false
            val entities=level.entitiesForRendering().iterator()
            slots=object:Iterator<ContactDiscovery?> {
                override fun hasNext()=entities.hasNext()
                override fun next():ContactDiscovery? {
                    val entity:net.minecraft.world.entity.Entity?=entities.next()
                    return entity?.let{ContactDiscovery(it.id.toString(),System.identityHashCode(it))}
                }
            }
        }
        cache.tick(level,slots!!){inspect(client,it)}
        if(cache.scanInterrupted) slots=null
    }
    private fun inspect(client:Minecraft,d:ContactDiscovery):ContactTarget? {
        val entity=client.level?.getEntity(d.key.toIntOrNull() ?: return null) ?: return null
        if(System.identityHashCode(entity)!=d.generation || entity.isRemoved || entity is Player ||
            client.player?.distanceToSqr(entity)?.let{it>4096}!=false) return null
        val descriptor=SafariEsp.describe(entity)
        val species=SafariEspRules.identify(descriptor) ?: return null
        if(!SafariEsp.contactCurrent(entity,species.name,client) || SafariEsp.isRockmiteMound(entity,client)) return null
        val b=entity.boundingBox
        val body=ContactTargetProxies.body(if(entity is Display) "display" else if(entity is ArmorStand) "armor_stand" else "native",
            ShotVector(entity.x,entity.y,entity.z),ShotBounds(ShotVector(b.minX,b.minY,b.minZ),ShotVector(b.maxX,b.maxY,b.maxZ)),true,
            species.identifiers.any{it.shulker!=null})
        if(body==null) { unsupported=true;return null }
        return ContactTarget(d.key,d.generation,species.name,body.first,body.second)
    }
    fun refreshRevision(client:Minecraft) {
        if(world==null || world!==client.level) return
        if(client.level?.entityCount!=entityCount) { invalidateDiscovery();entityCount=client.level?.entityCount ?: -1 }
        if(espRevision!=SafariEsp.contactRevision) { validation.invalidate();espRevision=SafariEsp.contactRevision;revision++ }
    }
    fun snapshot(client:Minecraft,eye:ShotVector,aim:ShotVector,pitch:Double):ContactSceneSnapshot {
        refreshRevision(client)
        val (targets,cachedCoverage)=cache.snapshot()
        var coverage=if(unsupported) ContactCoverage(false,cachedCoverage.reasons+"unsupported_proxy") else cachedCoverage
        val curve=ContactCurveSampler.sample(eye,aim,pitch)
        if(client.level!==world || client.player==null) return ContactSceneSnapshot(ContactTrace(emptyList(),null,ContactStop.UNLOADED,emptySet()),emptyList(),ContactCoverage(false,setOf("scan_pending")),tick)
        val details=ArrayList<ContactTerrainDetail>()
        var trace=CapsuleContactSolver.trace(curve,targets,coverage){a,b->segment(client,a,b,details)}
        val primary=trace.primary
        if(primary?.kind==ContactKind.CRITTER) {
            val target=targets.firstOrNull{it.key==primary.targetKey}
            val error=if(target==null) "stale_target" else validation.validate(target){inspect(client,ContactDiscovery(it.key,it.generation))}
            // Current-position bounds changed since tick: don't submit an endpoint for the old body.
            if(error!=null) {
                coverage=ContactCoverage(false,coverage.reasons+error)
                trace=trace.copy(primary=null,stop=ContactStop.TARGET_COVERAGE,warnings=trace.warnings+coverage.reasons)
            }
        }
        return ContactSceneSnapshot(trace,targets.toList(),coverage,tick,details.toList())
    }
    private fun vector(v:Vec3)=ShotVector(v.x,v.y,v.z)
    private fun segment(client:Minecraft,a:ContactNode,b:ContactNode,details:MutableList<ContactTerrainDetail>):ContactSegmentScene {
        val level=client.level ?: return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.UNLOADED))
        val cells=ContactSceneBoundary.collisionCells(a.position,b.position)
        // ClientLevel.hasChunkAt always returns true in 26.1.2. Explicit false never creates chunks.
        if(cells.isEmpty() || cells.any{level.chunkSource.getChunk(it.first shr 4,it.third shr 4,ChunkStatus.FULL,false)==null}) {
            details.add(ContactTerrainDetail(a.parameter,b.parameter,null,null,null,emptyList(),ContactStop.UNLOADED))
            return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.UNLOADED))
        }
        var best:ContactCandidate?=null;var first=Double.POSITIVE_INFINITY;var fluidFraction=Double.POSITIVE_INFINITY
        var bestBlock:String?=null;var bestBoxes=emptyList<ShotBounds>()
        fun boxes(shapes:List<AABB>,pos:BlockPos)=shapes.map{it.move(pos)}.map{ShotBounds(ShotVector(it.minX,it.minY,it.minZ),ShotVector(it.maxX,it.maxY,it.maxZ))}
        for((x,y,z) in cells) {
            val pos=BlockPos(x,y,z);val state=level.getBlockState(pos)
            val shapes=state.getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(client.player!!)).toAabbs()
            val fluid=state.fluidState
            val fluidShapes=if(fluid.isEmpty) emptyList() else fluid.getShape(level,pos).toAabbs()
            if(shapes.size>32 || fluidShapes.size>32) return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.WORK_LIMIT))
            val bodies=boxes(shapes,pos)
            for(body in bodies) {
                val f=ContactIntersections.entry(a.position,b.position,body) ?: continue
                if(f>=first) continue
                first=f;val p=a.position.lerp(b.position,f)
                val faces=listOf(kotlin.math.abs(p.x-body.min.x) to ShotVector(-1.0,0.0,0.0),kotlin.math.abs(p.x-body.max.x) to ShotVector(1.0,0.0,0.0),
                    kotlin.math.abs(p.y-body.min.y) to ShotVector(0.0,-1.0,0.0),kotlin.math.abs(p.y-body.max.y) to ShotVector(0.0,1.0,0.0),
                    kotlin.math.abs(p.z-body.min.z) to ShotVector(0.0,0.0,-1.0),kotlin.math.abs(p.z-body.max.z) to ShotVector(0.0,0.0,1.0))
                best=ContactCandidate(ContactKind.TERRAIN,a.parameter+(b.parameter-a.parameter)*f,p,faces.minBy{it.first}.second,null,null)
                bestBlock=BuiltInRegistries.BLOCK.getKey(state.block).toString();bestBoxes=bodies
            }
            for(body in boxes(fluidShapes,pos)) ContactIntersections.entry(a.position,b.position,body)?.let{fluidFraction=minOf(fluidFraction,it)}
        }
        val boundary=if(fluidFraction.isFinite())ContactBoundary(fluidFraction,ContactStop.FLUID) else null
        if(best!=null || boundary!=null) details.add(ContactTerrainDetail(a.parameter,b.parameter,best?.position ?: a.position.lerp(b.position,fluidFraction),best?.normal,bestBlock,bestBoxes,boundary?.reason))
        return ContactSegmentScene(best,boundary)
    }
}
