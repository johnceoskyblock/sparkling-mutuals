// SPDX-License-Identifier: LGPL-2.1-only
// Adapted from NopoTheGamer's Ubik timer in SkyHanni (PR #4909).
package net.johnceo.sparklingmutuals.skyblock

import java.util.Locale
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.compat.smHideGui
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.history.HistoryStore
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component

internal object UbiksCubeRules {
    const val MENU = "Split or Steal"
    const val COOLDOWN = 2 * 60 * 60_000L
    private val wait = Regex("""^SPLIT! You need to wait (?=\d)(?:(\d{1,2})h\s*)?(?:(\d{1,2})m\s*)?(?:(\d{1,2})s)?\s+before you can play again\.$""")
    private val profile = Regex("""^(?:You are playing on profile|Your profile was changed to):\s*([A-Za-z]+)(?:\s+\(Co-op\))?\.?$""", RegexOption.IGNORE_CASE)

    fun profile(text: String): String? = profile.matchEntire(SafariRules.strip(text).trim())
        ?.groupValues?.get(1)?.lowercase(Locale.ROOT)

    fun reportedWait(line: String): Long? {
        val match = wait.matchEntire(SafariRules.strip(line).trim()) ?: return null
        val (hours, minutes, seconds) = match.groupValues.drop(1).map { it.toLongOrNull() ?: 0L }
        return (hours * 3600 + minutes * 60 + seconds) * 1000
    }

    fun remaining(deadline: Long, now: Long) = ((deadline - now).coerceAtLeast(0) + 999) / 1000

    fun format(seconds: Long): String = buildList {
        if (seconds >= 3600) add("${seconds / 3600}h")
        if (seconds % 3600 >= 60) add("${seconds / 60 % 60}m")
        if (seconds % 60 > 0) add("${seconds % 60}s")
    }.joinToString(" ")
}

object UbiksCubeTimer {
    private const val STORE = "ubiks-cube"
    private class Cooldowns(val deadlines: MutableMap<String, Long> = mutableMapOf())
    private val cooldowns by lazy { HistoryStore.load(STORE, Cooldowns::class.java, ::Cooldowns) }
    private var profile: String? = null
    private var pendingDeadline: Long? = null
    private var openMenu: Screen? = null

    fun register() {
        ClientReceiveMessageEvents.GAME.register(::receive)
        ClientReceiveMessageEvents.GAME_CANCELED.register(::receive)
    }

    private fun receive(message: Component, overlay: Boolean) {
        if (overlay) return
        val client = Minecraft.getInstance()
        val connection = client.connection ?: return
        client.execute {
            if (client.connection !== connection || !JoinWelcomeState.hypixel(client.currentServer?.ip)) return@execute
            val text = message.string
            UbiksCubeRules.profile(text)?.let { confirmed ->
                if (profile != null && profile != confirmed) pendingDeadline = null
                profile = confirmed
                savePending(client)
            }
            if (ConfigManager.ubiksCubeTimer) {
                UbiksCubeRules.reportedWait(text)?.takeIf { SkyblockSidebar.inSkyblock(client) }
                    ?.let { start(client, System.currentTimeMillis() + it) }
            }
        }
    }

    private fun scope(client: Minecraft): String? {
        val player = client.player ?: return null
        return profile?.let { "${player.uuid}.$it" }
    }

    private fun start(client: Minecraft, deadline: Long) {
        pendingDeadline = deadline
        savePending(client)
    }

    private fun savePending(client: Minecraft) {
        val deadline = pendingDeadline ?: return
        val key = scope(client) ?: return
        if (cooldowns.deadlines.put(key, deadline) != deadline) HistoryStore.save(STORE, cooldowns)
        pendingDeadline = null
    }

    fun tick(client: Minecraft) {
        if (!ConfigManager.ubiksCubeTimer) { openMenu = null; return }
        savePending(client)
        val menu = (client.smScreen as? AbstractContainerScreen<*>)?.takeIf {
            SafariRules.strip(it.title.string).trim() == UbiksCubeRules.MENU && inSkyblock(client)
        }
        if (menu != null && menu !== openMenu) start(client, System.currentTimeMillis() + UbiksCubeRules.COOLDOWN)
        openMenu = menu
    }

    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (!ConfigManager.ubiksCubeTimer || client.options.smHideGui || client.smScreen is ContestGui || !inSkyblock(client)) return
        val key = scope(client)
        val deadline = pendingDeadline ?: key?.let { cooldowns.deadlines[it] ?: 0L } ?: return
        panel(UbiksCubeRules.remaining(deadline, System.currentTimeMillis())).draw(graphics, SafariHud.UBIKS_CUBE)
    }

    private fun inSkyblock(client: Minecraft) = client.player != null && JoinWelcomeState.hypixel(client.currentServer?.ip) &&
        SkyblockSidebar.inSkyblock(client)

    private fun panel(seconds: Long) = HudPanel("Ubik's Cube", rows = listOf(
        HudRow("Cooldown", if (seconds == 0L) "Ready!" else UbiksCubeRules.format(seconds), HudRow.GRAY,
            if (seconds == 0L) 0xFF55FF55.toInt() else HudRow.GOLD)
    ))

    fun preview() = panel(5_025)

    fun reset() {
        profile = null
        pendingDeadline = null
        openMenu = null
    }
}
