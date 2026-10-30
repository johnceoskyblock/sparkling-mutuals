package net.johnceo.sparklingmutuals.alerts

enum class SparkleWarpMode { SOLO, DUO }

/** Pure cycle rules; runtime callers must confirm leadership before executing commands. */
object SparkleWarpPolicy {
    const val RETURN_AT = 19_000L
    const val PARTY_SIZE = 4
    fun canStart(leader: Boolean, members: Int) = leader && members in 0 until PARTY_SIZE
    fun returningPhase(startedAt: Long, now: Long) = now-startedAt >= RETURN_AT
    private val ign = Regex("[A-Za-z0-9_]{1,16}")
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

/** Leave once, wait for roster/leave confirmation, then accept exactly once. */
class LeechInvitation {
    private var inviter: String? = null
    private var expiresAt=0L
    private var leaving=false
    private var left=false
    val pending get()=inviter != null
    fun offer(name: String, now: Long) { inviter=name; expiresAt=now+15_000; leaving=false; left=false }
    fun leftParty() { if (leaving) left=true }
    fun next(now: Long, inParty: Boolean?): String? {
        val name=inviter ?: return null
        if (now >= expiresAt) { clear();return null }
        if (left || inParty == false) { clear();return "p accept $name" }
        if (inParty == true && !leaving) { leaving=true;return "p leave" }
        return null
    }
    fun clear() { inviter=null; expiresAt=0; leaving=false; left=false }
}
