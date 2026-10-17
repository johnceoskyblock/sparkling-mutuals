package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.shots.ShotVariant
import net.johnceo.sparklingmutuals.safari.shots.preview.contacts.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.component.DataComponents

/** Passive view of the local player; never starts recording or changes a throw. */
object CapsulePreview {
    private val rules=CapsulePreviewRules()
    private var world: Any?=null
    private var registered=false
    private val frames=ContactGuideLifecycle()
    fun beginFrame() { frames.reset();CapsulePreviewRenderer.clearPending() }
    fun register() {
        if(registered) return
        registered=true
        WorldRenderCompat.register(CapsulePreviewRenderer::render)
    }
    fun tick(client: Minecraft) {
        world=client.level
        rules.tick(world,currentState(client))
        CapsuleContactScene.tick(client,ConfigManager.experimentalCapsulePath && ConfigManager.allowUncalibratedCapsuleEstimates ||
            net.johnceo.sparklingmutuals.safari.shots.ShotRecorder.status().recording)
    }
    fun reset() { CapsulePreviewRenderer.clearPending();world=null;rules.reset();frames.reset();CapsuleContactScene.reset() }
    internal fun currentState(client: Minecraft): CapsulePreviewState? {
        if(!ConfigManager.experimentalCapsulePath || client.level==null) return null
        return collectionState(client)?.copy(enabled=ConfigManager.experimentalCapsulePath,allowUncalibratedEstimates=ConfigManager.allowUncalibratedCapsuleEstimates)
    }
    internal fun collectionState(client:Minecraft):CapsulePreviewState? {
        if(client.level==null) return null
        val p=client.player ?: return null
        val itemId=p.mainHandItem.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","")
        return CapsulePreviewState(true, SafariAssist.inSafari,
            ShotVariant.fromId(itemId),client.options.cameraType.isFirstPerson,p.onGround(),
            p.isShiftKeyDown,p.isSprinting,p.isSwimming,p.abilities.flying,p.isPassenger,
            ConfigManager.hideFlyingCapsules,ConfigManager.hideGroundCapsules,client.smScreen!=null,
            client.options.smHideGui,p.xRot.toDouble(),ShotPacketCompat.vector(p.position()),ShotPacketCompat.vector(p.deltaMovement),
            true,p.isInWater,p.isInLava)
    }
    internal fun frame(client:Minecraft):CapsulePreviewFrame? {
        val state=currentState(client);val available=availability(state)
        val p=client.player ?: return null
        if(available!=CapsulePreviewAvailability.AVAILABLE) { frames.reset();return CapsulePreviewFrameBuilder.build(available,ShotPacketCompat.vector(p.eyePosition),ShotPacketCompat.vector(p.lookAngle)) }
        val partial=client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val eye=ShotPacketCompat.vector(p.getEyePosition(partial))
        // Local-player aim uses current rotations; view APIs may substitute a detached camera's angles.
        val aim=ShotPacketCompat.vector(p.lookAngle)
        val level=client.level ?: return null
        val camera=ShotPacketCompat.vector(client.gameRenderer.smCamera.position())
        CapsuleContactScene.refreshRevision(client)
        val view=listOf(state,eye,aim,camera,ConfigManager.capsulePathWidth,CapsuleContactScene.revision,SafariEsp.contactRevision)
        return frames.resolve(level,view) {
            if(state?.allowUncalibratedEstimates==true) {
                val scene=CapsuleContactScene.snapshot(client,eye,aim,state.pitch)
                val label=if(scene.waterEstimate && state.estimateLabel?.contains("Water")!=true)
                    listOfNotNull(state.estimateLabel,"Water").joinToString(" / ") else state.estimateLabel
                CapsulePreviewFrameBuilder.contacts(scene.trace,camera,ConfigManager.capsulePathWidth,label)
            }
            else CapsulePreviewFrameBuilder.build(available,eye,aim).let{f->f.copy(mesh=ContactGuideGeometry.build(
                ContactTrace(f.points.mapIndexed{i,v->ContactNode(i.toDouble(),v,true)},null,ContactStop.RANGE_LIMIT,emptySet()),eye,ConfigManager.capsulePathWidth))}
        }
    }
    internal fun availability(state: CapsulePreviewState?): CapsulePreviewAvailability {
        if(Minecraft.getInstance().level !== world) { reset();return CapsulePreviewAvailability.HIDDEN }
        return rules.evaluate(state)
    }
    fun renderHud(graphics: GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        val frame=frame(client) ?: return
        val status=frame.status ?: return
        CapsuleStatusHud.render(graphics,status)
    }
}
