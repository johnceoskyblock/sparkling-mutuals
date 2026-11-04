package net.johnceo.sparklingmutuals

import java.net.URI
import net.johnceo.sparklingmutuals.config.ModUpdates
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

/** One local introduction per connection, after arriving in SkyBlock. */
internal class JoinWelcomeState {
    private var readyAt: Long? = null
    var shown = false
        private set
    fun shouldShow(skyblock: Boolean, ready: Boolean, now: Long): Boolean {
        if (shown) return false
        if (!skyblock) { readyAt = null; return false }
        val start = readyAt ?: now.also { readyAt = it }
        if (!ready || now - start < 2000) return false
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
        val skyblock = hypixel && SkyblockSidebar.inSkyblock(client)
        if (skyblock) ModUpdates.check()
        if (!state.shouldShow(skyblock, ModUpdates.label != "Checking…", System.currentTimeMillis())) return
        val update = ModUpdates.release?.tag?.takeIf { (ModUpdates.compareVersions(it, ModUpdates.installed) ?: 0) > 0 }
        player.sendSystemMessage(message(update))
    }
    /** This introduction is one MOTD-style block; ordinary local feedback keeps its [SM] prefix. */
    fun message(updateTag: String? = null): Component {
        fun text(value: String, color: Int = SafariTheme.CHAT_TEXT) = Component.literal(value)
            .withStyle { it.withColor(color) }
        fun command(value: String, description: String) = text(value, SafariTheme.CHAT_ACCENT).withStyle {
            it.withClickEvent(ClickEvent.RunCommand(value))
                .withHoverEvent(HoverEvent.ShowText(text(description)))
        }
        val heading = text("────── ", SafariTheme.ACCENT)
                .append(text("Sparkling Mutuals", SafariTheme.CHAT_ACCENT).withStyle { it.withBold(true) })
                .append(text(" ──────", SafariTheme.ACCENT))
        val font=Minecraft.getInstance().font
        val divider="─".repeat((font.width(heading)+font.width("─")/2)/font.width("─").coerceAtLeast(1))
        val lines = buildList {
            addAll(listOf(
            heading,
            text("  ").append(command("/sm", "Open settings")).append(" Settings   ")
                .append(command("/sm help", "Show available local and party commands")).append(" Commands"),
            text("  ").append(command("/unique", "Select Unique mode")).append("  Unique run mode."),
            text("  ").append(command("/fc", "Select Full Clear mode")).append("  Full clear run mode."),
            text("  ").append(command("/sparkle", "Select Sparkle mode")).append("  Sparkle run mode."),
            text("  ").append(command("/sm gui", "Move and resize HUDs")).append(" HUD layout   ")
                .append(command("/sm history", "Open runs, bests and stats")).append(" History")
            ))
            updateTag?.let { tag ->
                val url = URI.create("${ModUpdates.REPOSITORY}/releases/tag/$tag")
                val version = text("v${tag.removePrefix("v")}", SafariTheme.CHAT_ACCENT).withStyle {
                    it.withUnderlined(true).withClickEvent(ClickEvent.OpenUrl(url))
                        .withHoverEvent(HoverEvent.ShowText(text("Open the GitHub release")))
                }
                add(text("  Latest release: ").append(version))
            }
            add(text(divider, SafariTheme.ACCENT))
        }
        return Component.empty().also { block ->
            lines.forEachIndexed { index, line ->
                if (index > 0) block.append("\n")
                block.append(line)
            }
        }
    }
    fun reset() { state = JoinWelcomeState() }
}
