package net.johnceo.sparklingmutuals.honeyhives

import net.johnceo.sparklingmutuals.safari.SafariRules

/** Client-independent personal timers and ordered click/chat correlation. */
class HoneyHiveTracker(private var refillAt: MutableMap<String, Long>, private var queenFull: MutableSet<String> = mutableSetOf(),
    private var provisional: Boolean = false, private var lootedAt: MutableMap<String, Long> = mutableMapOf(),
    private var observedRefills: MutableMap<String, Long> = mutableMapOf()) {
    private val confirmedKeys = mutableSetOf<String>()
    private data class Click(val key: String, val at: Long, var eligible: Boolean = true)
    private val clicks = ArrayDeque<Click>()
    private var lastLoot: Click? = null
    private var queenBeforeLoot: Click? = null
    private fun lootDeadline(key: String) = lootedAt[key]?.takeIf { it in 0..Long.MAX_VALUE - HoneyHiveRules.REFILL_MILLIS }
        ?.plus(HoneyHiveRules.REFILL_MILLIS)
    fun state(key: String, now: Long) = HoneyHiveRules.state(now,
        if (key !in queenFull) lootDeadline(key) ?: refillAt[key] else refillAt[key])

    fun click(key: String, now: Long) {
        expire(now)
        // A fresh interaction starts a new loot, even after a Queen-first response.
        queenBeforeLoot = null
        val eligible = state(key, now) == HiveState.FULL
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
                val queen = queenBeforeLoot?.takeIf { now - it.at in 0..CLICK_WINDOW }
                if (queen != null) {
                    queenBeforeLoot = null
                    lootedAt[queen.key] = now
                    return true // This response belongs to the loot whose refill arrived first.
                }
                val click = clicks.firstOrNull { it.eligible } ?: return false
                clicks.remove(click)
                lastLoot = Click(click.key, now)
                observedRefills.remove(click.key)
                lootedAt[click.key] = now
                refillAt[click.key] = now + HoneyHiveRules.REFILL_MILLIS
                queenFull.remove(click.key)
                click.key
            }
            line.startsWith("QUEEN BEE!") -> {
                val loot = lastLoot?.takeIf { now - it.at in 0..CLICK_WINDOW }
                    ?: clicks.singleOrNull()?.also {
                        clicks.remove(it)
                        queenBeforeLoot = Click(it.key, now)
                    } ?: return false
                observedRefills.remove(loot.key)
                refillAt.remove(loot.key)
                queenFull.add(loot.key)
                clicks.filter { it.key == loot.key }.forEach { it.eligible = true }
                lastLoot = null
                loot.key
            }
            else -> return false
        }
        confirmedKeys.add(key)
        return true
    }

    /** A recorded loot owns its entire hour; labels may confirm readiness only after that hour. */
    @Suppress("UNUSED_PARAMETER")
    fun observe(key: String, label: String?, honeyLevel: Int? = null, now: Long): Boolean {
        val status = label?.let(SafariRules::strip)?.replace('\u00a0', ' ')?.trim() ?: return false
        val countdown = countdownMillis(status)
        val exact = lootDeadline(key)
        if (key in queenFull) return false // Only a new confirmed loot can consume a confirmed instant refill.
        if (exact != null && (now < exact || countdown != null)) return false
        if (exact == null && status.equals("Click to loot!", true) && refillAt[key]?.let { now < it } == true) return false
        val previous = refillAt[key]
        val fullBefore = key in queenFull
        when {
            status.equals("Click to loot!", true) -> {
                refillAt.remove(key)
                clicks.filter { it.key == key }.forEach { it.eligible = true }
            }
            countdown != null -> {
                refillAt[key] = now + countdown; observedRefills[key] = now + countdown
                queenFull.remove(key)
            }
            else -> return false
        }
        return previous != refillAt[key] || fullBefore != (key in queenFull)
    }

    fun resetSession() { clicks.clear(); lastLoot = null; queenBeforeLoot = null; }

    /** The profile join line can arrive between a click, loot and Queen Bee messages. */
    fun promote(times: MutableMap<String, Long>, full: MutableSet<String>, loots: MutableMap<String, Long> = mutableMapOf(), observations: MutableMap<String, Long> = mutableMapOf()) {
        // Confirmed loots while the profile was unknown are newer than its saved Queen Bee state.
        val transferable = if (provisional) refillAt.filterKeys { it in confirmedKeys } else refillAt
        full.removeAll(transferable.keys)
        if (times !== refillAt) times.putAll(transferable)
        full.addAll(if (provisional) queenFull.filter { it in confirmedKeys } else queenFull)
        loots.putAll(if (provisional) lootedAt.filterKeys { it in confirmedKeys } else lootedAt)
        if (!provisional) observations.putAll(observedRefills)
        observedRefills = observations
        lootedAt = loots
        provisional = false
        full.forEach(times::remove)
        refillAt = times; queenFull = full
    }


    private fun expire(now: Long) {
        while (clicks.firstOrNull()?.let { now - it.at !in 0..CLICK_WINDOW } == true) clicks.removeFirst()
    }

    companion object {
        private const val CLICK_WINDOW = 5_000L
        private const val SERVER_GRACE = 2_000L
        private val countdown = Regex("(?i)^Refill in: (?:(\\d+)m(?:\\s+)?)?(?:(\\d+)s)?$")
        fun countdownMillis(text: String): Long? {
            val match = countdown.matchEntire(SafariRules.strip(text).replace('\u00a0', ' ').trim()) ?: return null
            if (match.groupValues.drop(1).all { it.isEmpty() }) return null
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            return (minutes * 60_000 + seconds * 1_000).takeIf { it in 0..HoneyHiveRules.REFILL_MILLIS }
        }
    }
}
