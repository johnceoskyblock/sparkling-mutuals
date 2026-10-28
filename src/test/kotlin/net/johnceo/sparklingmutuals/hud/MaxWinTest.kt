package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.safari.resources.SafariResources
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MaxWinTest {
    @Test fun `seven coin maximum works with ordinary and first class tickets once per run`() {
        for (first in listOf(false,true,null)) {
            val state = MaxWinState(); val run = Any()
            assertFalse(state.poll(run,6,2,5,1,first,0))
            assertTrue(state.poll(run,7,2,5,0,first,1000))
            assertEquals(7,state.coins)
            assertFalse(state.poll(run,7,2,5,0,first,2000))
            assertTrue(state.visible(5999)); assertFalse(state.visible(6000))
            assertTrue(state.gain(120000)>0)
            state.poll(run,1,2,5,0,first,121000)
            assertTrue(state.gain(121000)>0)
            state.poll(run,0,2,5,0,first,122000)
            assertEquals(0f,state.gain(122000))
            assertFalse(state.poll(run,7,2,5,0,first,123000))
            assertTrue(state.poll(Any(),7,2,5,0,first,124000))
        }
    }
    @Test fun `eight coin run waits for all eight rather than firing at seven`() {
        val state=MaxWinState();val run=Any()
        assertFalse(state.poll(run,7,3,5,1,true,0))
        assertFalse(state.poll(run,7,3,5,0,true,500))
        assertTrue(state.poll(run,8,3,5,0,true,1000))
        assertEquals(8,state.coins)
        assertFalse(state.poll(run,8,3,5,0,true,2000))
        assertTrue(state.gain(5000)>state.gain(10000))
        assertTrue(state.gain(10000)>state.gain(60000))
        assertTrue(state.gain(60000)>state.gain(300000))
        assertTrue(state.gain(300000)>0)
        state.poll(null,8,3,5,0,true,301000)
        assertFalse(state.visible(301000));assertEquals(0f,state.gain(301000))
    }
    @Test fun `teammate starting coins never enter the personal maximum`() {
        val resources=SafariResources()
        resources.inventory(listOf("Shining Coin" to 2),initial=true)
        resources.partyNotice("Party > Friend: [SM] Starting items: 3x Shining Coin")
        assertEquals(3,resources.coinContributions().values.sum())
        assertEquals(2,resources.startingCount(FloorDropType.COIN))
        val state=MaxWinState()
        assertTrue(state.poll(Any(),7,resources.startingCount(FloorDropType.COIN),5,0,true,0))
        assertEquals(7,state.coins)
    }
    @Test fun `unknown sources and incomplete collection do not create a false maximum`() {
        val state=MaxWinState();val run=Any()
        assertFalse(state.poll(run,7,0,0,0,null,0))
        assertFalse(state.poll(run,7,2,5,1,false,1))
        assertFalse(state.poll(run,7,1,5,0,false,2))
        assertTrue(state.poll(run,7,2,5,0,false,3))
        state.poll(run,0,2,5,0,false,4)
        assertTrue(state.visible(4))
        state.reset();assertFalse(state.visible(5));assertEquals(0f,state.gain(5))
    }
}
