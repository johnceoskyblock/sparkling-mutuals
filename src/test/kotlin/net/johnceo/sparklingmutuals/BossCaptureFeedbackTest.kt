package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.commands.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class BossCaptureFeedbackTest {
    @Test fun `named boss requests preserve targets and distinguish observed records`() {
        val request = PartyCommand.fromChat("§aParty > §bFriend§f: !PB doom JohnFounder")!!
        val command = request.command.forResponder("Me")
        assertEquals("JohnFounder", command.ign)
        assertEquals("!pb doom JohnFounder", command.text)
        assertTrue(command.matchesResponse("JohnFounder's Doomspiral Observed PB: 1:00.000"))
        assertFalse(command.matchesResponse("Me's Doomspiral PB: 1:00.000"))
        assertNotEquals(request.token, PartyCommand.fromChat("Party > Friend: !pb doom Another")!!.token)
        assertTrue(PartyCommand.fromChat("Party > Friend: !pb doom duo")!!.command.duo)
    }
    @Test fun `loot share times use local run origin and persist case insensitive party history`() {
        val bests = SafariPersonalBests(); val run = SafariRun(1000)
        bests.rememberParty(listOf("JohnFounder", "NeverCaught"))
        val first = bests.captureFeedback("LOOT SHARE! You received a Doomspiral Shard from JohnFounder catching a Doomspiral!", run, 61000, "Me")!!
        assertTrue(first.changed); assertTrue(first.notice.contains("1:00.000"))
        assertNull(bests.time("Doomspiral", duo = true))
        val slower = bests.captureFeedback("LOOT SHARE! You received a Doomspiral Shard from johnfounder catching a Doomspiral!", run, 66000, "Me")!!
        assertFalse(slower.changed); assertTrue(slower.notice.contains("5.000s slower"))
        val saved = Properties(); bests.save(saved)
        val restored = SafariPersonalBests(); restored.load(saved)
        assertTrue(restored.trackedResponse("JOHNFOUNDER", "Doomspiral").endsWith("1:00.000"))
        assertTrue(restored.trackedResponse("NeverCaught", "Wumpa").contains("Not recorded yet"))
        assertTrue(saved.stringPropertyNames().any { it.contains("nevercaught") })
    }
    @Test fun `personal feedback selects solo when eligible and compares previous best even without improvement`() {
        val bests = SafariPersonalBests(); val run = SafariRun(0)
        assertTrue(bests.captureFeedback("CAPTURE! You caught a Doomspiral!", run, 1000, "Me")!!.notice.contains("Duo"))
        run.inventoryEvidence(listOf("Soothing Incense" to 4))
        val solo = bests.captureFeedback("CAPTURE! You caught a Doomspiral!", run, 2000, "Me")!!
        assertTrue(solo.notice.contains("Solo")); assertTrue(solo.notice.contains("first recorded"))
        val slower = bests.captureFeedback("CAPTURE! You caught a Doomspiral!", run, 2500, "Me")!!
        assertTrue(slower.notice.contains("Solo")); assertTrue(slower.notice.contains("0.500s slower"))
        assertFalse(slower.changed)
        val faster = bests.captureFeedback("CAPTURE! You caught a Doomspiral!", run, 1500, "Me")!!
        assertTrue(faster.notice.contains("0.500s faster")); assertTrue(faster.changed)
        assertEquals(1500L, bests.time("Doomspiral")); assertEquals(1000L, bests.time("Doomspiral", duo = true))
    }
    @Test fun `invalid capture sources and stale runs never create records or notices`() {
        val bests = SafariPersonalBests(); val run = SafariRun(1000)
        for (line in listOf("Party > Friend: LOOT SHARE! You received a Doomspiral Shard from Friend catching a Doomspiral!", "LOOT SHARE! You received a Doomspiral Shard from Friend catching a Gazer!", "CAPTURE! Friend caught a Wumpa!"))
            assertNull(bests.captureFeedback(line, run, 2000, "Me"))
        assertNull(bests.captureFeedback("CAPTURE! You caught a Wumpa!", run, 999, "Me"))
        run.endedAt=2000
        assertNull(bests.captureFeedback("CAPTURE! You caught a Wumpa!", run, 3000, "Me"))
    }
}
