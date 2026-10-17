package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft

/**
 * `!dt [reason]` in party chat: someone wants a break after this run. The request waits until the run ends, then the
 * clients coordinate one combined party announcement naming every requester and reason, and every client holds the
 * Safari Manager shut until the requester says `r`, earns their contest ticket, or announces Ticket earned!. Asked outside the Safari,
 * it takes effect straight away. Requests leave with their requester: someone leaving or being removed from the party
 * drops theirs, and this client leaving, joining another party or the party disbanding drops them all. Turning the
 * setting off drops everything too.
 */
object SafariDowntime {
    private val request = Regex("""^!dt(?:\s+(.+))?$""", RegexOption.IGNORE_CASE)
    private val resume = Regex("""^(?:(?:ok|okay|yes|yep|yeah) )?(?:(?:i'm|im|i am|we're|we are|all|everyone is|everyone's) )?(?:all |finally )?(?:r|rdy|ready)(?: now| to go| when you are| whenever you are| whenever| let's go| lets go)?$""")
    private val readyTypo = Regex("^(?:r+|[etdfg]r+|r+[etdfg])$")
    fun readyMessage(body: String): Boolean {
        if ('?' in body) return false
        val raw = SafariRules.strip(body).lowercase().trim().trim { !it.isLetterOrDigit() }
        // Only short r-spam and one adjacent keyboard key; never arbitrary words containing r.
        if (raw.length <= 6 && readyTypo.matches(raw)) return true
        val words = SafariRules.strip(body).lowercase().replace('’', '\'')
            .replace(Regex("[.,!]+"), " ").trim().replace(Regex("\\s+"), " ")
        return resume.matches(words)
    }
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
    private val announcement = DowntimeAnnouncement()
    /** Torrhus entry checks banked requests before the next run-exit tick. */
    val entryHold get() = requests.isNotEmpty() && ConfigManager.downtimeCommand
    fun ticketMessage(body: String) = body.contains("ticket", ignoreCase = true)
    fun observeAnnouncement(body: String) { announcement.observe(body) }

    /** Handles one chat line; returns a party message this client should send, if any. */
    fun chat(text: String, inSafari: Boolean, self: String, contestMembers: Collection<String> = emptyList()): String? {
        if (!ConfigManager.downtimeCommand) { clear(); return null }
        if (partyChanged(SafariRules.strip(text).trim())) return null
        val chat = PartyChat.parse(text) ?: return null
        announcement.observe(chat.body)
        request.matchEntire(chat.body)?.let { match ->
            val reason = match.groupValues[1].trim()
            if (requests.isEmpty()) announcement.clear()
            // A leader's contest request asks the whole current party to get ready.
            if (reason.equals("contest", true)) contestMembers.forEach { member ->
                if (requests.keys.none { it.equals(member, true) }) requests[member] = "contest"
            }
            val existing = requests.keys.firstOrNull { it.equals(chat.sender, true) }
            requests[existing ?: chat.sender] = reason
            tell("[SM] ${chat.sender} requested downtime" + (if (reason.isEmpty()) "" else ": $reason") + if (inSafari) " (after this run)." else ".")
            return if (!inSafari) {
                if (active) { tell(localNotice()); summary() } else leftSafari(self)
            } else null
        }
        if (ticketMessage(chat.body)) {
            ready(chat.sender)
            return null
        }
        if (readyMessage(chat.body)) {
            val key = requests.keys.firstOrNull { it.equals(chat.sender, true) } ?: return null
            requests.remove(key)
            val wasActive = active
            if (requests.isEmpty()) active = false
            tell("[SM] ${chat.sender} is ready." + if (active) " Waiting for ${holders()}." else if (wasActive) " Downtime cleared." else " Request cancelled.")
        }
        return null
    }

    /** Party readiness is sender-specific for every reason; local scoreboard still calls ticketEarned. */
    fun ready(player: String): Boolean {
        val key = requests.keys.firstOrNull { it.equals(player, true) } ?: return false
        requests.remove(key)
        if (requests.isEmpty()) active = false
        tell("[SM] $key is ready." + if (active) " Waiting for ${holders()}." else " Downtime cleared.")
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
        tell("[SM] $key earned a ticket. " + if (active) "Waiting for ${holders()}." else "Downtime cleared.")
        return true
    }

    fun partyResetSignal(line: String) = partyGone.any { it.matches(SafariRules.strip(line).trim()) }

    /** Party membership notices. Returns whether the line was one, so it isn't read as chat. */
    private fun partyChanged(line: String): Boolean {
        if (partyGone.any { it.matches(line) }) {
            if (requests.isNotEmpty()) tell("[SM] Party changed. Downtime cleared.")
            clear()
            return true
        }
        val name = departed.firstNotNullOfOrNull { it.matchEntire(line) }?.groupValues?.get(1) ?: return false
        val key = requests.keys.firstOrNull { it.equals(name, true) } ?: return true
        requests.remove(key)
        val released = active && requests.isEmpty()
        if (requests.isEmpty()) active = false
        tell("[SM] $key left. Downtime request removed." +
            if (released) " Downtime cleared." else if (active) " Waiting for ${holders()}." else "")
        return true
    }

    /** The run is over: banked requests become a hold and any client can report the whole list. */
    @Suppress("UNUSED_PARAMETER")
    fun leftSafari(self: String): String? {
        if (!ConfigManager.downtimeCommand) { clear(); return null }
        if (requests.isEmpty()) return null
        if (!active) {
            active = true; announcementDue = true
            tell(localNotice())
        }
        if (!announcementDue) return null
        announcementDue = false
        return summary()
    }

    fun onClientTick(client: Minecraft) {
        if (!ConfigManager.downtimeCommand && (active || requests.isNotEmpty())) clear()
        if (client.player == null || client.connection == null || !PartyManager.hasInfo) return
        val inSafari = SafariManagerGuard.activeRun(client)
        if (!inSafari) leftSafari(client.player?.name?.string.orEmpty())?.let(::queueAnnouncement)
        val inCanyon = !inSafari && client.player != null && client.level != null &&
            (net.johnceo.sparklingmutuals.safari.SafariIsland.currentMode.equals("foraging_3", true) ||
                SkyblockSidebar.areaName(client).equals("Torrhus Canyon", true))
        // Transfers may briefly clear Safari detection before the destination is available.
        if (inCanyon || !holding)
            announcement.poll(System.currentTimeMillis(), if (holding) summary() else null)?.let(::send)
    }

    private fun displayName(key: String) = if (key.matches(Regex("[0-9a-f]{32}")))
        SafariPartyInput.nameOf(key) ?: "name loading" else key
    internal fun localNotice() = "[SM] Downtime: wait for ${holders()} to say r or report a ticket in party chat. Hold Shift to enter anyway."
    private fun summary() = DowntimeAnnouncement.format(requests.entries.associate { displayName(it.key) to it.value })
    fun queueAnnouncement(message: String) {
        val client = Minecraft.getInstance()
        val local = client.player?.uuid?.toString() ?: return
        val rank = PartyManager.withLocal(local)?.sortedDescending()?.indexOf(local)?.coerceAtLeast(0) ?: 0
        announcement.queue(message, System.currentTimeMillis(), rank)
    }

    /** Prune only members proven absent from the refreshed party, not the current instance. */
    fun pruneVerified(ids: Map<String, String>, members: Set<String>) {
        // UUID placeholders keep unknown names blocking until the roster lookup resolves them.
        requests.keys.toList().forEach { key ->
            val name = ids.entries.firstOrNull { it.value == key }?.key
            if (name != null) {
                val reason = requests.remove(key) ?: return@forEach
                if (requests.keys.none { it.equals(name, true) }) requests[SafariPartyInput.nameOf(key) ?: name] = reason
            }
        }
        requests.keys.removeAll { name ->
            val id = ids[name.lowercase()] ?: name.takeIf { it.matches(Regex("[0-9a-f]{32}")) }
            id != null && id !in members
        }
        if (requests.isEmpty()) active = false
    }

    fun holders() = requests.keys.joinToString(", ", transform = ::displayName)
    /** Explicit disable or confirmed party change drops requests. */
    fun clear() { active = false; announcementDue = false; requests.clear(); announcement.clear() }
    fun reset() { clear() }

    private fun send(message: String) { Minecraft.getInstance().player?.connection?.sendCommand("pc $message") }
    private fun tell(text: String) { Minecraft.getInstance()?.player?.sendSystemMessage(LocalChat.component(text)) }
}
