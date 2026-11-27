package net.johnceo.sparklingmutuals.history

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

internal enum class SafariEncounter(val label: String, val announcement: String, val crittersPerCheck: Int = 1) {
    DOOMSPIRAL("Doomspiral", "Something stirs in the Haunted Biome..."),
    WUMPA("Wumpa", "You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?"),
    GEMZIE("Gemzie", "A rumbling sound can be heard, and the door at the back of the chamber opens...", crittersPerCheck = 3);

    companion object {
        private val byAnnouncement = entries.associateBy { it.announcement }
        fun parse(text: String) = byAnnouncement[text]
    }
}

internal object SafariEncounterHistory {
    data class Check(val encounter: SafariEncounter, val at: Long)
    class Data(val accounts: MutableMap<String, MutableList<Check>> = mutableMapOf())
    private var data = Data()
    private const val FILE = "encounters"

    fun register() {
        data = HistoryStore.load(FILE, Data::class.java, ::Data)
        ClientReceiveMessageEvents.GAME.register(::receive)
        ClientReceiveMessageEvents.GAME_CANCELED.register(::receive)
    }

    private fun receive(message: Component, overlay: Boolean) {
        if (overlay) return
        val encounters = SafariMessages.lines(message.string).mapNotNull(SafariEncounter::parse)
        if (encounters.isEmpty()) return
        val client = Minecraft.getInstance()
        val connection = client.connection
        val now = System.currentTimeMillis()
        client.execute {
            if (client.connection !== connection || !HistoryServerPolicy.verifiedMain(client)) return@execute
            val owner = HistoryAccountId.selected(client) ?: return@execute
            runCatching { merge(owner, encounters.map { Check(it, now) }) }
        }
    }

    fun count(encounter: SafariEncounter): Int =
        data.accounts[ConfigManager.historyOwnerUuid].orEmpty().count { it.encounter == encounter } * encounter.crittersPerCheck

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
                if (accepted.lastOrNull()?.let { event.at - it.at <= 2_000 } != true) accepted += event
            }
            accepted
        }
        if (merged.size != saved.size) {
            val replacement = Data(data.accounts.toMutableMap().apply { put(owner, merged.toMutableList()) })
            HistoryStore.save(FILE, replacement).getOrThrow()
            data = replacement
        }
        return merged.sumOf { it.encounter.crittersPerCheck } - saved.sumOf { it.encounter.crittersPerCheck }
    }
}
