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
import net.minecraft.world.phys.*

data class ContactTerrainDetail(val fromParameter:Double,val toParameter:Double,val position:ShotVector?,val normal:ShotVector?,
    val block:String?,val shapes:List<ShotBounds>,val boundary:ContactStop?)
data class ContactSceneSnapshot(val trace:ContactTrace,val targets:List<ContactTarget>,val coverage:ContactCoverage,
    val sourceTick:Long,val terrain:List<ContactTerrainDetail> = emptyList())

/** Client-thread only. No recording dependency, file access, world mutation or chunk loading. */
object CapsuleContactScene {
    private val cache=ContactTargetCache()
    private var world:Any?=null
    private var slots:Iterator<ContactDiscovery?>?=null
    private var tick=0L
    private var identityAllowance=0
    private var unsupported=false
    private val collectionRules=net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreviewRules()
    fun probeEligible(client:Minecraft)=collectionRules.evaluate(net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreview.collectionState(client))==net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreviewAvailability.AVAILABLE
    fun reset() { cache.reset();world=null;slots=null;identityAllowance=0;unsupported=false;collectionRules.reset() }
    fun tick(client:Minecraft,enabled:Boolean) {
        val level=client.level
        if(!enabled || !SafariAssist.inSafari || level==null || client.player==null) { reset();return }
        if(world!==level) { reset();world=level }
        tick++;identityAllowance=16
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
    }
    private fun inspect(client:Minecraft,d:ContactDiscovery):ContactTarget? {
        val entity=client.level?.getEntity(d.key.toIntOrNull() ?: return null) ?: return null
        if(System.identityHashCode(entity)!=d.generation || entity.isRemoved || entity is Player ||
            client.player?.distanceToSqr(entity)?.let{it>4096}!=false) return null
        val descriptor=SafariEsp.describe(entity)
        val species=SafariEspRules.identify(descriptor) ?: return null
        if(!SafariEsp.contactCurrent(entity,species.name,client)) return null
        val b=entity.boundingBox
        val body=ContactTargetProxies.body(if(entity is Display) "display" else if(entity is ArmorStand) "armor_stand" else "native",
            ShotVector(entity.x,entity.y,entity.z),ShotBounds(ShotVector(b.minX,b.minY,b.minZ),ShotVector(b.maxX,b.maxY,b.maxZ)),true,
            species.identifiers.any{it.shulker!=null})
        if(body==null) { unsupported=true;return null }
        return ContactTarget(d.key,d.generation,species.name,body.first,body.second)
    }
    fun snapshot(client:Minecraft,eye:ShotVector,aim:ShotVector,pitch:Double):ContactSceneSnapshot {
        val (targets,cachedCoverage)=cache.snapshot()
        var coverage=if(unsupported) ContactCoverage(false,cachedCoverage.reasons+"unsupported_proxy") else cachedCoverage
        val curve=ContactCurveSampler.sample(eye,aim,pitch)
        if(client.level!==world || client.player==null) return ContactSceneSnapshot(ContactTrace(emptyList(),null,ContactStop.UNLOADED,emptySet()),emptyList(),ContactCoverage(false,setOf("scan_pending")),tick)
        val details=ArrayList<ContactTerrainDetail>()
        var trace=CapsuleContactSolver.trace(curve,targets,coverage){a,b->segment(client,a,b,details)}
        val primary=trace.primary
        if(primary?.kind==ContactKind.CRITTER) {
            val target=targets.firstOrNull{it.key==primary.targetKey}
            val fresh=if(identityAllowance>0 && target!=null) { identityAllowance--;inspect(client,ContactDiscovery(target.key,target.generation)) } else null
            // Current-position bounds changed since tick: don't submit an endpoint for the old body.
            if(fresh==null || fresh!=target) {
                coverage=ContactCoverage(false,setOf(if(identityAllowance==0) "identity_budget" else "stale_target"))
                trace=trace.copy(primary=null,stop=ContactStop.TARGET_COVERAGE,warnings=trace.warnings+coverage.reasons)
            }
        }
        return ContactSceneSnapshot(trace,targets.toList(),coverage,tick,details.toList())
    }
    private fun vector(v:Vec3)=ShotVector(v.x,v.y,v.z)
    private fun segment(client:Minecraft,a:ContactNode,b:ContactNode,details:MutableList<ContactTerrainDetail>):ContactSegmentScene {
        val level=client.level ?: return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.UNLOADED))
        val cells=ContactSceneBoundary.cells(a.position,b.position)
        // Preflight all crossed cells before Minecraft's shape clip is invoked.
        if(cells.isEmpty() || cells.any{!level.hasChunkAt(BlockPos(it.first,it.second,it.third))}) {
            details.add(ContactTerrainDetail(a.parameter,b.parameter,null,null,null,emptyList(),ContactStop.UNLOADED))
            return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.UNLOADED))
        }
        val from=Vec3(a.position.x,a.position.y,a.position.z);val to=Vec3(b.position.x,b.position.y,b.position.z)
        val hit=level.clip(ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,client.player!!))
        val fluid=level.clip(ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,client.player!!))
        val length=a.position.distanceSquared(b.position)
        fun fraction(p:Vec3)=if(length==0.0) 0.0 else ((p.x-from.x)*(to.x-from.x)+(p.y-from.y)*(to.y-from.y)+(p.z-from.z)*(to.z-from.z))/length
        val fluidHit=fluid.type!=HitResult.Type.MISS && !level.getFluidState(fluid.blockPos).isEmpty
        val boundary=if(fluidHit) ContactBoundary(fraction(fluid.location).coerceIn(0.0,1.0),ContactStop.FLUID) else null
        if(hit.type==HitResult.Type.MISS) {
            if(boundary!=null) details.add(ContactTerrainDetail(a.parameter,b.parameter,vector(fluid.location),null,null,emptyList(),ContactStop.FLUID))
            return ContactSegmentScene(null,boundary)
        }
        val state=level.getBlockState(hit.blockPos)
        val shapes=state.getCollisionShape(level,hit.blockPos).toAabbs()
        if(shapes.size>32) return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.WORK_LIMIT))
        val normal=ShotVector(hit.direction.stepX.toDouble(),hit.direction.stepY.toDouble(),hit.direction.stepZ.toDouble())
        val boxes=shapes.map{it.move(hit.blockPos)}.map{ShotBounds(ShotVector(it.minX,it.minY,it.minZ),ShotVector(it.maxX,it.maxY,it.maxZ))}
        val f=if(hit.isInside) 0.0 else fraction(hit.location).coerceIn(0.0,1.0)
        val position=a.position.lerp(b.position,f)
        details.add(ContactTerrainDetail(a.parameter,b.parameter,position,normal,BuiltInRegistries.BLOCK.getKey(state.block).toString(),boxes,boundary?.reason))
        return ContactSegmentScene(ContactCandidate(ContactKind.TERRAIN,a.parameter+(b.parameter-a.parameter)*f,position,normal,null,null),boundary)
    }
}
