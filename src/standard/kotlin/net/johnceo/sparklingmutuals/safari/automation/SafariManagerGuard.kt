package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.config.BannerConfig
import net.johnceo.sparklingmutuals.hud.BannerKind
import net.johnceo.sparklingmutuals.hud.SafariBanners
import net.johnceo.sparklingmutuals.safari.SafariAreaMap
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariEntryRunState
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.minecraft.client.Minecraft

/** Tracks run presence and party kick reports for chat and HUDs. */
object SafariManagerGuard {
    private val runState = SafariEntryRunState()
    private val kickReport = Regex(
        """^(?:kick(?:ed)?[.!]*|.*\b(?:got|was|were|been) kicked\b.*|.*\bkicked from (?:the )?(?:safari|run|instance)\b.*)$""",
        RegexOption.IGNORE_CASE,
    )

    fun activeRun(client: Minecraft): Boolean {
        val player = client.player ?: return false
        return runState.active(client.level, SafariIsland.onIsland, SafariAssist.location,
            SafariAreaMap.areaAt(player.x, player.y, player.z))
    }

    fun inSafariInstance() = SafariIsland.onIsland
    fun tracksKickReports() = BannerConfig.enabled && BannerConfig.options.getValue(BannerKind.PARTY_KICKED).enabled
    fun isKickReport(body: String) = kickReport.matches(body.trim())

    fun partyKick(chat: PartyChat, receivedAt: Long) {
        val client = Minecraft.getInstance()
        if (!tracksKickReports() || !inSafariInstance() || chat.sender.equals(client.player?.name?.string, true)) return
        if (System.currentTimeMillis() - receivedAt !in 0..10_000) return
        SafariBanners.fire(BannerKind.PARTY_KICKED, player = chat.sender)
    }

    fun debug() = listOf("Safari kick alerts: enabled=${tracksKickReports()} inSafari=${inSafariInstance()}")
}
