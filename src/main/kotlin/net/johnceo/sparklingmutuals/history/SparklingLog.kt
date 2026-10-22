package net.johnceo.sparklingmutuals.history

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.history.importing.SparklingHistoryMerge
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.minecraft.client.Minecraft

/**
 * Every sparkling caught while this client was watching (yours and party members' announced in chat), with the
 * tickets the party used since the previous one. Saved to `sparkling-mutuals-sparklings.json`.
 */
object SparklingLog {
    class Data(var entries: MutableList<SparklingEntry> = mutableListOf(), var ticketsSinceSparkling: Int = 0)
    private var data = Data()
    val entries: List<SparklingEntry> get() = data.entries
    /** Tickets the party has used since anyone's last sparkling. */
    val ticketsSinceSparkling get() = data.ticketsSinceSparkling

    fun register() {
        data = HistoryStore.load("sparklings", Data::class.java, ::Data).also { d -> d.entries.sortByDescending { it.at } }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { SafariMessages.lines(message.string).forEach(::chat) }
        }
    }

    private fun chat(text: String) {
        val match = RunTally.sparkling.matchEntire(text) ?: return
        val player = match.groupValues[1]
        val critter = SafariRoster.named(match.groupValues[2].trim())?.name ?: match.groupValues[2].trim()
        val wasKnown = net.johnceo.sparklingmutuals.safari.SafariSparklingMode.caught(player, critter)
        if (!HistoryServerPolicy.allows()) return
        val entry = SparklingEntry(System.currentTimeMillis(), player, critter, tickets = data.ticketsSinceSparkling)
        data.ticketsSinceSparkling = 0
        data.entries.add(0, entry)
        save()
        val client = Minecraft.getInstance()
        val own = player.equals(client.player?.name?.string, true)
        Achievements.onSparkling(entry, own)
        markUnique(entry, own, wasKnown)
    }

    /** Compare the pre-catch party snapshot; never fetch a delayed API response to verify a live catch. */
    private fun markUnique(entry: SparklingEntry, own: Boolean, wasKnown: Boolean?) {
        val earlier = data.entries.any { it !== entry && it.player.equals(entry.player, true) && it.critter.equals(entry.critter, true) }
        if (!earlier && wasKnown == false) {
            entry.unique = true; save(); Achievements.onUnique(own)
        }
    }

    fun onTicket() { if (HistoryServerPolicy.allows()) { data.ticketsSinceSparkling++; save() } }

    /** Adds catches found elsewhere (old logs), skipping any already here. Returns how many were added. */
    fun importEntries(found: List<SparklingEntry>, ticketTimes: List<Long> = emptyList()): Int {
        val added = SparklingHistoryMerge.merge(data.entries, found, ticketTimes)
        if (added > 0) save().getOrThrow()
        return added
    }

    /** Which catch of this critter an entry was, counting everyone's. */
    fun numberOf(entry: SparklingEntry) = data.entries.count { it.critter.equals(entry.critter, true) && it.at <= entry.at }

    fun save() = HistoryStore.save("sparklings", data)
}
