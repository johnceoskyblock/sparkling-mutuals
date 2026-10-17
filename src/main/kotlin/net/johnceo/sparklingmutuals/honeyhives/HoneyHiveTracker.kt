package net.johnceo.sparklingmutuals.honeyhives

import net.johnceo.sparklingmutuals.safari.SafariRules

/** Client-independent personal timers and ordered click/chat correlation. */
class HoneyHiveTracker(private var refillAt: MutableMap<String, Long>, private var queenFull: MutableSet<String> = mutableSetOf(),
    private var provisional: Boolean = false, private var lootedAt: MutableMap<String, Long> = mutableMapOf()) {
    private val confirmedKeys = mutableSetOf<String>()
    private data class Click(val key: String, val at: Long, var eligible: Boolean = true)
    private val clicks = ArrayDeque<Click>()
    private var lastLoot: Click? = null
    private var queenBeforeLoot: Click? = null
    private val changedAt = mutableMapOf<String, Long>()
    private val emptyObserved = mutableSetOf<String>()
    private val blockEmpty = mutableSetOf<String>()
    fun state(key: String, now: Long) = if (key in blockEmpty) HiveState.EMPTY else HoneyHiveRules.state(now, refillAt[key])

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
                lootedAt[click.key] = now
                refillAt[click.key] = now + HoneyHiveRules.REFILL_MILLIS
                blockEmpty.remove(click.key)
                queenFull.remove(click.key)
                click.key
            }
            line.startsWith("QUEEN BEE!") -> {
                val loot = lastLoot?.takeIf { now - it.at in 0..CLICK_WINDOW }
                    ?: clicks.singleOrNull()?.also {
                        clicks.remove(it)
                        queenBeforeLoot = Click(it.key, now)
                    } ?: return false
                refillAt.remove(loot.key)
                blockEmpty.remove(loot.key)
                queenFull.add(loot.key)
                clicks.filter { it.key == loot.key }.forEach { it.eligible = true }
                lastLoot = null
                loot.key
            }
            else -> return false
        }
        confirmedKeys.add(key)
        changedAt[key] = now
        return true
    }

    /** Level five is full; lower levels use the matching status label when available. */
    fun observe(key: String, label: String?, honeyLevel: Int? = null, now: Long): Boolean {
        val status = label?.let(SafariRules::strip)?.replace('\u00a0', ' ')?.trim()
        val countdown = status?.let(::countdownMillis)
        val fullLabel = status?.equals("Click to loot!", true) == true
        if (honeyLevel != 0 && honeyLevel != 5 && !fullLabel && countdown == null) return false
        val previous = refillAt[key]
        val wasQueenFull = key in queenFull
        val wasBlockEmpty = key in blockEmpty
        when {
            honeyLevel == 5 || fullLabel -> {
                refillAt.remove(key); blockEmpty.remove(key); emptyObserved.remove(key); allowClicks(key)
            }
            countdown != null -> {
                queenFull.remove(key); blockEmpty.remove(key)
                refillAt[key] = now + countdown
            }
            honeyLevel == 0 -> {
                queenFull.remove(key)
                // Without a label, empty block state does not establish a new loot/deadline.
                blockEmpty.add(key)
            }
        }
        return previous != refillAt[key] || wasQueenFull != (key in queenFull) || wasBlockEmpty != (key in blockEmpty)
    }

    fun resetSession() { clicks.clear(); lastLoot = null; queenBeforeLoot = null; changedAt.clear(); emptyObserved.clear(); blockEmpty.clear() }

    /** The profile join line can arrive between a click, loot and Queen Bee messages. */
    fun promote(times: MutableMap<String, Long>, full: MutableSet<String>, loots: MutableMap<String, Long> = mutableMapOf()) {
        // Confirmed loots while the profile was unknown are newer than its saved Queen Bee state.
        val transferable = if (provisional) refillAt.filterKeys { it in confirmedKeys } else refillAt
        full.removeAll(transferable.keys)
        if (times !== refillAt) times.putAll(transferable)
        full.addAll(if (provisional) queenFull.filter { it in confirmedKeys } else queenFull)
        loots.putAll(if (provisional) lootedAt.filterKeys { it in confirmedKeys } else lootedAt)
        lootedAt = loots
        provisional = false
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
