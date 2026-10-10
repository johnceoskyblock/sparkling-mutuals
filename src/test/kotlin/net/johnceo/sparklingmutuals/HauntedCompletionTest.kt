package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class HauntedCompletionTest {
    @Test fun `Haunted zones require minimum UUIDs and correct boundary`() {
        for (name in listOf("Areita", "Bloodbat", "Solsnatcher", "Litterbug", "Duplico", "Hideonwall")) {
            val run = SafariRun(0)
            repeat(SafariFullClear.minimum(name)) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
            run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), false, playerX=27.01, playerZ=-63.0)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.HAUNTED, setOf(name), false, playerX=27.0, playerZ=-63.0)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), false, playerX=27.0, playerZ=-63.0)
            assertTrue(run.captureComplete(name))
            val seen = List(SafariFullClear.minimum(name)) { name to UUID.randomUUID() }
            repeat(4) { run.sparklingChecks.scan(SafariBiome.HAUNTED,-3.0,-63.0,seen.take(1)) }
            assertFalse(run.sparklingChecks.checked(name))
            run.sparklingChecks.scan(SafariBiome.HAUNTED,37.01,-63.0,seen)
            assertFalse(run.sparklingChecks.checked(name))
            run.sparklingChecks.scan(SafariBiome.HAUNTED,37.0,-63.0,emptyList())
            assertTrue(run.sparklingChecks.checked(name))
        }
    }
    @Test fun `Gazer Hideyho and Doomspiral require unique evidence without zones`() {
        for ((name,amount) in mapOf("Gazer" to 4,"Hideyho" to 1,"Doomspiral" to 1)) {
            val run=SafariRun(0); val ids=List(amount) { UUID.randomUUID() }
            repeat(amount) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
            run.updateCaptureEvidence(SafariBiome.HAUNTED,emptySet(),false,playerX=500.0,playerZ=500.0)
            assertFalse(run.captureComplete(name))
            ids.forEachIndexed { i,id -> run.observeCritter(i,name,id) }
            run.updateCaptureEvidence(SafariBiome.HAUNTED,emptySet(),false,playerX=500.0,playerZ=500.0)
            assertTrue(run.captureComplete(name))
            val fresh=SafariRun(0)
            fresh.sparklingChecks.scan(SafariBiome.HAUNTED,500.0,500.0,ids.map { name to it })
            assertEquals(name!="Doomspiral",fresh.sparklingChecks.checked(name))
            fresh.record(SafariCatch(SafariRoster.named(name)!!))
            fresh.sparklingChecks.scan(SafariBiome.HAUNTED,500.0,500.0,emptyList())
            assertTrue(fresh.sparklingChecks.checked(name))
        }
    }
    @Test fun `six picked coins require six unique Gimmiegolds and six Full Clear captures`() {
        val run=SafariRun(0); val seen=List(6) { "Gimmiegold" to UUID.randomUUID() }
        repeat(6) { run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!"); run.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!") }
        run.sparklingChecks.inventory(emptyList())
        run.sparklingChecks.scan(SafariBiome.HAUNTED,-4.0,-64.0,seen.take(3),hauntedDropsRemaining=0)
        assertFalse(run.sparklingChecks.checked("Gimmiegold"))
        run.sparklingChecks.scan(SafariBiome.HAUNTED,-4.0,-64.0,seen,hauntedDropsRemaining=0)
        assertTrue(run.sparklingChecks.checked("Gimmiegold"))
        seen.forEachIndexed { i,p -> run.observeCritter(i,p.first,p.second) }
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!)) }
        run.updateCaptureEvidence(SafariBiome.HAUNTED,emptySet(),false,playerX=-4.0,playerZ=-64.0)
        assertFalse(run.captureComplete("Gimmiegold"))
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!)) }
        assertTrue(run.captureComplete("Gimmiegold"))
        run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
        assertFalse(run.sparklingChecks.checked("Gimmiegold"))
        assertFalse(run.captureComplete("Gimmiegold"))
    }
    @Test fun `Haunted profitable ESP persists and Doomspiral remains until capture`() {
        fun party(name:String)=PartySparklingState().apply { select(setOf("me"));accept(setOf("me"),mapOf("me" to SafariRoster.all.map { it.name }.toSet()-name)) }
        val run=SafariRun(0);val id=UUID.randomUUID()
        assertTrue(SafariEspRules.neededForSparkling("Hideonwall",false,party("Hideonwall"),true,run,id,0))
        assertTrue(SafariEspRules.neededForSparkling("Hideonwall",false,party("Hideonwall"),true,run,id,10000))
        assertTrue(SafariEspRules.neededForSparkling("Doomspiral",false,party("Doomspiral"),false,run,id,0))
        assertTrue(SafariEspRules.neededForSparkling("Doomspiral",false,party("Doomspiral"),false,run,id,20000))
        run.record(SafariCatch(SafariRoster.named("Doomspiral")!!))
        assertFalse(SafariEspRules.neededForSparkling("Doomspiral",false,party("Doomspiral"),false,run,id,20001))
    }
}
