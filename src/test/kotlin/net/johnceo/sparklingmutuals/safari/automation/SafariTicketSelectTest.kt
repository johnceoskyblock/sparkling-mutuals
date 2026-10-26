package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.safari.SafariTicketType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariTicketSelectTest {
    @org.junit.jupiter.api.io.TempDir lateinit var dir: java.nio.file.Path
    @Test fun `ticket helper defaults on and GUI saves opt out and preference`() {
        val config = net.johnceo.sparklingmutuals.config.ConfigManager
        config.init(dir)
        assertTrue(config.autoSelectSafariTicket)
        assertEquals(1, config.safariTicketPreference)
        val settings = net.johnceo.sparklingmutuals.config.SafariSettings()
        settings.safari.entry.autoSelectTicket = false
        settings.safari.ticketPreference = 0
        settings.apply()
        config.init(dir)
        assertFalse(config.autoSelectSafariTicket)
        assertEquals(0, config.safariTicketPreference)
        assertFalse(java.nio.file.Files.readString(dir.resolve("sparkling-mutuals.properties")).contains("hideyhoQuestClicks"))
    }

    private fun options(vararg available: SafariTicketType) = SafariTicketType.entries.mapIndexed { index, tier ->
        SafariTicketSelect.Option(index, tier, tier in available)
    }
    @Test fun `ticket preference falls through available tiers and never selects an empty tier`() {
        val all = options(*SafariTicketType.entries.toTypedArray())
        assertEquals(SafariTicketType.FIRST_CLASS, SafariTicketSelect.choose(all, 0)?.tier)
        assertEquals(SafariTicketType.BASIC, SafariTicketSelect.choose(all, 1)?.tier)
        val middle = options(SafariTicketType.PREMIUM, SafariTicketType.ECONOMY)
        assertEquals(SafariTicketType.PREMIUM, SafariTicketSelect.choose(middle, 0)?.tier)
        assertEquals(SafariTicketType.ECONOMY, SafariTicketSelect.choose(middle, 1)?.tier)
        assertNull(SafariTicketSelect.choose(options(), 0))
        assertNull(SafariTicketSelect.choose(all.dropLast(1), 0))
        assertNull(SafariTicketSelect.choose(all.map { it.copy(available = null) }, 0))
    }
    @Test fun `owned counts and unavailable lore take priority over generic click prompts`() {
        assertEquals(true, SafariTicketSelect.available(listOf("§7Owned: §a1,234", "Click to use!")))
        assertEquals(false, SafariTicketSelect.available(listOf("Owned: 0", "Click to use!")))
        assertEquals(false, SafariTicketSelect.available(listOf("You don't have any tickets!", "Click to enter!")))
        assertEquals(true, SafariTicketSelect.available(listOf("You have 2 tickets")))
        assertEquals(true, SafariTicketSelect.available(listOf("Click to enter!")))
        assertNull(SafariTicketSelect.available(listOf("First Class Ticket", "A ticket for Critter Safari")))
    }
}
