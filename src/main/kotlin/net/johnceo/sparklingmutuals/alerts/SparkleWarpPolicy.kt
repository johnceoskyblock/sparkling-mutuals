package net.johnceo.sparklingmutuals.alerts

enum class SparkleWarpMode { SOLO, DUO }

/** Pure cycle rules; runtime callers confirm leadership before arming a cycle. */
object SparkleWarpPolicy {
    const val RETURN_AT = 24_000L
    const val DEFAULT_DELAY_SECONDS = 28
    const val PARTY_SIZE = 4
    fun canStart(leader: Boolean, members: Int) = leader && members in 0 until PARTY_SIZE
    fun returningPhase(startedAt: Long, now: Long) = now-startedAt >= RETURN_AT
    fun warpDeadline(startedAt: Long, delaySeconds: Int) = startedAt + delaySeconds * 1000L
    fun requirementRoster(actual: Set<String>, returning: Set<String>): Set<String> {
        fun id(value: String)=value.replace("-","").lowercase()
        val present=actual.map(::id).toSet()
        return actual + returning.filter { id(it) !in present }.take((PARTY_SIZE-actual.size).coerceAtLeast(0))
    }
    fun contestPause(enabled: Boolean, leech: Boolean, leader: Boolean?, ticketEarned: Boolean) =
        enabled && !leech && leader != false && !ticketEarned
    private val ign = Regex("[A-Za-z0-9_]{1,16}")
    fun inviteAllowed(name: String, whitelist: String) = ign.matches(name) && WarpList.parse(whitelist).any { it.equals(name,true) }
    fun returningCommand(names: List<String>, present: Set<String>, memberCount: Int): String? {
        val members=present.map(String::lowercase).toSet()
        val slots=(PARTY_SIZE-maxOf(memberCount,members.size)).coerceAtLeast(0)
        val missing=names.filter { ign.matches(it) && it.lowercase() !in members }.distinctBy(String::lowercase).take(slots)
        return missing.takeIf { it.isNotEmpty() }?.joinToString(" ",prefix="p ")
    }
    data class Cleanup(val leechers: List<String>, val commands: List<String>)
    fun cleanup(mode: SparkleWarpMode, self: String, duo: String, members: List<String>): Cleanup? {
        if (mode == SparkleWarpMode.DUO && (!ign.matches(duo) || duo.equals(self,true) || members.none { it.equals(duo,true) })) return null
        val others=members.filter { ign.matches(it) && !it.equals(self,true) }.distinctBy(String::lowercase)
        val leechers=others.filter { mode != SparkleWarpMode.DUO || !it.equals(duo,true) }
        return Cleanup(leechers, if (mode == SparkleWarpMode.SOLO) {
            if (others.isEmpty()) emptyList() else listOf("p disband")
        } else leechers.map { "p kick $it" })
    }
    fun inviter(raw: String): String? = net.johnceo.sparklingmutuals.safari.SafariMessages.lines(raw)
        .firstNotNullOfOrNull { invite.matchEntire(it.trim())?.groupValues?.get(1) }
    private val invite=Regex("^(?:\\[[^]\\r\\n]+]\\s*)*([A-Za-z0-9_]{1,16}) has invited you to join their party!$")
}

/** Names from a server party list, validated against the confirmed roster size and local leader. */
internal class StartPartyRoster {
    private val header = Regex("^Party Members \\((\\d+)\\)$")
    private val rank = Regex("\\[[^]]+]")
    private var count = 0
    private var leader: String? = null
    private val players = linkedMapOf<String, String>()

    fun observe(raw: String) {
        val line = raw.trim()
        header.matchEntire(line)?.let {
            clear()
            count = it.groupValues[1].toIntOrNull() ?: 0
            return
        }
        if (count == 0) return
        val role = line.substringBefore(':')
        if (role !in setOf("Party Leader", "Party Moderators", "Party Members")) return
        val names = WarpList.parse(rank.replace(line.substringAfter(':', ""), " "))
        if (role == "Party Leader") leader = names.singleOrNull()
        names.forEach { players[it.lowercase()] = it }
    }

    fun members(self: String, rosterSize: Int): List<String>? =
        players.values.toList().takeIf { count == rosterSize && it.size == count && leader.equals(self, true) }

    fun clear() { count = 0; leader = null; players.clear() }
}

/** Retry the returning batch once, using the latest roster and available slots. */
internal class ReturningPartyInvites {
    private var attempts = 0
    private var retryAt = 0L

    fun next(now: Long, deadline: Long, names: List<String>, present: Set<String>, memberCount: Int): String? {
        if (now >= deadline || attempts >= 2 || now < retryAt) return null
        val command = SparkleWarpPolicy.returningCommand(names, present, memberCount) ?: return null
        attempts++
        retryAt = now + 2_000
        return command
    }

    fun reset() {
        attempts = 0
        retryAt = 0
    }
}

/** Leave once, wait for roster/leave confirmation, then accept exactly once. */
class LeechInvitation {
    private var inviter: String? = null
    private var expiresAt=0L
    private var leaving=false
    private var left=false
    private var acceptAfter=0L
    val pending get()=inviter != null
    fun offer(name: String, now: Long) { inviter=name; expiresAt=now+15_000 }
    fun leftParty() { if (leaving) left=true }
    fun next(now: Long, inParty: Boolean?, protectedLeader: Boolean? = false): String? {
        val name=inviter ?: return null
        if (now >= expiresAt) { clear();return null }
        if (inParty == true && protectedLeader == true) { clear();return null }
        if (now >= acceptAfter && (inParty == false || left && inParty == null)) {
            clear()
            return "p accept $name"
        }
        if (inParty == true && protectedLeader == false && !leaving) {
            leaving=true
            acceptAfter=now+50
            return "p leave"
        }
        return null
    }
    fun clear() { inviter=null; expiresAt=0; leaving=false; left=false;acceptAfter=0 }
}
