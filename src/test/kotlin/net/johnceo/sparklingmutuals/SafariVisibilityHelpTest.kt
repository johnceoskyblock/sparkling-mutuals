package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.CommandHelp
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariVisibilityHelpTest {
    @Test fun `party help fits the server command length and lists both command groups`() {
        val reply = CommandHelp.partyReply(SafariMode.UNIQUE)
        assertTrue(reply.length + "/pc ".length <= 256, "Help reply has ${reply.length} characters")
        assertTrue(reply.contains("Party:"))
        assertTrue(reply.contains("Local:"))
        assertTrue(reply.contains("/captures"))
        assertTrue(reply.contains("!pb doom"))
        assertTrue(reply.contains("!pb haunted"))
        assertTrue(reply.contains("/fc, /unique"))
        assertFalse(reply.contains("/sparkling fc"))
        assertFalse(reply.contains("/pb toggle"))
    }
    @Test fun `entrance visibility is limited to the actual named subarea`() {
        assertFalse(SafariRules.isEntrance(listOf("Area: Torrhus Canyon")))
        assertFalse(SafariRules.isEntrance(listOf("Area: Hub", "Critter Safari Entrance")))
        assertTrue(SafariRules.isEntrance(listOf("Area: Critter Safari Entrance")))
        assertFalse(SafariRules.isEntrance(listOf("Area: Critter Safari")))
        assertEquals(SafariLocation.OUTSIDE, SafariLocation.resolve(listOf("Area: Torrhus Canyon"), null, true))
        assertEquals(SafariLocation.OUTSIDE, SafariLocation.resolve(listOf("Area: Critter Safari"), null, false))
        for (area in listOf("Torrhus Canyon", "Hub", "Village")) {
            val location = SafariLocation.resolve(listOf("Area: $area"), null, false)
            assertFalse(SafariVisibility.visible(1, location == SafariLocation.INSIDE, location == SafariLocation.ENTRANCE))
        }
        assertTrue(SafariVisibility.visible(1, false, SafariRules.isEntrance(listOf("⏣ Critter Safari Entrance"))))
    }
    @Test fun `biome panels hide at center and outside regardless of show where`() {
        for (where in 0..2) {
            assertFalse(SafariVisibility.biomePanels(where, true, true, null))
            assertFalse(SafariVisibility.biomePanels(where, false, true, SafariBiome.FOREST))
            assertFalse(SafariVisibility.biomePanels(where, true, false, SafariBiome.FOREST))
            SafariBiome.entries.forEach { assertTrue(SafariVisibility.biomePanels(where, true, true, it)) }
        }
    }
    @Test fun `local help has separate sections and readable descriptions for every command`() {
        val lines = CommandHelp.localLines(SafariMode.UNIQUE).map(SafariRules::strip)
        assertTrue(lines.any { it == "Party chat commands" })
        assertTrue(lines.any { it == "Local commands" })
        for (command in listOf("!mutual", "!missing", "!ticket", "!pb doom", "!pb wumpa", "!commands",
            "/sparkling", "/sparkling gui", "/captures", "/fc", "/unique", "/alert", "/alertdelay", "/apikey")) {
            assertTrue(lines.any { it.startsWith(command) && it.contains(" — ") }, command)
        }
    }
}
