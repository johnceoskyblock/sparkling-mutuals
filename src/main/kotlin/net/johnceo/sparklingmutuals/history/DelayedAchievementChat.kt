package net.johnceo.sparklingmutuals.history

/** Keep related chat lines together and wait for the server's catch block to finish. */
internal class DelayedAchievementChat<T>(private val delay: Long=500) {
    private val pending=ArrayDeque<T>()
    private var readyAt=0L
    fun add(messages: List<T>,now: Long) {
        pending.addAll(messages);readyAt=maxOf(readyAt,now+delay)
    }
    fun postpone(now: Long) { if(pending.isNotEmpty()) readyAt=maxOf(readyAt,now+delay) }
    fun drain(now: Long): List<T> {
        if(now<readyAt || pending.isEmpty()) return emptyList()
        return pending.toList().also { clear() }
    }
    fun clear() { pending.clear();readyAt=0 }
}
