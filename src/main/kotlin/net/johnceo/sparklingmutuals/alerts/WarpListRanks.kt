package net.johnceo.sparklingmutuals.alerts

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft

/** Rank evidence already sent by Hypixel. Warp List rendering never needs a profile lookup. */
internal object WarpListRanks {
    private val observed = linkedMapOf<String, Int>()
    private val rankedName = Regex("""^\[([A-Z+_]+)]\s+([A-Za-z0-9_]{1,16})(?=\s|:|$)""")
    private val chatPrefix = Regex("""^(?:(?:Party|Guild|Officer)\s*[>»]\s*|(?:From|To)\s+)""")

    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay -> if (!overlay) observeChat(message.string) }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ -> observeChat(message.string) }
    }

    private fun observeChat(message: String) {
        val client = Minecraft.getInstance()
        if (!JoinWelcomeState.hypixel(client.currentServer?.ip)) return
        client.execute {
            val line = SafariRules.strip(message).trim()
            if (':' in line) remember(chatPrefix.replaceFirst(line, ""))
        }
    }

    fun colors(client: Minecraft): Map<String, Int> {
        if (!JoinWelcomeState.hypixel(client.currentServer?.ip)) return emptyMap()
        client.connection?.onlinePlayers?.forEach { info ->
            info.tabListDisplayName?.string?.let { remember(SafariRules.strip(it).trim(), info.profile.name) }
        }
        return observed.toMap()
    }

    private fun remember(text: String, expected: String? = null) {
        val (rank, name) = rankedName.find(text)?.destructured ?: return
        if (expected != null && !name.equals(expected, true)) return
        val color = when (rank) {
            "VIP", "VIP+" -> 0xFF55FF55
            "MVP", "MVP+" -> 0xFF55FFFF
            "MVP++" -> 0xFFFFAA00
            "HELPER", "MOD", "GM", "GAME_MASTER", "ADMIN", "OWNER", "YOUTUBE" -> 0xFFFF5555
            else -> return
        }
        observed[name.lowercase()] = color.toInt()
        if (observed.size > 512) observed.remove(observed.keys.first())
    }
}
