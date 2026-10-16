package net.johnceo.sparklingmutuals.safari.shots

/** Client-thread reducer. Sink ownership is explicit so ring-to-writer transfer never double-reserves bytes. */
class ShotSession(val metadata: ShotMetadata, private val limits: ShotLimits, private val sink: ShotSink, private val budget: ShotBufferBudget) {
    private data class Pending(val event: ShotRecord.Event, val line: ByteArray)
    private data class InputState(val input: ShotObservation.Input, val at: Long,
        val tracks: MutableSet<String> = linkedSetOf(), val feedback: ArrayDeque<Long> = ArrayDeque(),
        val annotations: ArrayDeque<ShotManualOutcome> = ArrayDeque(), var ambiguous: Boolean = false)
    private val inputs = linkedMapOf<String, InputState>()
    private val seenCapsules = mutableSetOf<String>()
    private val activeCapsules = linkedMapOf<String,ShotObservation.Capsule>()
    private val ring = ArrayDeque<Pending>()
    private var last = ShotStamp(0,0)
    private var sequence = 0L
    private var events = 0
    private var bytes = 0L
    private var reason: ShotStopReason? = null
    private var lastInput: Long? = null
    private var gaps = false
    init {
        require(metadata.durationSeconds in 10..600)
        require(limits.maxInputs in 1..300 && limits.maxEvents in 2..100_000)
        require(budget.tryReserve(limits.footerReserve)) { "shot_buffer_unavailable" }
        val header = ShotCodec.encode(ShotRecord.Header(metadata,limits))
        if (!send(header)) stop(last, ShotStopReason.QUEUE_LIMIT)
    }
    fun status() = ShotStatus(metadata.session, reason == null, reason, inputs.size, events, bytes, sink.queuedBytes())
    fun advance(stamp: ShotStamp) {
        if (reason != null) return
        if (stamp.micros < last.micros || stamp.tick < last.tick) { stop(last,ShotStopReason.ERROR); return }
        last = stamp
        trimRing(stamp.micros)
        if (stamp.micros >= metadata.durationSeconds * 1_000_000L) stop(stamp,ShotStopReason.DURATION_LIMIT)
    }
    fun accept(stamp: ShotStamp, event: ShotObservation): Boolean {
        advance(stamp)
        if (reason != null) return false
        if (event is ShotObservation.Input && event.token !in inputs && inputs.size >= limits.maxInputs) {
            stop(stamp,ShotStopReason.INPUT_LIMIT);return false
        }
        if (event is ShotObservation.Annotation && event.token !in inputs) return false
        if (event is ShotObservation.Input && event.token in inputs) return false
        val record = ShotRecord.Event(++sequence,stamp.micros,stamp.tick,event)
        val line = try { ShotCodec.encode(record) } catch (_: IllegalArgumentException) { stop(stamp,ShotStopReason.ERROR);return false }
        if (event is ShotObservation.Input) {
            while (ring.isNotEmpty() && reason == null) {
                val pending = ring.removeFirst()
                if (!commitReserved(pending.line,pending.event.observation)) break
            }
            if (reason != null || !commit(line)) return false
            inputs.values.filter { stamp.micros-it.at <= limits.associationMicros }.forEach { it.ambiguous=true }
            val overlapping = inputs.values.any { stamp.micros-it.at <= limits.associationMicros }
            inputs[event.token] = InputState(event,stamp.micros,ambiguous=overlapping)
            lastInput = stamp.micros
            return true
        }
        val inside = lastInput?.let { stamp.micros-it <= limits.sampleWindowMicros } == true
        val feedback = event is ShotObservation.Feedback && inputs.values.any { stamp.micros-it.at <= limits.feedbackMicros }
        val terminal = event is ShotObservation.Capsule && event.action==ShotEntityAction.REMOVED && event.token in activeCapsules
        if (inside || feedback || terminal || event is ShotObservation.Annotation || event is ShotObservation.Continuation) {
            if (!commit(line,event)) return false
        } else {
            if (!budget.tryReserve(line.size)) { stop(stamp,ShotStopReason.QUEUE_LIMIT);return false }
            ring.addLast(Pending(record,line))
        }
        when (event) {
            is ShotObservation.Capsule -> if (event.action == ShotEntityAction.DISCOVERED && seenCapsules.add(event.token)) {
                if (seenCapsules.size > 256) { stop(stamp,ShotStopReason.EVENT_LIMIT);return false }
                val candidates = inputs.values.filter { event.flying && it.input.variant == event.variant &&
                    stamp.micros-it.at in 0..limits.associationMicros && event.position.distanceSquared(it.input.pose.eye) <= limits.associationDistanceSquared }
                candidates.forEach {
                    if (it.tracks.size < 16) it.tracks.add(event.token) else { it.ambiguous=true;gaps=true }
                    if (candidates.size > 1 || event.discovery != ShotDiscovery.SPAWN) it.ambiguous=true
                }
            }
            is ShotObservation.Feedback -> if (event.feedback.kind !in setOf(ShotFeedbackKind.SHARED_CAPTURE,ShotFeedbackKind.STARTLE))
                inputs.values.filter { stamp.micros-it.at <= limits.feedbackMicros &&
                    (event.feedback.variant==null || event.feedback.variant==it.input.variant) }.forEach {
                    if (it.feedback.size == 16) { it.feedback.removeFirst();gaps=true };it.feedback.addLast(record.sequence)
                }
            is ShotObservation.Annotation -> inputs[event.token]?.let {
                if (it.annotations.size == 16) { it.annotations.removeFirst();gaps=true };it.annotations.addLast(event.outcome)
            }
            is ShotObservation.Gap -> if(event.reason!=ShotGap.SCAN_PENDING) gaps=true
            else -> {}
        }
        return reason == null
    }
    private fun trimRing(now: Long) {
        while (ring.firstOrNull()?.event?.micros?.let { now-it > limits.preInputMicros } == true)
            budget.release(ring.removeFirst().line.size)
    }
    private fun send(line: ByteArray): Boolean {
        if (!budget.tryReserve(line.size)) return false
        if (!sink.offer(line)) { budget.release(line.size);return false }
        bytes += line.size
        return true
    }
    private fun commit(line: ByteArray, observation:ShotObservation?=null): Boolean {
        // Feedback can arrive after the sample window. Flush earlier buffered evidence before any direct write.
        while (ring.isNotEmpty() && reason == null) {
            val pending=ring.removeFirst()
            if (!commitReserved(pending.line,pending.event.observation)) return false
        }
        if (reason != null) return false
        if (!budget.tryReserve(line.size)) { stop(last,ShotStopReason.QUEUE_LIMIT);return false }
        return commitReserved(line,observation)
    }
    private fun commitReserved(line: ByteArray, observation:ShotObservation?=null): Boolean {
        val stop = when {
            events >= limits.maxEvents-1 -> ShotStopReason.EVENT_LIMIT
            bytes+line.size > limits.maxBytes-limits.footerReserve -> ShotStopReason.BYTE_LIMIT
            else -> null
        }
        if (stop != null) { budget.release(line.size);stop(last,stop);return false }
        if (!sink.offer(line)) { budget.release(line.size);stop(last,ShotStopReason.QUEUE_LIMIT);return false }
        events++;bytes+=line.size
        if(observation is ShotObservation.Capsule) {
            if(observation.action==ShotEntityAction.REMOVED) activeCapsules.remove(observation.token)
            else activeCapsules[observation.token]=observation
        }
        return true
    }
    fun stop(stamp: ShotStamp, reason: ShotStopReason) {
        if (this.reason != null) return
        this.reason=reason
        while (ring.isNotEmpty()) budget.release(ring.removeFirst().line.size)
        // File finalization and track completeness are distinct. Never turn a stop into an observed outcome.
        for(capsule in activeCapsules.values) {
            val terminal=ShotRecord.Event(++sequence,maxOf(last.micros,stamp.micros),maxOf(last.tick,stamp.tick),
                capsule.copy(action=ShotEntityAction.REMOVED,removal=ShotRemoval.RECORDING_STOPPED))
            val line=ShotCodec.encode(terminal)
            if(events>=limits.maxEvents-1 || bytes+line.size>limits.maxBytes-limits.footerReserve || !send(line)) { gaps=true;break }
            events++
        }
        activeCapsules.clear()
        val summaries = inputs.values.map { state ->
            ShotSummary(state.input.token,state.input.variant,state.input.scenario,state.input.split,state.tracks.toList(),
                if (state.ambiguous || state.tracks.size > 1) ShotAssociation.AMBIGUOUS else if (state.tracks.size == 1) ShotAssociation.CANDIDATE_UNIQUE else ShotAssociation.UNMATCHED,
                state.feedback.toList(),state.annotations.toList())
        }
        val footer=ShotRecord.Footer(++sequence,maxOf(last.micros,stamp.micros),maxOf(last.tick,stamp.tick),reason,
            reason in setOf(ShotStopReason.USER_STOP,ShotStopReason.EXPORT) && !gaps,events,inputs.size,bytes,summaries,gaps)
        // A full timeline remains in events. A bounded footer may omit summaries, explicitly marked incomplete.
        var count=summaries.size
        var encoded: ByteArray?
        do {
            encoded=runCatching { ShotCodec.encode(footer.copy(shots=summaries.take(count),gaps=gaps || count<summaries.size,complete=footer.complete && count==summaries.size)) }.getOrNull()
            if (encoded == null) count /= 2
        } while (encoded == null && count > 0)
        if (encoded == null) encoded=ShotCodec.encode(footer.copy(shots=emptyList(),gaps=true,complete=false))
        sink.finish(encoded)
        bytes+=encoded.size
    }
}
