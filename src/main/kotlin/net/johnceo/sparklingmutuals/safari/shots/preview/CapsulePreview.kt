package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.shots.ShotVariant
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.component.DataComponents

/** Passive view of the local player; never starts recording or changes a throw. */
object CapsulePreview {
    private val rules=CapsulePreviewRules()
    private var world: Any?=null
    private var registered=false
    fun register() {
        if(registered) return
        registered=true
        WorldRenderCompat.register(CapsulePreviewRenderer::render)
    }
    fun tick(client: Minecraft) {
        world=client.level
        rules.tick(world,currentState(client))
    }
    fun reset() { world=null;rules.reset() }
    internal fun currentState(client: Minecraft): CapsulePreviewState? {
        if(!ConfigManager.experimentalCapsulePath || client.level==null) return null
        val p=client.player ?: return null
        val itemId=p.mainHandItem.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","")
        return CapsulePreviewState(ConfigManager.experimentalCapsulePath, SafariAssist.inSafari,
            ShotVariant.fromId(itemId)==ShotVariant.ORDINARY,client.options.cameraType.isFirstPerson,p.onGround(),
            p.isShiftKeyDown,p.isSprinting,p.isSwimming,p.abilities.flying,p.isPassenger,
            ConfigManager.hideFlyingCapsules,ConfigManager.hideGroundCapsules,client.smScreen!=null,
            client.options.smHideGui,p.xRot.toDouble(),ShotPacketCompat.vector(p.position()),ShotPacketCompat.vector(p.deltaMovement))
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
        val frame=CapsulePreviewFrameBuilder.build(available,ShotPacketCompat.vector(p.eyePosition),ShotPacketCompat.vector(p.lookAngle))
        val status=frame.status ?: return
        graphics.text(client.font,status,(graphics.guiWidth()-client.font.width(status))/2,
            graphics.guiHeight()/2+18,0xFFFFB347.toInt(),true)
    }
}
