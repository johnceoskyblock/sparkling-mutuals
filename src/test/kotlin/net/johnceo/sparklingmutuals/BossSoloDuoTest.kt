package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.commands.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class BossSoloDuoTest {
    @Test fun `Wumpa solo requires all eight personal prerequisites and duo only the own boss capture`() {
        val run = SafariRun(1000); val bests = SafariPersonalBests()
        SafariBiome.ICY.critters.filter { it.name != "Wumpa" }.forEach { run.record(SafariCatch(it, false)) }
        assertTrue(bests.record("CAPTURE! You caught a Wumpa!", run, 2000))
        assertNull(bests.time("Wumpa")); assertEquals(1000L, bests.time("Wumpa", duo = true))
        SafariBiome.ICY.critters.filter { it.name !in setOf("Wumpa", "Troodon") }.forEach { run.record(SafariCatch(it)) }
        bests.record("CAPTURE! You caught a Wumpa!", run, 2500)
        assertNull(bests.time("Wumpa"))
        run.record(SafariCatch(SafariRoster.named("Troodon")!!))
        assertTrue(bests.record("CAPTURE! You caught a Wumpa!", run, 3000))
        assertEquals(2000L, bests.time("Wumpa")); assertEquals(1000L, bests.time("Wumpa", duo = true))
    }
    @Test fun `Doom solo remembers a simultaneous inventory stack total independently of alert switches`() {
        val run = SafariRun(0); val bests = SafariPersonalBests()
        run.inventoryEvidence(listOf("Soothing Incense" to 2)); run.inventoryEvidence(listOf("Soothing Incense" to 2))
        bests.record("CAPTURE! You caught a Doomspiral!", run, 1000)
        assertNull(bests.time("Doomspiral"))
        run.inventoryEvidence(listOf("Soothing Incense Shard" to 4)); assertFalse(run.hadFourIncense)
        run.inventoryEvidence(listOf("§aSoothing Incense" to 2, "Soothing Incense" to 2))
        run.inventoryEvidence(emptyList())
        assertTrue(bests.record("CAPTURE! You caught a Doomspiral!", run, 2000))
        assertEquals(2000L, bests.time("Doomspiral")); assertEquals(1000L, bests.time("Doomspiral", duo = true))
        assertFalse(SafariRun(3000).hadFourIncense)
    }
    @Test fun `legacy unrestricted PBs migrate to duo and both categories persist independently`() {
        val props = Properties().apply { setProperty("pb.doomspiralMillis", "800"); setProperty("pb.wumpaMillis", "900") }
        val bests = SafariPersonalBests(); bests.load(props)
        assertNull(bests.time("Doomspiral")); assertNull(bests.time("Wumpa"))
        assertEquals(800L, bests.time("Doomspiral", duo = true))
        val run = SafariRun(0); run.inventoryEvidence(listOf("Soothing Incense" to 4))
        bests.record("CAPTURE! You caught a Doomspiral!", run, 1000)
        val saved = Properties(); bests.save(saved)
        val restored = SafariPersonalBests(); restored.load(saved)
        assertEquals(1000L, restored.time("Doomspiral")); assertEquals(800L, restored.time("Doomspiral", duo = true))
    }
    @Test fun `duo requests parse case insensitively and cannot suppress solo responses`() {
        for (word in listOf("doom", "wumpa")) {
            val duo = PartyCommand.fromChat("Party > Friend: !PB $word DUO")!!.command.forResponder("Me")
            val solo = PartyCommand.fromChat("Party > Friend: !pb $word")!!.command.forResponder("Me")
            assertTrue(duo.duo); assertNotEquals(duo.text, solo.text)
            val text = "Me's ${duo.pbName} Duo PB: 0:01.000"
            assertTrue(duo.matchesResponse(text)); assertFalse(solo.matchesResponse(text))
            assertFalse(duo.matchesResponse("Me's ${duo.pbName} PB: 0:01.000"))
        }
    }
    @Test fun `skull replies cover empty mutuals and wholly missing selected discovery sets`() {
        for (timesave in listOf(false, true)) {
            assertEquals("None. :skull:", SafariLookup.mutualResult(emptySet(), timesave))
            assertEquals("All. :skull:", SafariLookup.missingResult(emptySet(), timesave))
            assertNotEquals("All. :skull:", SafariLookup.missingResult(setOf("WUMPA"), timesave))
        }
        assertTrue(PartyCommand(PartyCommandKind.MUTUALS).matchesResponse("Mutual Sparkling Critters: None. :skull:"))
        assertTrue(PartyCommand(PartyCommandKind.MISSING, "Me").matchesResponse("Missing Timesave Sparklings for Me: All. :skull:"))
    }
}
