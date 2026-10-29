package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BirdFoodSparklingCheckTest {
    @Test fun `final stack consumption waits for one spawn message per food not per Macaw`() {
        val check=BirdFoodSparklingCheck()
        check.floor(1);check.inventory(listOf("Bag of Seeds" to 3),0)
        check.floor(0);check.inventory(emptyList(),1000)
        assertEquals(0,check.leftToUse);assertEquals(3,check.pendingSpawns);assertFalse(check.complete)
        check.spawn("A Bluebird was attracted to the Birdfeeder!",1100)
        check.spawn("Two Macaws were attracted to the Birdfeeder!",1200)
        assertFalse(check.complete);assertEquals(1,check.pendingSpawns)
        check.spawn("A Parakeet was attracted to the Birdfeeder!",1300)
        assertTrue(check.complete)
        check.inventory(listOf("Yogi Berry" to 1),1400)
        assertFalse(check.complete)
        check.inventory(emptyList(),1500)
        assertFalse(check.complete)
        check.spawn("A Bluebird was attracted to the Birdfeeder!",1600)
        assertTrue(check.complete)
    }
    @Test fun `spawn before inventory packet is matched but old or relayed chat is not`() {
        val check=BirdFoodSparklingCheck()
        check.floor(0);check.inventory(listOf("Wriggleworm" to 1),1000)
        assertFalse(check.spawn("Party > Other: A Bluebird was attracted to the Birdfeeder!",1050))
        check.spawn("A Bluebird was attracted to the Birdfeeder!",1100)
        check.inventory(emptyList(),1150)
        assertTrue(check.complete)
        val stale=BirdFoodSparklingCheck()
        stale.floor(0);stale.spawn("A Bluebird was attracted to the Birdfeeder!",0)
        stale.inventory(listOf("Wriggleworm" to 1),5000);stale.inventory(emptyList(),5100)
        assertFalse(stale.complete)
    }
    @Test fun `party food reports do not raise local Sparkling demand`() {
        val run=SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(listOf("Bag of Seeds" to 1),0)
        run.scanBirdSources(0)
        run.resources.partyNotice("Party > Friend: [SM] Starting items: 9x Bag of Seeds")
        run.inventoryEvidence(emptyList(),1000)
        assertFalse(run.birdSpawnsComplete())
        run.birdFoodCheck.spawn("A Bluebird was attracted to the Birdfeeder!",1100)
        assertTrue(run.birdSpawnsComplete())
        assertEquals(0,run.birdFoodLeftToUse)
        assertEquals(0,run.count("Bluebird"))
    }
    @Test fun `final food batch does not inherit a missed older spawn announcement`() {
        val check=BirdFoodSparklingCheck()
        check.floor(0);check.inventory(listOf("Yogi Berry" to 3),0)
        check.inventory(listOf("Yogi Berry" to 2),1000)
        assertEquals(1,check.pendingSpawns)
        check.inventory(emptyList(),5000)
        assertEquals(2,check.pendingSpawns)
        check.spawn("A Bluebird was attracted to the Birdfeeder!",5100)
        assertFalse(check.complete)
        check.spawn("A Parakeet was attracted to the Birdfeeder!",5200)
        assertTrue(check.complete)
    }
}
