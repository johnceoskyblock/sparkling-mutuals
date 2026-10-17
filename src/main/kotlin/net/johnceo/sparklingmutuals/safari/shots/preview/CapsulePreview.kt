package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
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
    private var frameState:CapsulePreviewState?=null
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
    fun reset() { world=null;rules.reset();frames.reset();frameState=null;CapsuleContactScene.reset() }
    internal fun currentState(client: Minecraft): CapsulePreviewState? {
        if(!ConfigManager.experimentalCapsulePath || client.level==null) return null
        return collectionState(client)?.copy(enabled=ConfigManager.experimentalCapsulePath,allowUncalibratedEstimates=ConfigManager.allowUncalibratedCapsuleEstimates)
    }
    internal fun collectionState(client:Minecraft):CapsulePreviewState? {
        if(client.level==null) return null
        val p=client.player ?: return null
        val itemId=p.mainHandItem.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","")
        return CapsulePreviewState(true, SafariAssist.inSafari,
            ShotVariant.fromId(itemId)==ShotVariant.ORDINARY,client.options.cameraType.isFirstPerson,p.onGround(),
            p.isShiftKeyDown,p.isSprinting,p.isSwimming,p.abilities.flying,p.isPassenger,
            ConfigManager.hideFlyingCapsules,ConfigManager.hideGroundCapsules,client.smScreen!=null,
            client.options.smHideGui,p.xRot.toDouble(),ShotPacketCompat.vector(p.position()),ShotPacketCompat.vector(p.deltaMovement),
            true)
    }
    internal fun frame(client:Minecraft):CapsulePreviewFrame? {
        val state=currentState(client);val available=availability(state)
        val p=client.player ?: return null
        if(available!=CapsulePreviewAvailability.AVAILABLE) { frames.reset();frameState=null;return CapsulePreviewFrameBuilder.build(available,ShotPacketCompat.vector(p.eyePosition),ShotPacketCompat.vector(p.lookAngle)) }
        val partial=client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val eye=ShotPacketCompat.vector(p.getEyePosition(partial));val aim=ShotPacketCompat.vector(p.getViewVector(partial))
        val frame=if(state?.allowUncalibratedEstimates==true) CapsulePreviewFrameBuilder.contacts(
            CapsuleContactScene.snapshot(client,eye,aim,state.pitch).trace,ShotPacketCompat.vector(client.gameRenderer.smCamera.position()),ConfigManager.capsulePathWidth)
        else CapsulePreviewFrameBuilder.build(available,eye,aim).let{f->f.copy(mesh=ContactGuideGeometry.build(
            ContactTrace(f.points.mapIndexed{i,v->ContactNode(i.toDouble(),v,true)},null,ContactStop.RANGE_LIMIT,emptySet()),eye,ConfigManager.capsulePathWidth))}
        client.level?.let{frames.store(it,ShotPacketCompat.vector(p.lookAngle),frame)};frameState=state
        return frame
    }
    internal fun availability(state: CapsulePreviewState?): CapsulePreviewAvailability {
        if(Minecraft.getInstance().level !== world) { reset();return CapsulePreviewAvailability.HIDDEN }
        return rules.evaluate(state)
    }
    fun renderHud(graphics: GuiGraphicsExtractor) {
        val client=Minecraft.getInstance()
        val state=currentState(client)
        val available=availability(state)
        val p=client.player ?: return
        val frame=if(state==frameState && available==CapsulePreviewAvailability.AVAILABLE)
            frames.current(client.level,ShotPacketCompat.vector(p.lookAngle)) ?: return
            else CapsulePreviewFrameBuilder.build(available,ShotPacketCompat.vector(p.eyePosition),ShotPacketCompat.vector(p.lookAngle))
        val status=frame.status ?: return
        val lines=client.font.split(net.minecraft.network.chat.Component.literal(status),(graphics.guiWidth()-16).coerceAtLeast(1))
        lines.forEachIndexed { index,line ->
            graphics.text(client.font,line,(graphics.guiWidth()-client.font.width(line))/2,
                graphics.guiHeight()/2+18+index*client.font.lineHeight,0xFFFFB347.toInt(),true)
        }
    }
}
