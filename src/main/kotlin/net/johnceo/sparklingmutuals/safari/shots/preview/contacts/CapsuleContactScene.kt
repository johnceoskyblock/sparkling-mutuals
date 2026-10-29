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
    val sourceTick:Long,val terrain:List<ContactTerrainDetail> = emptyList(),val waterEstimate:Boolean=false)

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
    private var unsupportedSurface=false
    private var entityCount=-1
    private var wormKeys=emptySet<String>()
    private var wormAssociationDirty=false
    private var partSupport=emptyMap<String,List<ContactTarget>>()
    private var bodyDiscoveryPending=true
    private var bodyEpoch=0L
    private var bodySweepEpoch=-1L
    private val wormWitnesses=linkedMapOf<String,ContactTarget>()
    private var wormWitnessOverflow=false
    private val metadataChecks=ContactMetadataQueue()
    private var metadataOverflow=false
    private var discoveryRestarts=0L
    private var metadataQueued=0L
    private var metadataHandled=0L
    private var completedSweeps=0L
    /** Cached counts only, read by the explicit local debug command without extra inspections. */
    fun debugSummary():Map<String,Any> {
        val (targets,coverage)=cache.snapshot()
        return mapOf("sourceTick" to tick,"cachedTargets" to targets.size,
            "bodyCandidates" to targets.count{it.bodyPart},"supportedBodies" to partSupport.size,
            "bodyDiscoveryPending" to bodyDiscoveryPending,"associationDirty" to wormAssociationDirty,
            "sweepFinished" to cache.sweepFinished,"coverageReasons" to coverage.reasons,
            "witnessParts" to wormWitnesses.size,"witnessHeads" to wormWitnesses.values.count{!it.bodyPart},
            "witnessOverflow" to wormWitnessOverflow,"pendingMetadata" to metadataChecks.size,
            "metadataOverflow" to metadataOverflow,"discoveryRestarts" to discoveryRestarts,
            "metadataQueued" to metadataQueued,"metadataHandled" to metadataHandled,
            "completedSweeps" to completedSweeps,"bodyEpoch" to bodyEpoch,"sweepEpoch" to bodySweepEpoch)
    }
    fun invalidateDiscovery() { if(world!=null) { slots=null;cache.invalidate();validation.invalidate();bodyDiscoveryPending=true;bodyEpoch++;discoveryRestarts++;revision++ } }
    fun metadataChanged(entityId:Int) { if(world!=null) {
        // TAIL packet hooks see the updated equipment. Empty-head labels cannot currently be
        // Shyworm heads/body items; later equipment events will enqueue a newly equipped stand.
        val entity=(world as? net.minecraft.client.multiplayer.ClientLevel)?.getEntity(entityId)
        if(entityId.toString() !in wormKeys && entity is ArmorStand && !entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty) {
            metadataQueued++
            if(!metadataChecks.add(ContactDiscovery(entityId.toString(),System.identityHashCode(entity)))) {
                // A dropped identity may already lie behind the iterator. Require a new whole sweep.
                metadataOverflow=true;invalidateDiscovery()
            }
        }
        cache.metadataChanged();entityChanged(entityId)
    } }
    fun entityChanged(entityId:Int) { if(world!=null) {
        val key=entityId.toString()
        if(key in wormKeys) wormAssociationDirty=true
        validation.invalidate(key);revision++
    } }
    fun relativeEntityChanged(packet:net.minecraft.network.protocol.game.ClientboundMoveEntityPacket) {
        // O(1) ID routing only; full descriptor/body inspections retain their separate sixteen-check allowance.
        val level=world as? net.minecraft.client.multiplayer.ClientLevel ?: return
        packet.getEntity(level)?.let{entityChanged(it.id)}
    }
    fun reset() { cache.reset();world=null;slots=null;validation.reset();entityCount=-1;unsupported=false;unsupportedSurface=false;wormKeys=emptySet();wormAssociationDirty=false;bodyDiscoveryPending=true;bodyEpoch=0;bodySweepEpoch=-1;wormWitnesses.clear();wormWitnessOverflow=false;metadataChecks.reset();metadataOverflow=false;discoveryRestarts=0;metadataQueued=0;metadataHandled=0;completedSweeps=0;partSupport=emptyMap();priority=null;revision++ }
    fun tick(client:Minecraft,enabled:Boolean) {
        val level=client.level
        if(!enabled || !SafariAssist.inSafari || level==null || client.player==null) { reset();return }
        if(world!==level) { reset();world=level }
        tick++;validation.beginTick();espRevision=SafariEsp.contactRevision;revision++
        val player=client.player!!
        priority=ContactTargetPriority(ContactCurveSampler.sample(vector(player.eyePosition),vector(player.lookAngle),player.xRot.toDouble()))
        if(entityCount!=level.entityCount) { invalidateDiscovery();entityCount=level.entityCount }
        // Preserve the whole association graph independently of solver retention, including
        // connectors displaced by unrelated targets. At most 64 extra inspections per tick.
        for((key,previous) in wormWitnesses.toMap()) {
            val current=inspect(client,ContactDiscovery(key,previous.generation))
            if(current==null || current.species!="Shyworm" || current.surface!=null) wormWitnesses.remove(key)
            else wormWitnesses[key]=current
        }
        if(slots?.hasNext()!=true) {
            unsupported=false;unsupportedSurface=false
            bodySweepEpoch=bodyEpoch;wormWitnessOverflow=false
            val entities=level.entitiesForRendering().iterator()
            slots=object:Iterator<ContactDiscovery?> {
                override fun hasNext()=entities.hasNext()
                override fun next():ContactDiscovery? {
                    val entity:net.minecraft.world.entity.Entity?=entities.next()
                    return entity?.let{ContactDiscovery(it.id.toString(),System.identityHashCode(it))}
                }
            }
        }
        // Reset sweep overflow above before dirty inspections: a newly revealed 65th node
        // must remain uncertain, even when metadata arrives at a sweep boundary.
        for(d in metadataChecks.drain(16)) { inspect(client,d);metadataHandled++ }
        // 160 cache + 64 association + 16 metadata + 16 selected validations = 256.
        cache.tick(level,slots!!,160){inspect(client,it)}
        val associationTargets=wormWitnesses.values.toList()
        wormKeys=associationTargets.filter{it.species=="Shyworm"}.mapTo(hashSetOf()){it.key}
        if(wormWitnessOverflow || associationTargets.count{!it.bodyPart}>16) bodyDiscoveryPending=true
        else if(cache.sweepFinished && bodySweepEpoch==bodyEpoch) { bodyDiscoveryPending=false;metadataOverflow=false }
        if(cache.sweepFinished) completedSweeps++
        partSupport=if(bodyDiscoveryPending || metadataChecks.size>0) emptyMap() else ShywormContactParts.support(associationTargets)
        wormAssociationDirty=false
        if(cache.scanInterrupted) slots=null
    }
    private fun inspect(client:Minecraft,d:ContactDiscovery):ContactTarget? {
        val entity=client.level?.getEntity(d.key.toIntOrNull() ?: return null) ?: return null
        if(System.identityHashCode(entity)!=d.generation || entity.isRemoved || entity is Player ||
            client.player?.distanceToSqr(entity)?.let{it>4096}!=false) return null
        val descriptor=SafariEsp.describe(entity)
        if(SafariEsp.isRockmiteMound(entity,client)) {
            val surface=(entity as? Display.ItemDisplay)?.let{MoundContactSurface.inspect(client,it)}
            if(surface==null) { unsupportedSurface=true;return null }
            return ContactTarget(d.key,d.generation,"Rockmite",surface.bounds(),"mound_surface_v1",surface=surface)
        }
        val part=entity is ArmorStand && descriptor.texture==ShywormContactParts.BODY_TEXTURE
        val species=if(part) "Shyworm" else SafariEspRules.identify(descriptor)?.name ?: return null
        if(part) {
            // This observed geometry is only supported for ordinary, unposed marker head-items.
            val stand=entity
            if(!stand.isMarker || stand.isSmall || listOf(stand.headPose,stand.bodyPose).any{it.x()!=0f || it.y()!=0f || it.z()!=0f}) {unsupported=true;return null}
        } else if(!SafariEsp.contactCurrent(entity,species,client)) return null
        val b=entity.boundingBox
        val body=ContactTargetProxies.body(if(entity is Display) "display" else if(entity is ArmorStand) "armor_stand" else "native",
            ShotVector(entity.x,entity.y,entity.z),ShotBounds(ShotVector(b.minX,b.minY,b.minZ),ShotVector(b.maxX,b.maxY,b.maxZ)),true,
            !part && SafariEspRules.identify(descriptor)?.identifiers?.any{it.shulker!=null}==true)
        if(body==null) { unsupported=true;return null }
        val target=ContactTarget(d.key,d.generation,species,body.first,body.second,bodyPart=part)
        if(species=="Shyworm") {
            if(d.key in wormWitnesses || wormWitnesses.size<64) wormWitnesses[d.key]=target
            else wormWitnessOverflow=true
        }
        return target
    }
    fun refreshRevision(client:Minecraft) {
        if(world==null || world!==client.level) return
        if(client.level?.entityCount!=entityCount) { invalidateDiscovery();entityCount=client.level?.entityCount ?: -1 }
        if(espRevision!=SafariEsp.contactRevision) { validation.invalidate();espRevision=SafariEsp.contactRevision;revision++ }
    }
    fun snapshot(client:Minecraft,eye:ShotVector,aim:ShotVector,pitch:Double):ContactSceneSnapshot {
        refreshRevision(client)
        val (discovered,cachedCoverage)=cache.snapshot()
        // A changed off-path part/head can make an otherwise unchanged path ambiguous.
        // Rebuild after the next bounded tick refresh rather than doing extra render-thread inspections.
        val support=if(wormAssociationDirty || bodyDiscoveryPending || metadataChecks.size>0) emptyMap() else partSupport
        val targets=discovered.filter{!it.bodyPart || it.key in support}
        val omittedBody=discovered.any{it.bodyPart && it.key !in support}
        var coverage=if(unsupported || omittedBody || unsupportedSurface) ContactCoverage(false,cachedCoverage.reasons+"unsupported_proxy") else cachedCoverage
        val curve=ContactCurveSampler.sample(eye,aim,pitch)
        if(client.level!==world || client.player==null) return ContactSceneSnapshot(ContactTrace(emptyList(),null,ContactStop.UNLOADED,emptySet()),emptyList(),ContactCoverage(false,setOf("scan_pending")),tick)
        val details=ArrayList<ContactTerrainDetail>()
        var firstWaterParameter=Double.POSITIVE_INFINITY
        var trace=CapsuleContactSolver.trace(curve,targets,coverage){a,b->segment(client,a,b,details,true){firstWaterParameter=minOf(firstWaterParameter,it)}}
        val primary=trace.primary
        if(primary?.targetKey!=null) {
            val target=targets.firstOrNull{it.key==primary.targetKey}
            val dependencies=target?.let{support[it.key] ?: listOf(it)}
            val error=if(dependencies==null) "stale_target" else dependencies.firstNotNullOfOrNull{t->validation.validate(t){inspect(client,ContactDiscovery(it.key,it.generation))}}
            // Current-position bounds changed since tick: don't submit an endpoint for the old body.
            if(error!=null) {
                coverage=ContactCoverage(false,coverage.reasons+error)
                trace=trace.copy(primary=null,stop=ContactStop.TARGET_COVERAGE,warnings=trace.warnings+coverage.reasons)
            }
        }
        if(trace.primary?.kind==ContactKind.TERRAIN) {
            val patch=surfacePatch(client,trace.primary.position,targets)
            trace=trace.copy(surfacePatch=patch ?: emptyList(),warnings=if(patch==null)trace.warnings+"shape_limit" else trace.warnings)
        }
        val waterEstimate=firstWaterParameter.isFinite() && firstWaterParameter<=(trace.nodes.lastOrNull()?.parameter ?: Double.NEGATIVE_INFINITY)+CapsuleContactSolver.TIE
        return ContactSceneSnapshot(trace,targets.toList(),coverage,tick,details.toList(),waterEstimate)
    }
    private fun surfacePatch(client:Minecraft,center:ShotVector,targets:List<ContactTarget>):List<ContactSurfaceBox>? {
        val level=client.level ?: return null
        val cells=ContactSceneBoundary.collisionCells(center,center)
        if(cells.any{level.chunkSource.getChunk(it.first shr 4,it.third shr 4,ChunkStatus.FULL,false)==null}) return null
        val patch=ArrayList<ContactSurfaceBox>()
        for((x,y,z) in cells) {
            val pos=BlockPos(x,y,z)
            val shapes=level.getBlockState(pos).getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(client.player!!)).toAabbs()
            if(shapes.size>32) return null
            for(b in shapes) {
                val moved=b.move(pos)
                val surface=ContactSurfaceBox.axis(ShotBounds(ShotVector(moved.minX,moved.minY,moved.minZ),ShotVector(moved.maxX,moved.maxY,moved.maxZ)))
                if(surface.facesNear(center,.65).isEmpty()) continue
                if(patch.size==32) return null
                patch.add(surface)
            }
        }
        for(t in targets) {
            val s=t.surface ?: continue
            if(s.facesNear(center,.65).isEmpty()) continue
            if(validation.validate(t){inspect(client,ContactDiscovery(it.key,it.generation))}!=null) return null
            if(patch.size==32) return null
            patch.add(s)
        }
        return patch.toList()
    }
    private fun vector(v:Vec3)=ShotVector(v.x,v.y,v.z)
    private fun segment(client:Minecraft,a:ContactNode,b:ContactNode,details:MutableList<ContactTerrainDetail>,waterPreview:Boolean,onWater:(Double)->Unit):ContactSegmentScene {
        val level=client.level ?: return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.UNLOADED))
        val cells=ContactSceneBoundary.collisionCells(a.position,b.position)
        // ClientLevel.hasChunkAt always returns true in 26.1.2. Explicit false never creates chunks.
        if(cells.isEmpty() || cells.any{level.chunkSource.getChunk(it.first shr 4,it.third shr 4,ChunkStatus.FULL,false)==null}) {
            details.add(ContactTerrainDetail(a.parameter,b.parameter,null,null,null,emptyList(),ContactStop.UNLOADED))
            return ContactSegmentScene(null,ContactBoundary(0.0,ContactStop.UNLOADED))
        }
        var best:ContactCandidate?=null;var first=Double.POSITIVE_INFINITY;var fluidFraction=Double.POSITIVE_INFINITY;var waterFraction=Double.POSITIVE_INFINITY
        var bestBlock:String?=null;var bestBoxes=emptyList<ShotBounds>()
        fun boxes(shapes:List<AABB>,pos:BlockPos)=shapes.map{it.move(pos)}.map{ShotBounds(ShotVector(it.minX,it.minY,it.minZ),ShotVector(it.maxX,it.maxY,it.maxZ))}
        for((x,y,z) in cells) {
            val pos=BlockPos(x,y,z);val state=level.getBlockState(pos)
            val shapes=state.getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(client.player!!)).toAabbs()
            val fluid=state.fluidState
            // Preview uses the same uncalibrated spatial estimate through water. Strict recorder
            // probes keep their original fluid boundary; solid waterlogged geometry still applies.
            val water=waterPreview && fluid.`is`(net.minecraft.tags.FluidTags.WATER)
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
            for(body in boxes(fluidShapes,pos)) ContactIntersections.entry(a.position,b.position,body)?.let{
                if(water) waterFraction=minOf(waterFraction,it) else fluidFraction=minOf(fluidFraction,it)
            }
        }
        if(waterFraction.isFinite() && waterFraction<=minOf(first,fluidFraction)) onWater(a.parameter+(b.parameter-a.parameter)*waterFraction)
        val boundary=if(fluidFraction.isFinite())ContactBoundary(fluidFraction,ContactStop.FLUID) else null
        if(best!=null || boundary!=null) details.add(ContactTerrainDetail(a.parameter,b.parameter,best?.position ?: a.position.lerp(b.position,fluidFraction),best?.normal,bestBlock,bestBoxes,boundary?.reason))
        return ContactSegmentScene(best,boundary)
    }
}
