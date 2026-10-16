package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft

/**
 * `!dt [reason]` in party chat: someone wants a break after this run. The request waits until the run ends, then the
 * requester's own client posts "X has requested downtime" (one client, not every member's) and every client holds the
 * Safari Manager shut until the requester says `r`, earns their contest ticket, or personally announces Ticket earned!. Asked outside the Safari,
 * it takes effect straight away. Requests leave with their requester: someone leaving or being removed from the party
 * drops theirs, and this client leaving, joining another party or the party disbanding drops them all. Turning the
 * setting off drops everything too.
 */
object SafariDowntime {
    private val request = Regex("""^!dt(?:\s+(.+))?$""", RegexOption.IGNORE_CASE)
    private val resume = Regex("""^r$""", RegexOption.IGNORE_CASE)
    private const val NAME = "(?:\\[[^]]+]\\s*)*([A-Za-z0-9_]{1,16})"
    /** One member gone: their request goes with them. */
    private val departed = listOf(
        Regex("^$NAME (?:has left the party|has been removed from the party)[.!]$"),
        Regex("^$NAME was removed from your party because they disconnected[.!]?$"),
        Regex("^Kicked $NAME because they were offline[.!]?$"))
    /** This client is in a different party (or none) now: every request belonged to the old one. */
    private val partyGone = listOf(
        Regex("^You (?:left the party|have been kicked from the party(?: by .+)?)[.!]?$"),
        Regex("^You have joined .+'s party!$"),
        Regex("^(?:The party was disbanded|$NAME has disbanded the party)(?:[.!]| because .+)$"),
        Regex("^You are not (?:currently )?in a party[.!]?$"))
    /** Requester -> reason (may be empty), in the order they asked. */
    private val requests = linkedMapOf<String, String>()
    private var active = false
    private var announcementDue = false
    /** A hold only stands while the setting is on, so switching it off releases the manager at once. */
    val holding get() = active && ConfigManager.downtimeCommand
    /** Torrhus entry must see banked requests synchronously, even before the next run-exit tick. */
    val entryHold get() = requests.isNotEmpty() && ConfigManager.downtimeCommand

    /** Handles one chat line; returns a party message this client should send, if any. */
    fun chat(text: String, inSafari: Boolean, self: String): String? {
        if (!ConfigManager.downtimeCommand) { clear(); return null }
        if (partyChanged(SafariRules.strip(text).trim())) return null
        val chat = PartyChat.parse(text) ?: return null
        request.matchEntire(chat.body)?.let { match ->
            val reason = match.groupValues[1].trim()
            val existing = requests.keys.firstOrNull { it.equals(chat.sender, true) }
            requests[existing ?: chat.sender] = reason
            tell("[SM] ${chat.sender} wants downtime after this run" + if (reason.isEmpty()) "." else ": $reason")
            return if (!inSafari) leftSafari(self) else null
        }
        if (chat.body.equals("Ticket earned!", ignoreCase = true)) {
            ready(chat.sender)
            return null
        }
        if (resume.matches(chat.body)) {
            val key = requests.keys.firstOrNull { it.equals(chat.sender, true) } ?: return null
            requests.remove(key)
            val wasActive = active
            if (requests.isEmpty()) active = false
            tell("[SM] ${chat.sender} is back" + if (!wasActive) ". Downtime request cancelled."
                else if (active) "; still waiting on ${holders()}." else ". Safari Manager released.")
        }
        return null
    }

    /** Party readiness is sender-specific for every reason; local scoreboard still calls ticketEarned. */
    fun ready(player: String): Boolean {
        val key = requests.keys.firstOrNull { it.equals(player, true) } ?: return false
        requests.remove(key)
        if (requests.isEmpty()) active = false
        tell("[SM] $key is ready." + if (active) " Still waiting on ${holders()}." else " Downtime cleared.")
        return true
    }
    /** A connection transfer invalidates run context, not the same party's requests. */
    fun transfer() {
        if (!active && requests.isNotEmpty()) { active = true; announcementDue = true }
    }

    /** A ticket releases only this player's contest request, pending or already blocking entry. */
    fun ticketEarned(player: String): Boolean {
        val key = requests.entries.firstOrNull { it.key.equals(player, true) && it.value.equals("contest", true) }?.key
            ?: return false
        requests.remove(key)
        if (requests.isEmpty()) active = false
        tell("[SM] $key earned a ticket. " + if (active) "Still waiting on ${holders()}." else "Contest downtime cleared.")
        return true
    }

    fun partyResetSignal(line: String) = partyGone.any { it.matches(SafariRules.strip(line).trim()) }

    /** Party membership notices. Returns whether the line was one, so it isn't read as chat. */
    private fun partyChanged(line: String): Boolean {
        if (partyGone.any { it.matches(line) }) {
            if (requests.isNotEmpty()) tell("[SM] Party changed: downtime requests dropped" + if (active) ". Safari Manager released." else ".")
            clear()
            return true
        }
        val name = departed.firstNotNullOfOrNull { it.matchEntire(line) }?.groupValues?.get(1) ?: return false
        val key = requests.keys.firstOrNull { it.equals(name, true) } ?: return true
        requests.remove(key)
        val released = active && requests.isEmpty()
        if (requests.isEmpty()) active = false
        tell("[SM] $key left the party: their downtime request is dropped" +
            if (released) ". Safari Manager released." else if (active) "; still waiting on ${holders()}." else ".")
        return true
    }

    /** The run is over: banked requests become a hold. Returns the requester's own announcement, if this is them. */
    fun leftSafari(self: String): String? {
        if (!ConfigManager.downtimeCommand) { clear(); return null }
        if (requests.isEmpty()) return null
        if (!active) {
            active = true; announcementDue = true
            tell("[SM] Safari Manager held for downtime. Say r in party chat when ready.")
        }
        if (!announcementDue || self.isBlank()) return null
        announcementDue = false
        val mine = requests.entries.firstOrNull { it.key.equals(self, true) } ?: return null
        return "${mine.key} has requested downtime" + if (mine.value.isEmpty()) "" else " for reason: ${mine.value}"
    }

    fun onClientTick(client: Minecraft) {
        if (!ConfigManager.downtimeCommand && (active || requests.isNotEmpty())) clear()
        if (client.player == null || client.connection == null || !net.johnceo.sparklingmutuals.party.PartyManager.hasInfo) return
        val inSafari = SafariManagerGuard.activeRun(client)
        if (!inSafari) leftSafari(client.player?.name?.string.orEmpty())?.let(::send)
    }

    /** Remove only identities proven absent from the new known party, not people merely outside render range. */
    fun pruneVerified(ids: Map<String, String>, members: Set<String>) {
        requests.keys.removeAll { name -> ids[name.lowercase()]?.let { it !in members } == true }
        if (requests.isEmpty()) active = false
    }

    fun holders() = requests.keys.joinToString(", ")
    /** Explicit disable or confirmed party change drops requests. */
    fun clear() { active = false; announcementDue = false; requests.clear() }
    fun reset() { clear() }

    private fun send(message: String) { Minecraft.getInstance().player?.connection?.sendCommand("pc $message") }
    private fun tell(text: String) { Minecraft.getInstance()?.player?.sendSystemMessage(LocalChat.component(text)) }
}
