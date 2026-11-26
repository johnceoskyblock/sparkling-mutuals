package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.config.ConfigManager
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
    class Data(var entries: MutableList<SparklingEntry> = mutableListOf(), var ticketsSinceSparkling: Int = 0,
        var legacyOwnerUuid: String? = null, var ticketCounts: MutableMap<String, Int> = mutableMapOf())
    private var data = Data()
    var revision=0L
        private set
    private var accountEntries: Pair<String, List<SparklingEntry>>? = null
    private fun belongs(entry: SparklingEntry, owner: String) =
        (entry.ownerUuid ?: data.legacyOwnerUuid ?: owner) == owner
    val entries: List<SparklingEntry> get() {
        val owner = ConfigManager.historyOwnerUuid
        return accountEntries?.takeIf { it.first == owner }?.second ?: data.entries.filter { belongs(it, owner) }
            .also { accountEntries = owner to it }
    }
    /** Tickets the party has used since anyone's last sparkling. */
    val ticketsSinceSparkling get() = data.ticketCounts[ConfigManager.historyOwnerUuid]
        ?: if (data.legacyOwnerUuid == null || data.legacyOwnerUuid == ConfigManager.historyOwnerUuid) data.ticketsSinceSparkling else 0

    internal fun claimLegacy(owner: String) {
        if (data.legacyOwnerUuid != null) return
        data.legacyOwnerUuid = owner
        if (data.entries.isNotEmpty() || data.ticketsSinceSparkling > 0) save()
    }

    fun register() {
        data = HistoryStore.load("sparklings", Data::class.java, ::Data).also { d -> d.entries.sortByDescending { it.at } }
        accountEntries = null
        revision++
        if (data.entries.groupBy { it.ownerUuid ?: data.legacyOwnerUuid }.values.map { SparklingUniqueness.reconcile(it) }.any { it }) {
            runCatching { HistoryStore.backup("sparklings", "pre-unique-repair") }
                .onSuccess { save() }
                .onFailure { org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/history").warn("Couldn't back up unique history repair", it) }
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { SafariMessages.lines(message.string).forEach(::chat) }
        }
    }

    private fun chat(text: String) {
        val match = RunTally.sparkling.matchEntire(text) ?: return
        Achievements.sparklingChatReceived()
        val player = match.groupValues[1]
        val critter = SafariRoster.named(match.groupValues[2].trim())?.name ?: match.groupValues[2].trim()
        val wasKnown = net.johnceo.sparklingmutuals.safari.SafariSparklingMode.caught(player, critter)
        val entry = SparklingEntry(System.currentTimeMillis(), player, critter, tickets = ticketsSinceSparkling)
        val unique = SparklingUniqueness.isNew(entry, entries, wasKnown)
        entry.unique=unique
        net.johnceo.sparklingmutuals.hud.SpecialCatchAlert.caught(player,critter,unique)
        val client = Minecraft.getInstance()
        val owner = HistoryAccountId.selected(client) ?: return
        entry.ownerUuid = owner
        val own = player.equals(client.user.name, true)
        entry.personal = own
        if (!HistoryServerPolicy.allows()) return
        data.ticketCounts[owner] = 0
        data.entries.add(0, entry)
        save()
        Achievements.onSparkling(entry, own)
        markUnique(entry, own, wasKnown)
        if (net.johnceo.sparklingmutuals.config.ConfigManager.sparklingCatchCount &&
            net.johnceo.sparklingmutuals.safari.SafariAssist.inSafari &&
            net.johnceo.sparklingmutuals.party.PartyManager.getMembers().size>1)
            PartyManager.sendPartyChat(client, SparklingCatchCount.text(entry, entries))
    }

    /** Compare the pre-catch party snapshot; never fetch a delayed API response to verify a live catch. */
    private fun markUnique(entry: SparklingEntry, own: Boolean, wasKnown: Boolean?) {
        if (SparklingUniqueness.isNew(entry, entries, wasKnown)) {
            entry.unique = true; save(); Achievements.onUnique(own)
        }
    }

    fun onTicket() {
        val owner = HistoryAccountId.selected(Minecraft.getInstance()) ?: return
        if (HistoryServerPolicy.allows()) {
            data.ticketCounts[owner] = ticketsSinceSparkling + 1; save()
        }
    }

    /** Called after the pre-import backup; unavailable/deleted logs cannot remove saved records. */
    fun reconcileImport(found: List<SparklingEntry>, alpha: List<SparklingEntry>, foreign: List<SparklingEntry> = emptyList()): Int {
        val account = entries.toMutableList()
        val count = net.johnceo.sparklingmutuals.history.importing.HistoryImportRepair.sparklings(account, found, alpha, foreign)
        if (count > 0) replaceAccountEntries(account)
        return count
    }

    /** Adds catches found elsewhere (old logs), skipping any already here. Returns how many were added. */
    fun importEntries(found: List<SparklingEntry>, ticketTimes: List<Long> = emptyList()): Int {
        val account = entries.toMutableList()
        val added = SparklingHistoryMerge.merge(account, found, ticketTimes)
        replaceAccountEntries(account)
        save().getOrThrow()
        return added
    }

    /** Which catch of this critter an entry was, counting everyone's. */
    private fun replaceAccountEntries(account: List<SparklingEntry>) {
        data.entries.removeIf { belongs(it, ConfigManager.historyOwnerUuid) }
        data.entries.addAll(account)
        data.entries.sortByDescending { it.at }
        accountEntries = null
    }

    fun numberOf(entry: SparklingEntry) = entries.count { it.critter.equals(entry.critter, true) && it.at <= entry.at }
    fun isPersonal(entry: SparklingEntry, currentName: String) = entry.personal ?: entry.player.equals(currentName, true)

    fun save() = HistoryStore.save("sparklings", data).also { accountEntries = null; revision++ }
}
