package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

/** One local introduction per connection, including transfers between Hypixel lobbies. */
internal class JoinWelcomeState {
    private var readyAt: Long? = null
    var shown = false
        private set
    fun shouldShow(hypixel: Boolean, ready: Boolean, now: Long): Boolean {
        if (shown || !ready) return false
        if (!hypixel) { readyAt = null; return false }
        val start = readyAt ?: now.also { readyAt = it }
        if (now - start < 2000) return false
        shown = true
        return true
    }
    companion object {
        fun hypixel(address: String?): Boolean {
            val host = address?.trim()?.substringBefore(':')?.removeSuffix(".")?.lowercase() ?: return false
            return host == "hypixel.net" || host.endsWith(".hypixel.net")
        }
    }
}

object JoinWelcome {
    private var state = JoinWelcomeState()
    fun tick(client: Minecraft) {
        if (state.shown) return
        val player = client.player ?: return
        if (client.connection == null || client.level == null) return
        val hypixel = JoinWelcomeState.hypixel(client.currentServer?.ip) ||
            SkyblockSidebar.lines(client).any { it.equals("www.hypixel.net", true) || it.equals("alpha.hypixel.net", true) }
        if (!state.shouldShow(hypixel, true, System.currentTimeMillis())) return
        player.sendSystemMessage(message())
    }
    /** This introduction is one MOTD-style block; ordinary local feedback keeps its [SM] prefix. */
    fun message(): Component {
        fun text(value: String, color: Int = SafariTheme.CHAT_TEXT) = Component.literal(value)
            .withStyle { it.withColor(color) }
        fun command(value: String, description: String) = text(value, SafariTheme.CHAT_ACCENT).withStyle {
            it.withClickEvent(ClickEvent.RunCommand(value))
                .withHoverEvent(HoverEvent.ShowText(text(description)))
        }
        val lines = listOf(
            text("────── ", SafariTheme.ACCENT)
                .append(text("Sparkling Mutuals", SafariTheme.CHAT_ACCENT).withStyle { it.withBold(true) })
                .append(text(" ──────", SafariTheme.ACCENT)),
            text("  ").append(command("/sm", "Open settings")).append(" Settings   ")
                .append(command("/sm help", "Show available local and party commands")).append(" Commands"),
            text("  ").append(command("/unique", "Select Unique mode")).append("  Unique run mode."),
            text("  ").append(command("/fc", "Select Full Clear mode")).append("  Full clear run mode."),
            text("  ").append(command("/sparkle", "Select Sparkle mode")).append("  Sparkle run mode."),
            text("  ").append(command("/sm gui", "Move and resize HUDs")).append(" HUD layout   ")
                .append(command("/sm history", "Open runs, bests and stats")).append(" History"),
            text("────────────────────────────────", SafariTheme.ACCENT)
        )
        return Component.empty().also { block ->
            lines.forEachIndexed { index, line ->
                if (index > 0) block.append("\n")
                block.append(line)
            }
        }
    }
    fun reset() { state = JoinWelcomeState() }
}
