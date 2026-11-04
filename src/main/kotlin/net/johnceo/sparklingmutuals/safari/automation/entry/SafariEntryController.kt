package net.johnceo.sparklingmutuals.safari.automation.entry

import net.johnceo.sparklingmutuals.safari.SafariRoster

enum class EntryPhase { OFF, WAITING_CONTEXT, WAITING_SIGHTING, READY, WAITING_MENU, WAITING_ADMISSION, DONE, CANCELLED, EXPIRED }

/** Evidence supplied by a future adapter, never inferred from menu shape alone. */
data class EntryMenu(val containerId: Int, val exact: Boolean, val ready: Boolean, val availableSlot: Int?) {
    init { require(containerId > 0 && (availableSlot == null || availableSlot in 0..1023)) }
}

data class EntryFrame(
    val context: EntryContext?, val enabled: Boolean = false, val ticketHelperEnabled: Boolean = false,
    val atEntry: Boolean = false, val destinationAssociated: Boolean = false, val partyKnown: Boolean = false,
    val guardAllows: Boolean = false, val managerLoaded: Boolean = false, val managerReachable: Boolean = false,
    val menu: EntryMenu? = null, val personalAdmission: Boolean = false,
    val manualCancel: Boolean = false, val unrelatedScreen: Boolean = false
)

/** Proposals only. Nothing here sends chat, touches a menu, or interacts with an entity. */
sealed interface EntryEffect {
    data class Query(val message: EntryMessage.Query) : EntryEffect
    data class Manager(val attempt: String) : EntryEffect
    data class Ticket(val attempt: String, val containerId: Int, val slot: Int) : EntryEffect
}

class SafariEntryController(private val token: () -> String = ::entryToken) {
    private var context: EntryContext? = null
    private var arrivedAt = 0L
    private val clock = EntryClock()
    private val requester = EntryReportRequester(token)
    private var sighting: EntrySighting? = null
    private var managerProposed = false
    private var ticketProposed = false
    private var ownedMenu: Int? = null
    private var ownedSlot: Int? = null
    private var attempt = ""
    private var verificationEstablished = false
    private val capturedAt = mutableMapOf<String, Long>()
    private val retiredEvents = mutableSetOf<String>()
    var phase = EntryPhase.OFF
        private set
    var suppressesAutomaticMenus = false
        private set
    /** Permission was consumed before a hypothetical send; server consumption remains unknown. */
    var ticketIssued = false
        private set

    /** A world replacement alone is not evidence of a genuinely new arrival. */
    fun beginArrival(current: EntryContext, now: Long, confirmedNewArrival: Boolean): Boolean {
        if (!confirmedNewArrival || now < 0 || context?.sameArrival(current) == true) return false
        context = current; arrivedAt = now; clock.reset(now); requester.beginArrival(current, now)
        sighting = null; managerProposed = false; ticketProposed = false; ownedMenu = null; ownedSlot = null
        ticketIssued = false; suppressesAutomaticMenus = false
        verificationEstablished = false; capturedAt.clear(); retiredEvents.clear()
        attempt = token().also { require(EntryWire.token.matches(it)) }
        phase = EntryPhase.WAITING_CONTEXT
        return true
    }
    fun observe(value: EntrySighting) {
        if (value.context != context || terminal() || value.event in retiredEvents ||
            capturedAt[value.species]?.let { value.receivedAt <= it } == true) return
        val old = sighting
        if (old?.event == value.event && value.expiresAt <= old.receivedAt) { cancel(); return }
        sighting = if (old?.event == value.event) old.copy(expiresAt = minOf(old.expiresAt, value.expiresAt)) else value
    }
    fun receiveReply(current: EntryContext, sender: EntryPeer, reply: EntryMessage.Reply, now: Long): Boolean {
        val value = requester.receive(current, sender, reply, now) ?: return false
        if (current != context || terminal()) return false
        observe(value)
        return true
    }
    fun captured(current: EntryContext, species: String, now: Long) {
        if (current != context || SafariRoster.all.none { it.name == species }) return
        if (!clock.accept(now)) { cancel(); return }
        capturedAt[species] = now
        requester.cancel() // Any in-flight pre-capture reply can describe the now-retired opportunity.
        if (sighting?.species != species) return
        val event = sighting!!.event
        if (retiredEvents.size >= 128 && event !in retiredEvents) { cancel(); return }
        retiredEvents.add(event)
        sighting = null
        if (managerProposed) cancel() else if (!terminal()) phase = EntryPhase.WAITING_SIGHTING
    }
    private fun terminal() = phase in setOf(EntryPhase.OFF, EntryPhase.DONE, EntryPhase.CANCELLED, EntryPhase.EXPIRED)
    fun cancel() {
        if (managerProposed) suppressesAutomaticMenus = true
        requester.cancel(); sighting = null; phase = EntryPhase.CANCELLED
    }
    /** Re-arm has no authority to erase an existing arrival's action or request budget. */
    fun rearm(): Boolean = false

    private fun base(frame: EntryFrame, current: EntryContext) = frame.enabled && frame.ticketHelperEnabled &&
        frame.atEntry && frame.destinationAssociated && frame.partyKnown && frame.guardAllows &&
        !frame.manualCancel && !frame.unrelatedScreen && current.party.members.size > 1 && current.party.leader != current.account

    fun tick(frame: EntryFrame, now: Long): List<EntryEffect> {
        val current = context ?: return emptyList()
        if (!clock.accept(now) || frame.context != current) { cancel(); return emptyList() }
        // Cancellation cannot undo a sent input; late genuine personal evidence is still recorded.
        if (frame.personalAdmission) { phase = EntryPhase.DONE; suppressesAutomaticMenus = false; requester.cancel(); return emptyList() }
        if (frame.manualCancel || frame.unrelatedScreen) { cancel(); return emptyList() }
        if (!frame.enabled || !frame.ticketHelperEnabled) { cancel(); phase = EntryPhase.OFF; return emptyList() }
        if (terminal()) return emptyList()
        val verified = frame.partyKnown && frame.destinationAssociated
        if (!verified && verificationEstablished) { cancel(); return emptyList() }
        if (verified) verificationEstablished = true
        if (now >= entryDeadline(arrivedAt, 12000)) { cancel(); phase = EntryPhase.EXPIRED; return emptyList() }
        if (!base(frame, current)) {
            if (managerProposed) cancel() else phase = EntryPhase.WAITING_CONTEXT
            return emptyList()
        }
        if (ticketProposed) return emptyList()
        val fresh = sighting?.takeIf { it.valid(current, now) }
        if (fresh == null) {
            if (managerProposed) { cancel(); return emptyList() }
            phase = EntryPhase.WAITING_SIGHTING
            return requester.request(current, now)?.let { listOf(EntryEffect.Query(it)) }.orEmpty()
        }
        if (!managerProposed) {
            phase = EntryPhase.READY
            if (!frame.managerLoaded || !frame.managerReachable || frame.menu != null) return emptyList()
            managerProposed = true; phase = EntryPhase.WAITING_MENU
            return listOf(EntryEffect.Manager(attempt))
        }
        val menu = frame.menu ?: return emptyList()
        if (!menu.exact || ownedMenu?.let { it != menu.containerId } == true) { cancel(); return emptyList() }
        ownedMenu = menu.containerId
        if (!menu.ready) return emptyList()
        val slot = menu.availableSlot ?: run { cancel(); return emptyList() }
        ownedSlot = slot
        ticketProposed = true; phase = EntryPhase.WAITING_ADMISSION
        return listOf(EntryEffect.Ticket(attempt, menu.containerId, slot))
    }

    /** Advisory ownership check; a future sender MUST atomically consume the permit below. */
    fun allowsAutomaticTicket(current: EntryContext, containerId: Int, now: Long): Boolean =
        clock.accept(now) && current == context && ticketProposed && !ticketIssued && !suppressesAutomaticMenus &&
            phase == EntryPhase.WAITING_ADMISSION && ownedMenu == containerId &&
            now < entryDeadline(arrivedAt, 12000) && sighting?.valid(current, now) == true

    /** Recheck the complete current frame at the packet boundary; successful permission is one-use. */
    fun consumeTicketPermit(frame: EntryFrame, now: Long): Boolean {
        if (ticketIssued || phase == EntryPhase.DONE) return false
        val current = context ?: return false
        val menu = frame.menu
        if (frame.context != current || !base(frame, current) || menu == null || !menu.exact || !menu.ready ||
            menu.availableSlot == null || menu.availableSlot != ownedSlot ||
            !allowsAutomaticTicket(current, menu.containerId, now)) { cancel(); return false }
        ticketIssued = true
        return true
    }
}
