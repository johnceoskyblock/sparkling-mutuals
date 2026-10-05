package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.SafariRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariAssistTest {
    @Test fun `scan uses an inclusive spherical 80 block radius`() {
        assertTrue(SafariRules.withinRange(6400.0))
        assertFalse(SafariRules.withinRange(6400.01))
        assertFalse(SafariRules.withinRange(Double.NaN))
    }

    @Test fun `haunted helpers only run in Safari haunted areas`() {
        assertTrue(SafariRules.isSafari(listOf("⏣ Critter Safari")))
        assertTrue(SafariRules.isSafari(listOf("⏣ Critter Safari\u200B\uE000")))
        assertTrue(SafariRules.isSafari(listOf("Area: Critter Safari\u200B")))
        assertFalse(SafariRules.isSafari(listOf("Area: Hub", "Critter Safari")))
        assertTrue(SafariRules.isHaunted(listOf("⏣ Haunted Mansion")))
        assertFalse(SafariRules.isHaunted(listOf("⏣ Hub", "Objective: Visit Haunted Mansion")))
        assertFalse(SafariRules.isSafari(listOf("⏣ Critter Safari Entrance")))
    }

    @Test fun `CritterMod 0_9 sparkling labels match exact species`() {
        assertEquals("Rockmite", SafariRules.sparklingSpecies("§6SPARKLING Rockmite"))
        assertEquals("Rockmite", SafariRules.sparklingSpecies("§6SPARKLING Rockmite\u200B"))
        assertEquals("Mantis Shrimp", SafariRules.sparklingSpecies("Sparkling Mantis Shrimp"))
        assertNull(SafariRules.sparklingSpecies("Rockmite"))
        assertNull(SafariRules.sparklingSpecies("Sparkling Rockmite Shard"))
        assertNull(SafariRules.sparklingSpecies("Capturing Sparkling Rockmite"))
    }

    @Test fun `only server Hideyho dialogue arms quest acceptance`() {
        assertTrue(SafariRules.isHideyhoDialogue("[NPC] Hideyho: Want to play again?"))
        assertFalse(SafariRules.isHideyhoDialogue("Party > Farmer: Hideyho: Want to play again?"))
        assertFalse(SafariRules.isHideyhoDialogue("[SM] Click to accept Hideyho"))
    }
}
