package net.johnceo.sparklingmutuals.safari.automation

enum class EntryStage { UNKNOWN, OTHER, TORRHUS, SAFARI }

/** Entry controls always require current party presence; run/location heuristics cannot bypass them. */
object EntryGuardDecision {
    fun block(stage: EntryStage, downtime: Boolean, kick: Boolean,
              presenceReady: Boolean, identityPending: Boolean, shiftOverride: Boolean = false): Boolean = !shiftOverride && when (stage) {
        EntryStage.OTHER -> false
        EntryStage.UNKNOWN -> downtime || kick || !presenceReady || identityPending
        EntryStage.TORRHUS -> downtime || identityPending
        EntryStage.SAFARI -> kick || !presenceReady
    }
}

/** Brief packet-settling pause; after two seconds the current party tab list decides. */
class EntryKickHold {
    private var instance: Any? = null
    private var until = 0L
    val active get() = holding(System.currentTimeMillis())
    fun holding(now: Long) = now < until
    fun report(world: Any?, at: Long = System.currentTimeMillis()) {
        if (instance !== world) reset()
        instance = world
        until = maxOf(until, at + 2000)
    }
    fun observe(world: Any?) { if (instance !== world) reset() }
    fun reset() { instance = null; until = 0 }
}

/** The latest readiness state per sender is sufficient; kick evidence is independent and conservative. */
data class EntryPartyMessage(val text: String, val sender: String, val world: Any?, val kickContext: Boolean, val downtime: Boolean, val receivedAt: Long = System.currentTimeMillis())
class EntryPartyInbox(private val capacity: Int = 32) {
    private val messages = linkedMapOf<String, EntryPartyMessage>()
    private var lastWorld: Any? = null
    fun observeWorld(world: Any) {
        if (lastWorld != null && lastWorld !== world) transfer()
        lastWorld = world
    }
    var overflowReadiness = false
        private set
    var overflowKick = false
        private set
    val overflow get() = overflowReadiness || overflowKick
    private val readyAfterOverflow = mutableSetOf<String>()
    val pending get() = messages.isNotEmpty() || overflow
    val pendingKick get() = overflowKick || messages.values.any { it.kickContext }
    val pendingDowntime get() = overflowReadiness || messages.values.any { it.downtime }
    fun add(item: EntryPartyMessage) {
        // Transition before enqueue: the first fresh kick can arrive before the world's first tick.
        item.world?.let(::observeWorld)
        fun put(kind: String, message: EntryPartyMessage) {
            val key = item.sender + kind
            if (messages.size >= capacity && key !in messages) {
                if (kind == ":kick") overflowKick = true else { overflowReadiness = true; readyAfterOverflow.clear() }
                return
            }
            val previous = messages.remove(key)
            // Multiple different unverified kick reports cannot safely be reduced to just one claimed victim.
            messages[key] = if (kind == ":kick" && previous != null && previous.text != message.text)
                message.copy(text = "Party > ${item.sender}: kick") else message
        }
        if (item.downtime) put(":ready", item.copy(kickContext = false))
        if (item.kickContext) put(":kick", item.copy(downtime = false))
    }
    fun entries() = messages.entries.map { it.key to it.value }
    fun remove(key: String) { messages.remove(key) }
    fun validated(id: String, ready: Boolean, members: Set<String>) {
        if (!overflowReadiness) return
        readyAfterOverflow.retainAll(members)
        if (ready) readyAfterOverflow.add(id) else readyAfterOverflow.remove(id)
        if (members.isNotEmpty() && readyAfterOverflow.containsAll(members)) { overflowReadiness = false; readyAfterOverflow.clear() }
    }
    fun transfer() {
        overflowKick = false
        messages.entries.removeAll { !it.value.downtime }
        messages.replaceAll { _, item -> item.copy(world = null, kickContext = false) }
    }
    fun disableDowntime() { messages.entries.removeAll { it.value.downtime }; overflowReadiness = false; readyAfterOverflow.clear() }
    fun disableKick() { messages.entries.removeAll { it.value.kickContext }; overflowKick = false }
    fun clear() { messages.clear(); readyAfterOverflow.clear(); overflowReadiness = false; overflowKick = false }
}

/** Manager identity survives temporary label/player-list changes, but never crosses a client world. */
class EntryManagerTargets {
    private var world: Any? = null
    private val known = mutableSetOf<String>()
    private val anchors = mutableSetOf<Triple<Double, Double, Double>>()
    fun recognize(instance: Any?, id: String, proxy: Boolean, x: Double, y: Double, z: Double,
        named: Boolean, partyPlayer: Boolean, labels: List<Triple<Double, Double, Double>>): Boolean {
        if (world !== instance) { reset(); world = instance }
        if (instance == null || partyPlayer) return false
        anchors.addAll(labels)
        if (named) { anchors.add(Triple(x, y, z)); known.add(id); return true }
        if (!proxy) return false
        if (id in known) return true
        val aligned = anchors.any { (ax, ay, az) ->
            kotlin.math.abs(x - ax) <= .5 && kotlin.math.abs(z - az) <= .5 && ay - y in -.5..3.5
        }
        if (aligned) known.add(id)
        return aligned
    }
    fun reset() { world = null; known.clear(); anchors.clear() }
}
