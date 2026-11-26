package net.johnceo.sparklingmutuals.history

import kotlin.math.abs
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.history.importing.HistoryBackfill
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.client.Minecraft

internal enum class SafariEncounter(val label: String, val announcement: String) {
    DOOMSPIRAL("Doomspiral", "Something stirs in the Haunted Biome..."),
    WUMPA("Wumpa", "You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?"),
    GEMZIE("Gemzie", "A rumbling sound can be heard, and the door at the back of the chamber opens...");

    companion object {
        private val byAnnouncement = entries.associateBy { it.announcement }
        fun parse(text: String) = byAnnouncement[text]
    }
}

internal object SafariEncounterHistory {
    data class Check(val encounter: SafariEncounter, val at: Long)
    class Data(val accounts: MutableMap<String, MutableList<Check>> = mutableMapOf())
    private var data = Data()
    private val lastRun = mutableMapOf<SafariEncounter, Pair<String, Long>>()
    private const val FILE = "encounters"

    fun register() {
        data = HistoryStore.load(FILE, Data::class.java, ::Data)
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (overlay) return@register
            val encounters = SafariMessages.lines(message.string).mapNotNull(SafariEncounter::parse)
            if (encounters.isEmpty()) return@register
            val client = Minecraft.getInstance()
            val connection = client.connection
            client.execute {
                if (client.connection !== connection || !HistoryServerPolicy.verifiedMain(client)) return@execute
                val owner = HistoryAccountId.selected(client) ?: return@execute
                val run = SafariTracking.ledger.current?.takeUnless { it.historyExcluded } ?: return@execute
                val now = System.currentTimeMillis()
                val runKey = owner to run.startedAt
                val fresh = encounters.distinct().filter { lastRun[it] != runKey }
                if (runCatching { merge(owner, fresh.map { Check(it, now) }) }.isSuccess) {
                    fresh.forEach { lastRun[it] = runKey }
                }
            }
        }
    }

    fun count(encounter: SafariEncounter): Int = data.accounts[ConfigManager.historyOwnerUuid].orEmpty().count { it.encounter == encounter }

    fun import(owner: String, checks: List<Check>): Int {
        HistoryStore.backup(FILE)
        return merge(owner, checks)
    }

    private fun merge(owner: String, checks: List<Check>): Int {
        if (checks.isEmpty()) return 0
        val saved = data.accounts[owner].orEmpty()
        val merged = (saved + checks).groupBy { it.encounter }.values.flatMap { events ->
            val accepted = mutableListOf<Check>()
            for (event in events.sortedBy { it.at }) {
                if (accepted.lastOrNull()?.let { abs(event.at - it.at) <= 2_000 } != true) accepted += event
            }
            accepted
        }
        if (merged.size != saved.size) {
            val replacement = Data(data.accounts.toMutableMap().apply { put(owner, merged.toMutableList()) })
            HistoryStore.save(FILE, replacement).getOrThrow()
            data = replacement
        }
        return merged.size - saved.size
    }
}
