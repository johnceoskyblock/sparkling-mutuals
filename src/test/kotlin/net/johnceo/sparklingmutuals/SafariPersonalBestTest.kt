package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class SafariPersonalBestTest {
    private fun soloRun(start: Long) = SafariRun(start).apply {
        inventoryEvidence(listOf("Soothing Incense" to 4))
        SafariBiome.ICY.critters.filter { it.name != "Wumpa" }.forEach { record(SafariCatch(it)) }
    }
    @Test fun `new best notice is only produced for your own faster capture`() {
        val bests = SafariPersonalBests()
        val run = soloRun(1000)
        assertNull(bests.newBest("LOOT SHARE! You received a Doomspiral Shard from Other catching a Doomspiral!", run, 2000))
        assertNull(bests.newBest("Party > Other: CAPTURE! You caught a Wumpa!", run, 2000))
        assertEquals("[SM] New Doomspiral PB: 1:01.000!", bests.newBest("CAPTURE! You caught a Doomspiral!", run, 62000))
        assertNull(bests.newBest("CAPTURE! You caught a Doomspiral!", run, 62000))
        assertNull(bests.newBest("CAPTURE! You caught a Doomspiral!", run, 63000))
        assertEquals("[SM] New Wumpa PB: 1:02.000!", bests.newBest("CAPTURE! You caught a Wumpa!", run, 63000))
        assertEquals(61000L, bests.time("Doomspiral"))
        assertEquals(62000L, bests.time("Wumpa"))
    }
    @Test fun `only own exact successful captures set independent personal bests`() {
        val bests = SafariPersonalBests()
        val run = soloRun(1000)
        assertFalse(bests.record("LOOT SHARE! You received a Wumpa Shard from Test catching a Wumpa!", run, 2000))
        assertFalse(bests.record("Party > Test: CAPTURE! You caught a Doomspiral!", run, 2000))
        assertFalse(bests.record("CAPTURE! You caught a DoomspiralFake!", run, 2000))
        assertTrue(bests.record("§aCAPTURE! You caught a Doomspiral and gained a Shard!", run, 90500))
        assertTrue(bests.record("CAPTURE! You caught a Wumpa!", run, 121000))
        assertEquals(89500L, bests.time("Doomspiral"))
        assertEquals(120000L, bests.time("Wumpa"))
        assertFalse(bests.record("CAPTURE! You caught a Doomspiral!", run, 100000))
        assertTrue(bests.record("CAPTURE! You caught a Doomspiral!", soloRun(200000), 260000))
        assertEquals(60000L, bests.time("Doomspiral"))
    }
    @Test fun `personal bests survive configuration reload`() {
        val bests = SafariPersonalBests()
        bests.record("CAPTURE! You caught a Wumpa!", soloRun(1000), 63250)
        val properties = Properties()
        bests.save(properties)
        val restored = SafariPersonalBests()
        restored.load(properties)
        assertEquals(62250L, restored.time("Wumpa"))
        assertEquals("Player's Wumpa PB: 1:02.250", restored.response("Player", "Wumpa"))
        assertEquals("Player's Doomspiral PB: Not recorded yet", restored.response("Player", "Doomspiral"))
    }
    @Test fun `pb party commands are case insensitive and coordinate separately for each responder`() {
        val doom = PartyCommand.fromChat("Party > [VIP] Test: !PB DoOm")!!.command.forResponder("Player")
        val wumpa = PartyCommand.fromChat("Party > Test: !pB WuMpA")!!.command.forResponder("Player")
        assertEquals(PartyCommandKind.PB_DOOM, doom.kind)
        assertEquals(PartyCommandKind.PB_WUMPA, wumpa.kind)
        assertTrue(doom.matchesResponse("Player's Doomspiral PB: 1:02.250"))
        assertFalse(doom.matchesResponse("Player's Wumpa PB: 1:02.250"))
        assertTrue(wumpa.matchesResponse("Player's Wumpa PB: Not recorded yet"))
        assertFalse(doom.matchesResponse("Other's Doomspiral PB: 1:02.250"))
        assertFalse(wumpa.matchesResponse("Other's Wumpa PB: Not recorded yet"))
        assertEquals(PartyCommand(PartyCommandKind.TICKETS, "Test"),
            PartyCommand.fromChat("Party > Requester: !tickets Test")!!.command.forResponder("Player"))
        assertNull(PartyCommand.fromChat("Party > Test: !pb doom extra"))
    }
}
