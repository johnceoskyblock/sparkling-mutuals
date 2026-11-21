package net.johnceo.sparklingmutuals.safari.completion

import java.util.Properties
import net.johnceo.sparklingmutuals.commands.PartyCommand
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
        assertTrue(first.changed)
        assertEquals(BossPbCategory.OBSERVED_SOLO, first.category)
        assertEquals("[SM] JohnFounder caught Doomspiral in 1:00.000 — Observed Solo, new record (PB: 1:00.000)", first.notice)
        assertNull(bests.time("Doomspiral", duo = true))
        val slower = bests.captureFeedback("LOOT SHARE! You received a Doomspiral Shard from johnfounder catching a Doomspiral!", run, 66000, "Me")!!
        assertFalse(slower.changed)
        assertEquals("[SM] JohnFounder caught Doomspiral in 1:05.000 — Observed Solo (PB: 1:00.000)", slower.notice)
        val saved = Properties(); bests.save(saved)
        val restored = SafariPersonalBests(); restored.load(saved)
        assertTrue(restored.trackedResponse("JOHNFOUNDER", "Doomspiral").endsWith("1:00.000"))
        assertTrue(restored.trackedResponse("NeverCaught", "Wumpa").contains("Not recorded yet"))
        assertTrue(saved.stringPropertyNames().any { it.contains("nevercaught") })
    }
    @Test fun `personal feedback shows the best time within the matching category`() {
        val bests = SafariPersonalBests(); val run = SafariRun(0)
        val line = "CAPTURE! You caught a Doomspiral!"
        assertEquals(BossPbCategory.DUO, bests.captureFeedback(line, run, 1000, "Me")!!.category)
        run.inventoryEvidence(listOf("Soothing Incense" to 4))
        val solo = bests.captureFeedback(line, run, 2000, "Me")!!
        assertEquals(BossPbCategory.SOLO, solo.category)
        assertTrue(solo.notice.endsWith("Solo, new record (PB: 0:02.000)"))
        val slower = bests.captureFeedback(line, run, 2500, "Me")!!
        assertTrue(slower.notice.endsWith("Solo (PB: 0:02.000)"))
        assertFalse(slower.changed)
        val faster = bests.captureFeedback(line, run, 1500, "Me")!!
        assertTrue(faster.notice.endsWith("Solo, new record (PB: 0:01.500)"))
        assertTrue(faster.changed)
        val tied = bests.captureFeedback(line, run, 1500, "Me")!!
        assertTrue(tied.notice.endsWith("Solo (PB: 0:01.500)"))
        assertFalse(tied.changed)
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
