package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.automation.GemzieGem
import net.johnceo.sparklingmutuals.safari.completion.GimmiegoldSparklingCheck
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ObjectiveEvidenceTest {
    @Test fun `gems acquired separately and placed podiums survive empty inventory`() {
        val state=GemzieObjective()
        GemzieGem.entries.forEach { state.inventory(listOf(it.label to 1)); state.inventory(emptyList()) }
        assertTrue(state.ready);assertFalse(state.opened)
        state.observe(GemzieGem.ORANGE,false,true)
        assertTrue(state.placed.isEmpty())
        GemzieGem.entries.forEach { state.observe(it,true,true) }
        assertTrue(state.opened)
        state.observe(GemzieGem.ORANGE,false,false)
        assertEquals(3,state.placed.size)
    }
    @Test fun `party starting messages cannot inflate coin minimums`() {
        val state=GimmiegoldSparklingCheck()
        state.floor(1,true);state.floor(2,true);state.floor(3,true)
        state.contributions(1,"self",mapOf("other" to 8,"self" to 7))
        assertEquals(4,state.required)
    }
    @Test fun `bird food counts feeder events and ignores party supplies`() {
        val state=SafariBirdLedger()
        state.startingGrant("Bag of Seeds")
        state.contributions("self",mapOf(("other" to FloorDropType.BERRY) to 5))
        assertEquals(10,state.feedingTarget)
        state.feeder("Bag of Seeds",2)
        assertFalse(state.spawn("Party > player: A Bluebird was attracted to the Birdfeeder!"))
        assertTrue(state.spawn("A Bluebird was attracted to the Birdfeeder!"))
        assertEquals(1,state.feederCount)
        assertTrue(state.spawn("Two Macaws were attracted to the Birdfeeder!"))
        assertEquals(2,state.spawnedCount("Macaw"))
        assertEquals(2,state.spawnEvents)
        assertEquals(0,state.feederCount)
    }
}
