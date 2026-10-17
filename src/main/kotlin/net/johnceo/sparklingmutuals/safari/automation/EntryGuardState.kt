package net.johnceo.sparklingmutuals.safari.automation

enum class EntryStage { UNKNOWN, OTHER, TORRHUS, SAFARI }

/** Entry controls always require current party presence; run/location heuristics cannot bypass them. */
object EntryGuardDecision {
    fun block(stage: EntryStage, downtime: Boolean, kick: Boolean,
              presenceReady: Boolean, identityPending: Boolean): Boolean = when (stage) {
        EntryStage.OTHER -> false
        EntryStage.UNKNOWN -> downtime || kick || !presenceReady || identityPending
        EntryStage.TORRHUS -> downtime || identityPending
        EntryStage.SAFARI -> kick || !presenceReady || identityPending
    }
}

/** A timer or repeated snapshot cannot prove recovery from a kick. IDs must be normalized UUIDs. */
class EntryKickHold {
    private data class Report(val instance: Any?, var sawAbsent: Boolean = false)
    private val reports = mutableMapOf<String?, Report>()
    val active get() = reports.isNotEmpty()
    fun report(instance: Any?, victim: String?) { reports.putIfAbsent(victim, Report(instance)) }
    fun observe(instance: Any?, members: Set<String>?, present: Set<String>, ready: Boolean) {
        if (instance == null || members == null) return
        reports.entries.removeAll { (target, report) ->
            val absent = if (target == null) members.any { it !in present } else target !in present
            if (absent) report.sawAbsent = true
            val recovered = when {
                instance !== report.instance -> true
                target == null -> report.sawAbsent
                target !in members -> true
                else -> report.sawAbsent && target in present
            }
            ready && recovered
        }
    }
    fun reset() { reports.clear() }
}

/** The latest readiness state per sender is sufficient; kick evidence is independent and conservative. */
data class EntryPartyMessage(val text: String, val sender: String, val world: Any?, val kickContext: Boolean, val downtime: Boolean)
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
