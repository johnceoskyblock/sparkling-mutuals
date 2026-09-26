package net.johnceo.sparklingmutuals

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam

object SkyblockSidebar {

    fun title(client: Minecraft): String {
        val objective = sidebar(client)

        return if (objective == null) {
            ""
        } else {
            stripFormatting(
                objective.displayName.string
            ).trim()
        }
    }

    fun lines(client: Minecraft): List<String> {
        val objective = sidebar(client)
            ?: return emptyList()

        val level = client.level
            ?: return emptyList()

        val scoreboard = level.scoreboard
        val lines = mutableListOf<String>()

        for (entry in scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden) {
                continue
            }

            val team = scoreboard.getPlayersTeam(entry.owner())

            val rendered: Component =
                if (team != null) {
                    PlayerTeam.formatNameForTeam(
                        team,
                        entry.ownerName()
                    )
                } else {
                    entry.ownerName()
                }

            val text = stripFormatting(rendered.string).trim()

            if (text.isNotEmpty()) {
                lines.add(text)
            }
        }

        return lines
    }

    fun inSkyblock(client: Minecraft): Boolean {
        return title(client)
            .uppercase()
            .contains("SKYBLOCK")
    }

    private fun sidebar(client: Minecraft): Objective? {
        val level = client.level
            ?: return null

        return level.scoreboard
            .getDisplayObjective(DisplaySlot.SIDEBAR)
    }

    private fun stripFormatting(text: String): String {
        if (!text.contains('§')) {
            return text
        }

        val out = StringBuilder(text.length)
        var i = 0

        while (i < text.length) {
            val c = text[i]

            if (c == '§' && i + 1 < text.length) {
                i++
            } else {
                out.append(c)
            }

            i++
        }

        return out.toString()
    }
}