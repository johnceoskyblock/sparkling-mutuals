package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WumpaCaptureProofTest {
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"),mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
    }
    @Test fun `awakening repairs missing shared types without personal credit or full clear counts`() {
        val run=SafariRun(0);val state=party()
        assertEquals(0,run.count("Tepid"));assertFalse(run.uniqueComplete("Nozzlenose",state))
        run.recordWumpaSpawn("The Wumpa has awoken.")
        for (mob in SafariBiome.ICY.critters.filter { it.name != "Wumpa" }) {
            assertEquals(1,run.count(mob.name),mob.name)
            assertTrue(run.uniqueComplete(mob.name,state),mob.name)
            assertTrue(run.inferredCapture(mob.name))
            assertEquals(0,run.personalCount(mob.name))
            assertEquals(0,run.playerCount(mob.name,"Someone"))
            assertFalse(run.fullClearComplete(mob.name),mob.name)
        }
        assertTrue(run.catches.isEmpty())
        assertEquals(0,run.count("Wumpa"));assertEquals(0,run.count("Foxtrot"))
        assertFalse(run.uniqueComplete("Wumpa",state))
        run.record(SafariCatch(SafariRoster.named("Tepid")!!,false,"Other"))
        assertEquals(1,run.count("Tepid"));assertFalse(run.inferredCapture("Tepid"))
        run.record(SafariCatch(SafariRoster.named("Tepid")!!,false,"Other"))
        assertEquals(2,run.count("Tepid"))
        run.recordWumpaSpawn("The Wumpa has awoken.")
        assertEquals(2,run.count("Tepid"))
    }
    @Test fun `Wumpa catch supplies same proof but player quoted messages never do`() {
        val run=SafariRun(0)
        run.recordWumpaSpawn("Party > Other: The Wumpa has awoken.")
        assertEquals(0,run.count("Tepid"))
        run.record(SafariCatch(SafariRoster.named("Wumpa")!!,false,"Other"))
        assertEquals(1,run.count("Tepid"));assertEquals(1,run.count("Nozzlenose"))
        assertEquals(1,run.count("Wumpa"))
        assertEquals(0,SafariRun(10000).count("Tepid"))
    }
}
