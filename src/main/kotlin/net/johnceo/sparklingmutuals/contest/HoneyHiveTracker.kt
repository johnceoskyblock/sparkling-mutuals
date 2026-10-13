package net.johnceo.sparklingmutuals.contest

/** Client-independent personal timers and ordered click/chat correlation. */
class HoneyHiveTracker(private var refillAt: MutableMap<String, Long>, private var queenFull: MutableSet<String> = mutableSetOf()) {
    private data class Click(val key: String, val at: Long, var eligible: Boolean = true)
    private val clicks = ArrayDeque<Click>()
    private var lastLoot: Click? = null
    private val changedAt = mutableMapOf<String, Long>()
    private val emptyObserved = mutableSetOf<String>()

    fun click(key: String, now: Long) {
        expire(now)
        val eligible = HoneyHiveRules.state(now, refillAt[key]) == HiveState.FULL
        val previous = clicks.firstOrNull { it.key == key }
        if (previous == null) clicks.addLast(Click(key, now, eligible))
        else if (eligible && !previous.eligible) {
            clicks.remove(previous)
            clicks.addLast(Click(key, now))
        }
    }

    fun chat(line: String, now: Long): Boolean {
        expire(now)
        val key = when {
            line.startsWith("HIVE!") -> {
                val click = clicks.firstOrNull { it.eligible } ?: return false
                clicks.remove(click)
                lastLoot = Click(click.key, now)
                refillAt[click.key] = now + HoneyHiveRules.REFILL_MILLIS
                queenFull.remove(click.key)
                click.key
            }
            line.startsWith("QUEEN BEE!") -> {
                val loot = lastLoot?.takeIf { now - it.at in 0..CLICK_WINDOW } ?: return false
                refillAt.remove(loot.key)
                queenFull.add(loot.key)
                clicks.filter { it.key == loot.key }.forEach { it.eligible = true }
                lastLoot = null
                loot.key
            }
            else -> return false
        }
        changedAt[key] = now
        return true
    }

    /** Server holograms take priority over the block, with a short grace for packets after a loot. */
    fun observe(key: String, label: String?, honeyLevel: Int, now: Long): Boolean {
        // A Queen Bee remains full until another confirmed loot, even if old packets still say empty.
        if (key in queenFull) return false
        if (changedAt[key]?.let { now - it in 0..SERVER_GRACE } == true) return false
        val previous = refillAt[key]
        val countdown = label?.let(::countdownMillis)
        when {
            label == "Click to loot!" -> { refillAt.remove(key); emptyObserved.remove(key); allowClicks(key) }
            countdown != null -> { refillAt[key] = now + countdown; emptyObserved.add(key) }
            honeyLevel == 5 -> { refillAt.remove(key); emptyObserved.remove(key); allowClicks(key) }
            honeyLevel == 0 -> {
                // A repeated empty block does not keep postponing the personal one-hour deadline.
                if (emptyObserved.add(key) && HoneyHiveRules.state(now, previous) == HiveState.FULL)
                    refillAt[key] = now + HoneyHiveRules.REFILL_MILLIS
            }
        }
        return previous != refillAt[key]
    }

    fun resetSession() { clicks.clear(); lastLoot = null; changedAt.clear(); emptyObserved.clear() }

    /** The profile join line can arrive between a click, loot and Queen Bee messages. */
    fun promote(times: MutableMap<String, Long>, full: MutableSet<String>) {
        // Confirmed loots while the profile was unknown are newer than its saved Queen Bee state.
        full.removeAll(refillAt.keys)
        if (times !== refillAt) times.putAll(refillAt)
        full.addAll(queenFull)
        full.forEach(times::remove)
        refillAt = times; queenFull = full
    }

    private fun allowClicks(key: String) { clicks.filter { it.key == key }.forEach { it.eligible = true } }

    private fun expire(now: Long) {
        while (clicks.firstOrNull()?.let { now - it.at !in 0..CLICK_WINDOW } == true) clicks.removeFirst()
    }

    companion object {
        private const val CLICK_WINDOW = 5_000L
        private const val SERVER_GRACE = 2_000L
        private val countdown = Regex("^Refill in: (?:(\\d+)m(?:\\s+)?)?(?:(\\d+)s)?$")
        fun countdownMillis(text: String): Long? {
            val match = countdown.matchEntire(text.trim()) ?: return null
            if (match.groupValues.drop(1).all { it.isEmpty() }) return null
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            return (minutes * 60_000 + seconds * 1_000).takeIf { it in 0..HoneyHiveRules.REFILL_MILLIS }
        }
    }
}
