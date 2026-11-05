package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard
import net.johnceo.sparklingmutuals.safari.automation.SafariTicketSelect
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

internal object CjsManagerGeometry {
    fun hit(eye: Vec3, view: Vec3, range: Double, box: AABB, block: Vec3?): Vec3? {
        if(!range.isFinite() || range !in .1..6.0 ||
            !listOf(eye.x,eye.y,eye.z,view.x,view.y,view.z).all { it.isFinite() }) return null
        val end=eye.add(view.normalize().scale(range))
        val hit=if(box.contains(eye)) eye else box.clip(eye,end).orElse(null) ?: return null
        if(block!=null && eye.distanceToSqr(block)<=eye.distanceToSqr(hit)) return null
        return hit
    }
}

/** Shared by native dispatch and controlled tests: the permit is consumed before any side effect. */
internal object CjsEntryDispatch {
    fun send(flow: CjsEntryFlow, effect: CjsEntryEffect, frame: () -> CjsEntryFrame, now: Long,
        boundary: (() -> Boolean) -> Boolean, manager: (UUID) -> Unit, ticket: (Int,Int) -> Unit): Boolean {
        if(!flow.matches(effect) || effect is CjsEntryEffect.Approach) return false
        val sent=try { boundary {
            val f=frame()
            when(effect) {
                is CjsEntryEffect.Manager -> if(flow.consumeManager(f,now)) { manager(effect.target);true } else false
                is CjsEntryEffect.Ticket -> if(flow.consumeTicket(f,now)) { ticket(effect.containerId,effect.slot);true } else false
            }
        } } catch(error: Exception) { flow.cancel("Input outcome uncertain: ${error.javaClass.simpleName}");return false }
        if(!sent) flow.cancel("Automatic input permission withdrawn")
        return sent
    }
}

/** No timers or raw packets. The normal guarded Minecraft interaction methods are the only inputs. */
internal class CjsEntryInput(private val approach: CjsApproachInput) {
    internal fun ticketMenu(identity: Any, containerId: Int, options: List<SafariTicketSelect.Option>): CjsTicketMenu {
        val choice = SafariTicketSelect.chooseLowest(options)
        // Higher tiers are irrelevant once the lowest available ticket has been confirmed.
        val allUnavailable = SafariTicketType.entries.all { tier ->
            options.singleOrNull { it.tier == tier }?.available == false
        }
        return CjsTicketMenu(identity, containerId, true, choice != null || allUnavailable, choice?.slot, choice?.tier)
    }

    private fun hit(client: Minecraft, target: Entity): Vec3? {
        val player=client.player ?: return null
        val level=client.level ?: return null
        if(target.isRemoved || level.getEntity(target.id)!==target || !player.isWithinEntityInteractionRange(target,0.0)) return null
        val eye=player.eyePosition;val view=player.getViewVector(1f);val range=player.entityInteractionRange()
        val block=level.clip(ClipContext(eye,eye.add(view.scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player))
        return CjsManagerGeometry.hit(eye,view,range,target.boundingBox,block.location.takeIf { block.type!=HitResult.Type.MISS })
    }
    fun frame(client: Minecraft, base: EntryDryRunFrame, signal: CjsRoomSignal?, enabled: Boolean,
        warpAllowed: Boolean, generation: Long, flow: CjsEntryFlow): CjsEntryFrame {
        val player=client.player
        val screen=client.smScreen
        val expectedMenu=screen is AbstractContainerScreen<*> && SafariTicketSelect.entryMenu(screen.title.string) &&
            flow.phase in setOf(CjsEntryPhase.WAITING_MENU,CjsEntryPhase.WAITING_ADMISSION)
        val target=approach.manager(client,base)
        val velocity=player?.deltaMovement?.horizontalDistance() ?: Double.NaN
        val safe=player!=null && client.level!=null && client.connection!=null && client.gameMode!=null &&
            !client.isPaused && player.isAlive && player.onGround() &&
            !player.isInWater && !player.isPassenger && !player.abilities.flying && !player.isSprinting &&
            !player.horizontalCollision && velocity.isFinite() && velocity<=.16 && approach.userIdle(client) && approach.aimMatches(client) &&
            (expectedMenu || approach.gameplayAvailable(client))
        val menu=if(expectedMenu && player!=null) {
            val current=player.containerMenu
            ticketMenu(current, current.containerId, SafariTicketSelect.options(client))
        } else null
        return CjsEntryFrame(base.context,signal,generation,warpAllowed,base.arrivedAt,enabled && base.enabled,base.atEntry,
            base.activeRun,SafariManagerGuard.allowsCjApproach(client),safe,false,screen!=null && !expectedMenu,
            target?.uuid,target?.let { hit(client,it)!=null }==true,velocity<.01 && !approach.ownsKeys,
            approach.outcome,menu)
    }
    fun dispatch(client: Minecraft, flow: CjsEntryFlow, effect: CjsEntryEffect,
        base: () -> EntryDryRunFrame, frame: () -> CjsEntryFrame, now: Long): Boolean {
        approach.release(client)
        val expected=SafariPartySync.entryRoom(client)
        if(expected==null || expected.generation!=effect.generation) { flow.cancel("WebSocket room unavailable");return false }
        return CjsEntryDispatch.send(flow,effect,frame,now,{ action -> SafariPartySync.withEntryRoom(client,expected,action) }, { id ->
            val player=checkNotNull(client.player)
            val target=checkNotNull(approach.manager(client,base())).also { check(it.uuid==id) }
            val point=checkNotNull(hit(client,target))
            check(!SafariManagerGuard.shouldBlock(target))
            checkNotNull(client.gameMode).interact(player,target,EntityHitResult(target,point),InteractionHand.MAIN_HAND)
        }, { container,slot ->
            val player=checkNotNull(client.player)
            check(player.containerMenu.containerId==container && !SafariManagerGuard.shouldBlockEntry(container,slot))
            checkNotNull(client.gameMode).handleContainerInput(container,slot,0,ContainerInput.PICKUP,player)
        })
    }
}
