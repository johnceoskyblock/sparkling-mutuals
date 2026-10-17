package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.safari.SafariIsland
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariManagerGuardTest {
    @Test fun `spam clicks recognize every aligned Manager proxy despite disappearing labels`() {
        val memory = EntryManagerTargets()
        val world = Any()
        val label = Triple(0.0, 12.0, 0.0)
        assertTrue(memory.recognize(world, "label", true, 0.0, 12.0, 0.0, true, false, listOf(label)))
        repeat(100) { attempt ->
            assertTrue(memory.recognize(world, "proxy-${attempt % 3}", true,
                0.0, 10.0 + attempt % 3, 0.0, false, false, emptyList()))
            assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, false, false, false, false))
        }
        assertFalse(memory.recognize(world, "party-player", true, 0.0, 10.0, 0.0, false, true, listOf(label)))
        assertFalse(memory.recognize(world, "nearby", true, 1.0, 10.0, 0.0, false, false, listOf(label)))
        assertFalse(memory.recognize(Any(), "proxy-0", true, 0.0, 10.0, 0.0, false, false, emptyList()))
    }
    @Test fun `missing member notices show a short count and cached player names`() {
        val names = mapOf("a" to "JohnFounder", "b" to "JohnCEO")
        assertEquals("1 missing: JohnFounder", SafariManagerGuard.missingMessage(setOf("a"), names::get))
        assertEquals("2 missing: JohnCEO, JohnFounder", SafariManagerGuard.missingMessage(setOf("a", "b"), names::get))
        assertEquals("Waiting for the party to load", SafariManagerGuard.missingMessage(emptySet(), names::get))
        assertEquals("1 missing: name loading", SafariManagerGuard.missingMessage(setOf("unknown"), names::get))
    }
    @Test fun `only server entry nametags in the Safari entry menu are guarded`() {
        assertTrue(SafariManagerGuard.entryItem("Critter Safari Entry", true, false))
        assertTrue(SafariManagerGuard.entryItem("§aCritter Safari Entry", true, false))
        assertFalse(SafariManagerGuard.entryItem("Critter Safari Entry", true, true))
        assertFalse(SafariManagerGuard.entryItem("Critter Safari Entry", false, false))
        assertFalse(SafariManagerGuard.entryItem("Storage", true, false))
    }
    @Test fun `party kick substring matches only the body`() {
        assertTrue(SafariManagerGuard.callsKick("Party > Farmer: KICK"))
        assertTrue(SafariManagerGuard.callsKick("Party > [VIP] Farmer: can someone kick Steve?"))
        assertTrue(SafariManagerGuard.callsKick("Party > Farmer: kickstart the run"))
        assertTrue(SafariManagerGuard.callsKick("Party > Farmer: I was kicked!"))
        assertFalse(SafariManagerGuard.callsKick("Party > KickPlayer: ready"))
        assertFalse(SafariManagerGuard.callsKick("Guild > Farmer: kick him"))
        assertFalse(SafariManagerGuard.callsKick("Farmer has been kicked from the party"))
    }
    @Test fun `only the Safari island counts, not Torrhus Canyon's Critter Safari zone`() {
        assertTrue(SafariIsland.isSafari("safari"))
        assertFalse(SafariIsland.isSafari("foraging_3"))
        assertFalse(SafariIsland.isSafari(null))
    }
}
