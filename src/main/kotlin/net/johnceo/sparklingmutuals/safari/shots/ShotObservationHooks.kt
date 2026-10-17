package net.johnceo.sparklingmutuals.safari.shots

import net.johnceo.sparklingmutuals.compat.ShotPacketCompat
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player

/** Off means no world inspection. Errors disable observation through the owner, never cancel gameplay. */
object ShotObservationHooks {
    private var adapter:ShotObservationAdapter?=null
    private var clock:()->ShotStamp={ ShotStamp(0,0) }
    private var failed:(Throwable)->Unit={}
    private var cycles=ShotInputCycles()
    fun bind(adapter:ShotObservationAdapter?,clock:()->ShotStamp,failed:(Throwable)->Unit) {
        this.adapter=adapter;this.clock=clock;this.failed=failed
        cycles=ShotInputCycles({ t,h,v,r,standalone ->
            adapter?.standalone=standalone
            adapter?.input(Minecraft.getInstance(),t,h,v,r,clock())
        },{ t,result,route -> adapter?.route(t,route);adapter?.continuation(t,result,clock()) })
    }
    private inline fun observe(stage:String,block:()->Unit) {
        if(adapter==null) return
        try { block() } catch(t:Exception) { try { failed(ShotObservationFailure(stage,t)) } catch(_:Exception) { /* Gameplay still proceeds. */ } }
    }
    fun begin()=observe("input/begin") { cycles.begin() }
    fun end()=observe("input/end") { cycles.end() }
    fun tick(client:Minecraft)=observe("tick") { adapter?.tick(client,clock()) }
    fun packet(packet:Any) {
        // Existing client-thread hooks invalidate preview checks even with recording off.
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
        observe("packet") {
            val client=Minecraft.getInstance();val level=client.level ?: return@observe
            ShotPacketCompat.copy(packet,level) { adapter?.packetBase(it) }.forEach { adapter?.packet(client,it,clock()) }
        }
    }
    fun use(player:Player,hand:InteractionHand,route:ShotUseRoute):ShotInputToken? {
        if(adapter==null) return null
        var token:ShotInputToken?=null
        observe("input/use") {
            if(player!==Minecraft.getInstance().player) return@observe
            val variant=ShotVariant.fromId(player.getItemInHand(hand).get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","")) ?: return@observe
            token=cycles.observe(if(hand==InteractionHand.MAIN_HAND) ShotHand.MAIN else ShotHand.OFF,variant,route)
        }
        return token
    }
    fun returned(token:ShotInputToken?,result:InteractionResult)=observe("input/returned") {
        if(token!=null) cycles.returned(token,when(result) {
            InteractionResult.PASS -> ShotUseResult.PASS
            InteractionResult.FAIL -> ShotUseResult.FAIL
            else -> if(result.consumesAction()) ShotUseResult.SUCCESS else ShotUseResult.OTHER
        })
    }
}
