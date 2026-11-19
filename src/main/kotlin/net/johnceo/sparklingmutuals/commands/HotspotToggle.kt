package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.network.chat.Component

object HotspotToggle {
    fun fromParty(text: String, feedback: (Component)->Unit): Boolean {
        val chat = PartyChat.parse(text) ?: return false
        val enabled = requested(chat.body) ?: return false
        set(enabled,feedback)
        return true
    }
    fun requested(body: String): Boolean? = when (body.trim().lowercase(java.util.Locale.ROOT)) {
        "!hotspot on" -> true
        "!hotspot off" -> false
        else -> null
    }
    fun set(enabled: Boolean, feedback: (Component)->Unit, manual: Boolean = false) {
        ConfigManager.hotspotAnnouncer = enabled
        ConfigManager.save()
        feedback(LocalChat.component("[SM] Hotspot assigning " + if(enabled) "on." else "off."))
        if (manual && enabled) feedback(LocalChat.component("[SM] Hotspot parties work best when all party members are using Sparkling Mutuals for the hotspot assignments"))
    }
}
