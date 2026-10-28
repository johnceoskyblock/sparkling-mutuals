package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.safari.SafariTicketType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariTicketSelectTest {
    @Test fun `ticket automation and menu hiding are enabled only on the actual Safari island`() {
        val config = net.johnceo.sparklingmutuals.config.ConfigManager
        val previous = config.autoSelectSafariTicket
        try {
            config.autoSelectSafariTicket = true
            assertTrue(SafariTicketSelect.enabledOnIsland("safari"))
            for (mode in listOf(null, "foraging_3", "hub", "garden", "Critter Safari Entrance"))
                assertFalse(SafariTicketSelect.enabledOnIsland(mode), mode)
            config.autoSelectSafariTicket = false
            assertFalse(SafariTicketSelect.enabledOnIsland("safari"))
        } finally { config.autoSelectSafariTicket = previous }
    }
    @Test fun `stale Safari mode cannot automate or hide Torrhus conversion menus`() {
        val config=net.johnceo.sparklingmutuals.config.ConfigManager
        val previous=config.autoSelectSafariTicket
        try {
            config.autoSelectSafariTicket=true
            assertTrue(SafariTicketSelect.validLocation("safari",0,listOf("Area: Critter Safari Entrance")))
            assertTrue(SafariTicketSelect.validLocation("safari",1,listOf("Area: Critter Safari")))
            assertTrue(SafariTicketSelect.validLocation("safari",2,listOf("Area: Cavern Biome")))
            assertFalse(SafariTicketSelect.validLocation("safari",null,listOf("Area: Torrhus Canyon")))
            assertFalse(SafariTicketSelect.validLocation("safari",0,listOf("Area: Torrhus Canyon")))
            assertFalse(SafariTicketSelect.validLocation("safari",null,listOf("Area: Critter Safari Entrance")))
            assertTrue(SafariTicketSelect.validLocation("foraging_3",0,listOf("Area: Critter Safari Entrance")))
            assertTrue(SafariTicketSelect.validLocation(null,0,listOf("Area: Critter Safari")))
            assertTrue(SafariTicketSelect.validLocation("safari",0,emptyList()))
            assertTrue(SafariTicketSelect.validParty(true,true,2,false))
            assertFalse(SafariTicketSelect.validParty(true,true,2,true))
            assertFalse(SafariTicketSelect.validParty(true,true,1,false))
            assertFalse(SafariTicketSelect.validParty(false,true,2,false))
            assertFalse(SafariTicketSelect.validParty(true,false,2,false))
        } finally { config.autoSelectSafariTicket=previous }
    }
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
    @Test fun `entry menu recognition needs neither own entry chat nor manager click and still respects location`() {
        assertTrue(SafariTicketSelect.entryMenu("Critter Safari Entry"))
        assertFalse(SafariTicketSelect.entryMenu("Ticket Converter"))
        val config=net.johnceo.sparklingmutuals.config.ConfigManager
        val previous=config.autoSelectSafariTicket
        try {
            config.autoSelectSafariTicket=true
            assertTrue(SafariTicketSelect.validLocation(null,0,listOf("Area: Critter Safari Entrance")))
            assertTrue(SafariTicketSelect.validLocation("safari",0,emptyList()))
            assertFalse(SafariTicketSelect.validLocation(null,0,emptyList()))
            // Actual Torrhus coordinates stay outside the Safari area map even with stale Safari API data.
            val torrhus=net.johnceo.sparklingmutuals.safari.SafariAreaMap.areaAt(-705.0,92.0,225.0)
            assertFalse(SafariTicketSelect.validLocation("safari",torrhus,listOf("Area: Torrhus Canyon")))
            assertFalse(SafariTicketSelect.validParty(true,true,2,true))
        } finally { config.autoSelectSafariTicket=previous }
    }
}
