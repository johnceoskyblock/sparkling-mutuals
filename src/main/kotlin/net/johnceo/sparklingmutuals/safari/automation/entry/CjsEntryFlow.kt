package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariTicketType

data class CjsRoomSignal(val context: EntryContext, val generation: Long, val active: Set<String>) {
    fun valid(current: EntryContext) = context==current && generation>=0 && active.isNotEmpty()
}
enum class CjsApproachOutcome { WAITING, MOVING, COMPLETE, CANCELLED }
enum class CjsEntryPhase { WAITING, APPROACH, SETTLING, WAITING_MENU, WAITING_ADMISSION, DONE, CANCELLED, EXPIRED }
data class CjsTicketMenu(val identity: Any, val containerId: Int, val exact: Boolean, val ready: Boolean,
    val slot: Int?, val tier: SafariTicketType?)
data class CjsEntryFrame(val context: EntryContext?=null, val signal: CjsRoomSignal?=null,
    val warpGeneration: Long=0, val warpAllowed: Boolean=false, val arrivedAt: Long=0,
    val enabled: Boolean=false, val atEntry: Boolean=false, val admitted: Boolean=false,
    val guardAllows: Boolean=false, val safe: Boolean=false, val manualCancel: Boolean=false,
    val unexpectedScreen: Boolean=false, val manager: UUID?=null, val reachable: Boolean=false,
    val settled: Boolean=false, val approach: CjsApproachOutcome=CjsApproachOutcome.WAITING,
    val menu: CjsTicketMenu?=null)
sealed interface CjsEntryEffect {
    val context: EntryContext
    val warp: Long
    val generation: Long
    data class Approach(override val context: EntryContext, override val warp: Long, override val generation: Long): CjsEntryEffect
    data class Manager(override val context: EntryContext, override val warp: Long, override val generation: Long,
        val target: UUID): CjsEntryEffect
    data class Ticket(override val context: EntryContext, override val warp: Long, override val generation: Long,
        val containerId: Int, val slot: Int): CjsEntryEffect
}

/** Pure per-warp owner. Proposals reserve budgets; consumption precedes an uncertain input. */
class CjsEntryFlow {
    private var context: EntryContext?=null
    private var warp=-1L
    private var socket=-1L
    private var deadline=0L
    private var target: UUID?=null
    private var menu: CjsTicketMenu?=null
    private var ticket: CjsTicketMenu?=null
    private var managerIssued=false
    private var ticketIssued=false
    private var claimed=false
    private val clock=EntryClock()
    var phase=CjsEntryPhase.WAITING;private set
    var status="Waiting for current WebSocket discovery";private set
    val suppressesAutomaticMenus get()=claimed
    val claimedContext get()=context.takeIf { claimed }
    private fun terminal()=phase in setOf(CjsEntryPhase.DONE,CjsEntryPhase.CANCELLED,CjsEntryPhase.EXPIRED)
    fun cancel(reason: String="Automatic entry cancelled") { phase=CjsEntryPhase.CANCELLED;status=reason }
    /** A native press is latched even if it is released before the next START sample. */
    fun inputEvent(pressed: Boolean): Boolean {
        if(!pressed || !claimed || terminal()) return false
        cancel("Automatic entry cancelled: manual input")
        return true
    }
    private fun valid(f: CjsEntryFrame, now: Long): Boolean = clock.accept(now) && now<deadline &&
        f.context==context && context!=null && f.enabled && f.atEntry && !f.admitted && f.safe && f.guardAllows &&
        !f.manualCancel && !f.unexpectedScreen && f.warpAllowed && f.warpGeneration==warp &&
        f.signal?.valid(context!!)==true && f.signal.generation==socket && f.manager==target
    fun tick(f: CjsEntryFrame, now: Long): CjsEntryEffect? {
        if(!clock.accept(now)) { cancel("Automatic entry cancelled: clock rollback");return null }
        if(f.warpAllowed && f.warpGeneration>warp && f.context!=null) {
            context=f.context;warp=f.warpGeneration;deadline=entryDeadline(f.arrivedAt,12000)
            socket=-1;target=null;menu=null;ticket=null;managerIssued=false;ticketIssued=false;claimed=false
            phase=CjsEntryPhase.WAITING
        }
        if(f.context==context && context!=null && f.admitted) {
            phase=CjsEntryPhase.DONE;status="Safari admission confirmed";return null
        }
        if(terminal() || context==null) return null
        if(now>=deadline) { cancel("Automatic entry expired");phase=CjsEntryPhase.EXPIRED;return null }
        if(claimed && !valid(f,now)) { cancel("Automatic entry cancelled: room, context, guard or input changed");return null }
        if(!claimed) {
            val c=context!!
            if(f.context!=c || now<f.arrivedAt || !f.enabled || !f.atEntry || !f.safe || !f.guardAllows ||
                f.manualCancel || f.unexpectedScreen || !f.warpAllowed || f.signal?.valid(c)!=true ||
                f.manager==null || c.account==c.party.leader || c.party.members.size<2 || f.menu!=null) return null
            claimed=true;socket=f.signal.generation;target=f.manager;phase=CjsEntryPhase.APPROACH
        }
        val c=context!!
        when(phase) {
            CjsEntryPhase.APPROACH, CjsEntryPhase.SETTLING -> {
                if(f.approach==CjsApproachOutcome.CANCELLED) { cancel("Approach cancelled");return null }
                if(f.approach!=CjsApproachOutcome.COMPLETE) return CjsEntryEffect.Approach(c,warp,socket)
                phase=CjsEntryPhase.SETTLING;status="Waiting for movement to settle"
                if(!f.settled) return null
                if(!f.reachable || f.menu!=null) { cancel("Manager is not safely reachable");return null }
                phase=CjsEntryPhase.WAITING_MENU;status="Opening Safari Manager"
                return CjsEntryEffect.Manager(c,warp,socket,target!!)
            }
            CjsEntryPhase.WAITING_MENU -> {
                if(!managerIssued) return null
                val current=f.menu
                if(current==null) { if(menu!=null) cancel("Entry menu closed");return null }
                if(!current.exact || current.containerId<=0 || menu?.let { it.identity!==current.identity || it.containerId!=current.containerId }==true) {
                    cancel("Entry menu replaced");return null
                }
                menu=current
                if(!current.ready) return null
                val slot=current.slot
                if(slot==null || slot<0 || current.tier==null) { cancel("No configured ticket available");return null }
                ticket=current;phase=CjsEntryPhase.WAITING_ADMISSION;status="Selecting ${current.tier.label} ticket"
                return CjsEntryEffect.Ticket(c,warp,socket,current.containerId,slot)
            }
            else -> return null
        }
    }
    fun matches(effect: CjsEntryEffect)=effect.context==context && effect.warp==warp && effect.generation==socket
    fun consumeManager(f: CjsEntryFrame, now: Long): Boolean {
        if(managerIssued || phase!=CjsEntryPhase.WAITING_MENU) return false
        if(!valid(f,now) || !f.reachable || !f.settled || f.menu!=null || f.approach!=CjsApproachOutcome.COMPLETE) {
            cancel("Manager permission withdrawn");return false
        }
        managerIssued=true
        return true
    }
    fun consumeTicket(f: CjsEntryFrame, now: Long): Boolean {
        if(ticketIssued || phase!=CjsEntryPhase.WAITING_ADMISSION) return false
        val proposed=ticket;val current=f.menu
        if(!valid(f,now) || proposed==null || current==null || !current.exact || !current.ready ||
            proposed.identity!==current.identity || proposed.containerId!=current.containerId ||
            proposed.slot!=current.slot || proposed.tier!=current.tier || current.slot==null || current.tier==null) {
            cancel("Ticket permission withdrawn");return false
        }
        ticketIssued=true;status="Ticket sent; awaiting personal admission"
        return true
    }
}
