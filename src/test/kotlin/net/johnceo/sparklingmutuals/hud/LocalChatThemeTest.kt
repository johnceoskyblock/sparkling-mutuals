package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.SafariTheme
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalChatThemeTest {
    @Test fun `legacy and existing prefixes normalize once for every local line`() {
        assertEquals(listOf("[SM] Warning", "[SM] Ready", "[SM] Details"),
            LocalChat.lines("§b[Sparkling Mutuals] Warning\n[SM] Ready\nDetails"))
        assertEquals(listOf("[SM] §cError"), LocalChat.lines("[SM] §cError"))
        val plain = LocalChat.component("Ready").toFlatList()
        assertEquals(SafariTheme.CHAT_ACCENT, plain.first().style.color?.value)
        assertEquals(SafariTheme.CHAT_TEXT, plain.last().style.color?.value)
    }
}
