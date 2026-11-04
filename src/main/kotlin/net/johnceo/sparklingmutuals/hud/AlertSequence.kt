package net.johnceo.sparklingmutuals.hud

/** Client-thread FIFO shared by the mod's timed alert presentations. */
internal class AlertSequence(private val audioPlaying: () -> Boolean = { false }) {
    private data class Request(val owner: Any, val key: Any?, val duration: Long,
        val minimumTicks: Int, val valid: () -> Boolean, val start: () -> Unit, val finish: () -> Unit)
    private val pending = ArrayDeque<Request>()
    private var active: Request? = null
    private var endsAt = 0L
    private var ticksRemaining = 0
    val busy get() = active != null || audioPlaying()

    fun submit(owner: Any, duration: Long, key: Any? = null, valid: () -> Boolean = { true },
               finish: () -> Unit = {}, minimumTicks: Int = 0, start: () -> Unit) {
        if (key != null && (active?.let { it.owner == owner && it.key == key } == true ||
                pending.any { it.owner == owner && it.key == key })) return
        pending.addLast(Request(owner, key, duration, minimumTicks, valid, start, finish))
    }

    fun tick(now: Long) {
        active?.let {
            if (ticksRemaining > 0) ticksRemaining--
            if ((now >= endsAt && ticksRemaining == 0) || (it.minimumTicks == 0 && !it.valid())) finishActive()
        }
        while (active == null && pending.isNotEmpty() && !audioPlaying()) {
            val request = pending.removeFirst()
            if (!request.valid()) continue
            active = request
            endsAt = now + request.duration.coerceAtLeast(1)
            ticksRemaining = request.minimumTicks
            request.start()
        }
    }

    fun clear(owner: Any, keyMatches: (Any?) -> Boolean = { true }) {
        pending.removeAll { it.owner == owner && keyMatches(it.key) }
        if (active?.let { it.owner == owner && keyMatches(it.key) } == true) finishActive()
    }

    fun reset() { pending.clear(); finishActive() }

    private fun finishActive() {
        val request = active
        active = null
        request?.finish?.invoke()
    }
}

internal object SafariAlertSequence {
    val queue = AlertSequence {
        net.johnceo.sparklingmutuals.ui.AlertSounds.bannerAudioPlaying() ||
            CatchCelebrationAudio.playing ||
            net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters.audioPlaying()
    }
    fun tick() = queue.tick(System.nanoTime() / 1_000_000)
}
