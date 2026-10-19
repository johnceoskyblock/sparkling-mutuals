package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.completion.SparklingChecks

/** Bounded receipt-time owner binding; UUID membership is rechecked at delivery. */
class StartingResourceInbox {
    companion object {
        fun canonicalUuid(raw: String): String? {
            val value = raw.replace("-", "").lowercase()
            if (!value.matches(Regex("[0-9a-f]{32}"))) return null
            return "${value.take(8)}-${value.substring(8,12)}-${value.substring(12,16)}-${value.substring(16,20)}-${value.substring(20)}"
        }
    }
    private data class Pending(val owner: Any, val chat: PartyChat, val raw: String, val at: Long)
    private val pending = linkedMapOf<String, Pending>()
    private val recent = linkedMapOf<String, Long>()
    private var recentOwner: Any? = null
    val names get() = pending.values.map { it.chat.sender.lowercase() }.toSet()
    fun clear() { pending.clear(); recent.clear(); recentOwner = null }
    fun add(owner: Any, raw: String, now: Long): Boolean {
        if (raw.length > 1024) return false
        val chat = PartyChat.parse(raw) ?: return false
        val report = StartingItemReport.parse(chat.body)
        if (report != null && report.scope == null) return false
        if (report == null && StartingResourceNotice.parse(chat.body, PartySparklingState()) == null &&
            SparklingChecks.doneBiome(raw) == null) return false
        if (recentOwner !== owner) { clear(); recentOwner = owner }
        recent.entries.removeIf { now - it.value > 1000 }
        val key = chat.sender.lowercase() + ":" + chat.body
        if (key in recent) return true
        if (recent.size >= 128) recent.remove(recent.keys.first())
        recent[key] = now
        if (pending.size >= 64 && key !in pending) pending.remove(pending.keys.first())
        pending.putIfAbsent(key, Pending(owner, chat, raw, now))
        return true
    }
    fun drain(owner: Any, verifiedNames: Map<String, String>, roster: Set<String>?, now: Long,
        acceptsScope: (String) -> Boolean, accept: (String, PartyChat) -> Unit) {
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            val (_, item) = iterator.next()
            if (item.owner !== owner || now - item.at !in 0..60000) { iterator.remove(); continue }
            if (roster == null) continue
            val id = verifiedNames[item.chat.sender.lowercase()] ?: continue
            if (id !in roster) { iterator.remove(); continue }
            val report = StartingItemReport.parse(item.chat.body)
            if (report?.scope != null && !acceptsScope(report.scope)) continue
            iterator.remove()
            accept(id, item.chat)
        }
    }
}
