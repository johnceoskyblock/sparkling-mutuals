package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

object HotspotToggle {
    internal const val PREFERENCES_SUBCOMMAND = "preferences"

    fun fromParty(text: String, feedback: (Component)->Unit): Boolean {
        val chat = PartyChat.parse(text) ?: return false
        val enabled = requested(chat.body) ?: return false
        set(enabled,feedback)
        return true
    }
    fun requested(body: String): Boolean? = when (PartyCommand.normalize(body).lowercase(java.util.Locale.ROOT)) {
        "!hotspot on", "!hotspots on" -> true
        "!hotspot off", "!hotspots off" -> false
        else -> null
    }
    fun set(enabled: Boolean, feedback: (Component)->Unit, manual: Boolean = false) {
        val turnedOn = enabled && !ConfigManager.hotspotAnnouncer
        ConfigManager.hotspotAnnouncer = enabled
        if (enabled && SafariIsland.onIsland && !SafariAssist.departing)
            SafariTracking.ledger.current?.runParty?.observeHotspot(true)
        ConfigManager.save()
        feedback(LocalChat.component("[SM] Hotspot assigning " + if(enabled) "on." else "off."))
        if (manual && enabled) feedback(LocalChat.component("[SM] Hotspot parties work best when all party members are using Sparkling Mutuals for the hotspot assignments"))
        if (turnedOn) {
            val link = Component.literal("[Preferences]").withStyle {
                it.withColor(SafariTheme.CHAT_ACCENT).withUnderlined(true)
                    .withClickEvent(ClickEvent.RunCommand("/hotspot $PREFERENCES_SUBCOMMAND"))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Open Biome Hotspot Preferences")))
            }
            feedback(LocalChat.component(Component.literal("Want to set your biome preferences? ").append(link)))
        }
    }
}
