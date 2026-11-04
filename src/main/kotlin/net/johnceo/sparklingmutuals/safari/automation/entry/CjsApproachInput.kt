package net.johnceo.sparklingmutuals.safari.automation.entry

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.ai.attributes.Attributes
import org.lwjgl.glfw.GLFW
import kotlin.math.*

/** Native input owner, revalidated at START_CLIENT_TICK before vanilla consumes walking keys. */
class CjsApproachInput {
    private var world: Any? = null
    private var generation = -1L
    private var policy = CjsApproach()
    private var input: CjsWalkingKeys? = null
    private var lastStatus = ""
    var outcome=CjsApproachOutcome.WAITING; private set
    val ownsKeys get()=input?.owns==true
    fun userIdle(client: Minecraft): Boolean {
        val o=client.options
        return listOf(o.keyUp,o.keyDown,o.keyLeft,o.keyRight,o.keyJump,o.keyShift,o.keySprint,o.keyAttack,o.keyUse).none {
            physical(client,it) || it.isDown && (!ownsKeys || it!==o.keyUp && it!==o.keyShift)
        }
    }
    internal fun physical(client: Minecraft, binding: KeyMapping): Boolean {
        val key = InputConstants.getKey(binding.saveString())
        if (key.value < 0) return false
        return when (key.type) {
            InputConstants.Type.KEYSYM -> InputConstants.isKeyDown(client.window, key.value)
            InputConstants.Type.MOUSE -> GLFW.glfwGetMouseButton(client.window.handle(),key.value)==GLFW.GLFW_PRESS
            else -> true // Cannot verify a scancode binding; do not claim idle input.
        }
    }
    fun release(client: Minecraft) {
        input?.drive(false,false)
    }
    fun reset(client: Minecraft) {
        release(client); world=null;generation=-1; policy=CjsApproach(); lastStatus="";outcome=CjsApproachOutcome.WAITING
    }
    internal fun manager(client: Minecraft, frame: EntryDryRunFrame): Entity? {
        val level=client.level ?: return null
        val self=client.player ?: return null
        val entities=level.entitiesForRendering().filter { !it.isRemoved && level.getEntity(it.id)===it }.toList()
        val labels=entities.filter { SafariRules.strip(it.customName?.string ?: it.name.string).equals("Safari Manager",true) }
        val players=client.connection?.listedOnlinePlayers.orEmpty().mapTo(hashSetOf()) { it.profile.id } +
            frame.context?.party?.members.orEmpty() + self.uuid
        val bodies=entities.filter { e -> e.uuid !in players && self.distanceToSqr(e)<36 &&
            (e is Player || e.type.toShortString()=="interaction") && labels.any {
                hypot(it.x-e.x,it.z-e.z)<.6 && it.y-e.y in 0.0..3.5
            }
        }
        val npcPlayers=bodies.filterIsInstance<Player>()
        return if(npcPlayers.isNotEmpty()) npcPlayers.singleOrNull() else bodies.singleOrNull()
    }
    private fun pathClear(client: Minecraft, target: Entity, friction: Double): Boolean {
        val p=client.player ?: return false
        val level=client.level ?: return false
        val dx=target.x-p.x;val dz=target.z-p.z;val distance=hypot(dx,dz)
        if (!distance.isFinite() || distance<1.0) return false
        val heading=policy.heading()
        val x=heading?.let { -sin(it*PI/180) } ?: (dx/distance)
        val z=heading?.let { cos(it*PI/180) } ?: (dz/distance)
        if (!level.noCollision(p,p.boundingBox.expandTowards(x*1.05,0.0,z*1.05))) return false
        val swept=p.boundingBox.expandTowards(x*1.05,0.0,z*1.05)
        if (level.getEntities(p,swept).any { it.isAlive && it!==target && (it is Player || it.isPushable) }) return false
        val radius=p.bbWidth/2.0
        for (step in 0..4) for (ox in listOf(-radius,radius)) for (oz in listOf(-radius,radius)) {
            val sx=p.x+x*step*.25+ox;val sz=p.z+z*step*.25+oz
            val pos=BlockPos.containing(sx,p.y-.01,sz)
            val block=level.getBlockState(pos)
            if(friction !in .5.. .7 || abs(block.block.friction-friction)>.0001 || !block.fluidState.isEmpty) return false
            val supported=block.getCollisionShape(level,pos).toAabbs().any { box ->
                sx-pos.x>=box.minX && sx-pos.x<=box.maxX && sz-pos.z>=box.minZ && sz-pos.z<=box.maxZ &&
                    abs(pos.y+box.maxY-p.y)<.1
            }
            if(!supported) return false
        }
        return true
    }
    fun tick(client: Minecraft, frame: EntryDryRunFrame, signal: CjsRoomSignal?, enabled: Boolean,
        warpAllowed: Boolean, warpGeneration: Long, now: Long, report: (String)->Unit) {
        if(world!==client.level) { release(client);policy.cancel();world=client.level }
        if(generation!=warpGeneration && warpAllowed) {
            release(client);policy=CjsApproach();generation=warpGeneration;lastStatus="";outcome=CjsApproachOutcome.WAITING
        }
        val player=client.player
        try {
            val driver=input ?: CjsWalkingKeys(client.options.keyUp,client.options.keyShift,
                {client.options.toggleCrouch().get()}, {physical(client,client.options.keyUp)},
                {physical(client,client.options.keyShift)}).also { input=it }
            val target=if(frame.atEntry) manager(client,frame) else null
            val controls=client.options
            val keys=listOf(controls.keyUp,controls.keyDown,controls.keyLeft,controls.keyRight,controls.keyJump,
                controls.keyShift,controls.keySprint,controls.keyAttack,controls.keyUse)
            val velocity=player?.deltaMovement?.horizontalDistance() ?: Double.NaN
            val friction=player?.let { client.level?.getBlockState(BlockPos.containing(it.x,it.y-.01,it.z))?.block?.friction?.toDouble() } ?: Double.NaN
            val speed=player?.getAttributeValue(Attributes.MOVEMENT_SPEED) ?: Double.NaN
            val sneak=player?.getAttributeValue(Attributes.SNEAKING_SPEED) ?: Double.NaN
            val acceleration=speed*sneak*.98*.21600002/(friction*friction*friction)+.00001
            val drag=friction*.91+.000001
            val foreignLogical=keys.any { it.isDown && (!driver.owns || it!==controls.keyUp && it!==controls.keyShift) }
            val safe=warpAllowed && player!=null && client.level!=null && client.connection!=null && frame.atEntry && !frame.activeRun &&
                client.isWindowActive && client.mouseHandler.isMouseGrabbed && client.smScreen==null && !client.isPaused &&
                player.isAlive && player.onGround() && !player.isInWater && !player.isPassenger && !player.abilities.flying &&
                !player.isSprinting && !player.horizontalCollision && velocity<=.16 && (driver.owns || velocity<.01) &&
                speed.isFinite() && speed in .001.. .4 && sneak.isFinite() && sneak in .05.. .31 &&
                !foreignLogical && keys.none { physical(client,it) } && target!=null && pathClear(client,target,friction) &&
                SafariManagerGuard.allowsCjApproach(client)
            val f=CjsApproachFrame(frame.context,enabled && frame.enabled,signal,target?.uuid,
                player?.x ?: Double.NaN,player?.y ?: Double.NaN,player?.z ?: Double.NaN,
                target?.x ?: Double.NaN,target?.z ?: Double.NaN,player?.yRot ?: 0f,player?.xRot ?: 0f,safe,
                player?.eyeY ?: Double.NaN,target?.let { it.y+it.bbHeight*.7 } ?: Double.NaN,
                player?.isCrouching ?: false,velocity,acceleration,drag)
            val action=policy.tick(f,now)
            outcome=action.outcome
            driver.drive(action.forward,action.crouch)
            if(action.yaw!=null && player!=null) { player.yRot=action.yaw;player.xRot=action.pitch!! }
            if(action.status!=lastStatus && (enabled || driver.owns)) { lastStatus=action.status;report(action.status) }
        } catch(e: Exception) {
            policy.cancel();outcome=CjsApproachOutcome.CANCELLED
            try { release(client) } finally { report("Approach cancelled: ${e.javaClass.simpleName}") }
        }
    }
}
