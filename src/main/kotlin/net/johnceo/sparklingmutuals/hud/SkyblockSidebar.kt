package net.johnceo.sparklingmutuals.hud

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam

object SkyblockSidebar {

    fun title(client: Minecraft): String =
        sidebar(client)
            ?.let { stripFormatting(it.displayName.string).trim() }
            ?: ""

    fun lines(client: Minecraft): List<String> {
        val objective = sidebar(client) ?: return emptyList()
        val level = client.level ?: return emptyList()
        val scoreboard = level.scoreboard

        return scoreboard.listPlayerScores(objective)
            .filterNot { it.isHidden }
            .map { entry ->
                val team = scoreboard.getPlayersTeam(entry.owner())

                val rendered: Component = if (team != null) {
                    PlayerTeam.formatNameForTeam(team, entry.ownerName())
                } else {
                    entry.ownerName()
                }

                stripFormatting(rendered.string).trim()
            }
            .filter(String::isNotEmpty)
    }

    fun inSkyblock(client: Minecraft): Boolean =
        title(client).uppercase().contains("SKYBLOCK")

    private fun sidebar(client: Minecraft): Objective? =
        client.level
            ?.scoreboard
            ?.getDisplayObjective(DisplaySlot.SIDEBAR)

    private fun stripFormatting(text: String): String {
        if (!text.contains('§')) return text

        val out = StringBuilder(text.length)
        var i = 0

        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) {
                i += 2
            } else {
                out.append(text[i])
                i++
            }
        }

        return out.toString()
    }
}