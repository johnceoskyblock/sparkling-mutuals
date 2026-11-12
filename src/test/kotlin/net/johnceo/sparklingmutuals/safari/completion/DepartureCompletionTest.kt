package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DepartureCompletionTest {
    private fun party(needed: Set<String> = emptySet()) = PartySparklingState().apply {
        select(setOf("me"))
        accept(setOf("me"),mapOf("me" to SafariRoster.all.map { it.name }.toSet()-needed))
    }
    private fun catch(run: SafariRun,name: String) = run.record(SafariCatch(SafariRoster.named(name)!!),1000)
    @Test fun `last bird spawn can finish a locally verified Forest after departure`() {
        var now=1000L
        val run=SafariRun(0) { now }
        val party=party(setOf("Bluebird"))
        run.visitBiome(SafariBiome.FOREST)
        run.updateCaptureEvidence(SafariBiome.FOREST,emptySet(),false)
        run.inventoryEvidence(listOf("Bag of Seeds" to 9),1000)
        run.birdFoodCheck.floor(0)
        run.inventoryEvidence(emptyList(),1100)
        repeat(8) { run.birdFoodCheck.spawn("A Bluebird was attracted to the Birdfeeder!",1200) }
        assertNull(run.completion.announcement(run,SafariBiome.FOREST,SafariMode.SPARKLING,party))
        assertTrue(run.completion.pendingDeparture(SafariBiome.FOREST))
        run.visitBiome(SafariBiome.CAVERN)
        now+=31_000 // A queued feeder batch is not discarded because travel took a while.
        assertNull(run.completion.announcement(run,SafariBiome.FOREST,SafariMode.SPARKLING,party))
        run.birdFoodCheck.spawn("A Bluebird was attracted to the Birdfeeder!",1300)
        assertEquals("fd",run.completion.announcement(run,SafariBiome.FOREST,SafariMode.SPARKLING,party))
        run.completion.sent(SafariBiome.FOREST,SafariMode.SPARKLING)
        assertNull(run.completion.announcement(run,SafariBiome.FOREST,SafariMode.SPARKLING,party))
        assertTrue(run.completion.pendingDeparture(SafariBiome.FOREST))
    }
    @Test fun `Hideyho reward after leaving sends Haunted only when the rest was verified`() {
        for(missingOther in listOf(false,true)) {
            val run=SafariRun(0) { 1000 }
            val party=party()
            run.visitBiome(SafariBiome.HAUNTED)
            run.updateCaptureEvidence(SafariBiome.HAUNTED,emptySet(),false)
            SafariBiome.HAUNTED.critters.filter { it.name!="Hideyho" && (!missingOther || it.name!="Areita") }
                .forEach { catch(run,it.name) }
            run.completion.hideyhoDialogue(1000)
            assertNull(run.completion.announcement(run,SafariBiome.HAUNTED,SafariMode.UNIQUE,party))
            run.visitBiome(SafariBiome.CAVERN)
            assertNull(run.completion.announcement(run,SafariBiome.HAUNTED,SafariMode.UNIQUE,party))
            catch(run,"Hideyho")
            assertEquals(if(missingOther) null else "hd",
                run.completion.announcement(run,SafariBiome.HAUNTED,SafariMode.UNIQUE,party))
        }
    }
    @Test fun `late remote Hideyho capture without local dialogue cannot create a report`() {
        val run=SafariRun(0)
        val party=party()
        run.visitBiome(SafariBiome.HAUNTED)
        run.updateCaptureEvidence(SafariBiome.HAUNTED,emptySet(),false)
        SafariBiome.HAUNTED.critters.filter { it.name!="Hideyho" }.forEach { catch(run,it.name) }
        run.completion.announcement(run,SafariBiome.HAUNTED,SafariMode.UNIQUE,party)
        run.visitBiome(SafariBiome.CAVERN);catch(run,"Hideyho")
        assertNull(run.completion.announcement(run,SafariBiome.HAUNTED,SafariMode.UNIQUE,party))
    }
}
