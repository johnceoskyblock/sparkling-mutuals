package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.SafariTheme
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalChatThemeTest {
    @Test fun `legacy and existing prefixes normalize once for every local line`() {
        assertEquals(listOf("[SM] Warning", "[SM] Ready", "[SM] Details"),
            LocalChat.lines("§b[Sparkling Mutuals] Warning\n[SM] Ready\nDetails"))
        assertEquals(listOf("[SM] §cError"), LocalChat.lines("[SM] §cError"))
    }
    @Test fun `gold palette stays scoped and preserves alpha and semantic colors`() {
        assertEquals(0xAA202026.toInt(), SafariTheme.color(0xAA202026.toInt()))
        SafariTheme.begin()
        try {
            assertEquals(0xAA352D21.toInt(), SafariTheme.color(0xAA202026.toInt()))
            assertEquals(0xFF55FF55.toInt(), SafariTheme.color(0xFF55FF55.toInt()))
        } finally { SafariTheme.end() }
        assertEquals(0xAA202026.toInt(), SafariTheme.color(0xAA202026.toInt()))
    }
}
