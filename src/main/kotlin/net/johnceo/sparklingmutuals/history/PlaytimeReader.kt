package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Inventory

/**
 * All-time Safari playtime from SkyBlock's playtime menu (`/playtime`, then the result): the "Safari" item says
 * "Playtime: 2,251 minutes". It covers time from before runs were recorded, so the hours stats and achievements use it.
 */
object PlaytimeReader {
    class Data(var minutes: Long = 0, var readAt: Long = 0, var ownerUuid: String? = null)
    private var data = Data()
    private val playtime = Regex("^(?:(?:Total |Safari )?Play\\s*time|Time Played)\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val timePart = Regex("([\\d,]+)\\s*(days?|d|hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)\\b", RegexOption.IGNORE_CASE)
    private val dyeGuide = Regex("^(?:\\(\\d+/\\d+\\) )?Dye Guide$", RegexOption.IGNORE_CASE)
    private val dyeTotal = Regex("^You['’]ve dropped: ([\\d,]+)$", RegexOption.IGNORE_CASE)
    private var lastScreen: AbstractContainerScreen<*>? = null
    private var lastRead: Pair<String, Long>? = null

    fun register() { data = HistoryStore.load("playtime", Data::class.java, ::Data) }

    fun tick(client: Minecraft) {
        val screen = client.smScreen as? AbstractContainerScreen<*>
        if (screen !== lastScreen) { lastScreen = screen; lastRead = null }
        if (screen == null || !HistoryServerPolicy.verifiedMain(client)) return
        val owner = HistoryAccountId.selected(client) ?: return
        val title = SafariRules.strip(screen.title.string).trim()
        val dyes = dyeGuide.matches(title)
        for (slot in screen.menu.slots) {
            if (slot.container is Inventory) continue
            val stack = slot.item
            if (stack.isEmpty) continue
            val name = SafariRules.strip(stack.hoverName.string).trim()
            val lore = stack.get(DataComponents.LORE)?.lines() ?: continue
            if (dyes && name.equals("Emerald Dye", true)) {
                val count = lore.firstNotNullOfOrNull { dyeDrops(it.string) } ?: continue
                if (lastRead != (owner to count.toLong())) {
                    Achievements.syncEmeraldDyes(owner, count)
                    lastRead = owner to count.toLong()
                }
                return
            }
            if (dyes || !(name.equals("Safari", true) || name.equals("Critter Safari", true))) continue
            val minutes = lore.firstNotNullOfOrNull { playtimeMinutes(it.string) } ?: continue
            if (lastRead == (owner to minutes)) return
            val first = data.minutes <= 0
            val snapshot = Data(minutes, System.currentTimeMillis(), owner)
            if (HistoryStore.save("playtime", snapshot).isSuccess) {
                data = snapshot
                lastRead = owner to minutes
                if (first) client.player?.sendSystemMessage(LocalChat.component("[SM] Safari playtime saved: §e%.1f hours§r.".format(minutes / 60.0)))
            }
            return
        }
    }

    internal fun dyeDrops(line: String): Int? = dyeTotal.matchEntire(SafariRules.strip(line).trim())
        ?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

    internal fun playtimeMinutes(line: String): Long? {
        val value = playtime.matchEntire(SafariRules.strip(line).trim())?.groupValues?.get(1) ?: return null
        val parts = timePart.findAll(value).toList()
        if (parts.isEmpty()) return null
        val totalSeconds = parts.sumOf { part ->
            val count = part.groupValues[1].replace(",", "").toLongOrNull()?.coerceAtMost(100_000_000L) ?: return null
            count * when (part.groupValues[2].first().lowercaseChar()) {
                'd' -> 86400; 'h' -> 3600; 'm' -> 60; else -> 1
            }
        }
        return totalSeconds / 60
    }

    val menuMillis get() = (data.minutes * 60_000L).takeIf { data.readAt > 0 && (data.ownerUuid == null ||
        data.ownerUuid == net.johnceo.sparklingmutuals.config.ConfigManager.historyOwnerUuid) }

    /** Hour achievements also count local run time before the player has opened the menu. */
    fun safariMillis(): Long {
        val runs = RunHistory.runs.sumOf { it.length }
        val playtime = menuMillis ?: return runs
        return maxOf(runs, playtime + RunHistory.runs.filter { it.start > data.readAt }.sumOf { it.length })
    }
}
