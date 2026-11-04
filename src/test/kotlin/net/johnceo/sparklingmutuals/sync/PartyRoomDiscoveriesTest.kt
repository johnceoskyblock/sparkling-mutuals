package net.johnceo.sparklingmutuals.sync

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartyRoomDiscoveriesTest {
    @Test fun `atomic found and caught apply before entry permission`() {
        val room=PartyRoomDiscoveries()
        assertNull(room.snapshot(1))
        room.welcome(1,true)
        assertNull(room.snapshot(1))
        assertTrue(room.receive(1,"3,0,1,0"))
        assertEquals(setOf("Flitter"),room.snapshot(1)!!.active)
        // No wall-clock age belongs to the new room criterion.
        assertEquals(setOf("Flitter"),room.snapshot(1)!!.active)
        room.receive(1,"3,0,3,0")
        assertTrue(room.snapshot(1)!!.active.isEmpty())
    }
    @Test fun `reconnect needs new snapshot and retains caught negatives`() {
        val room=PartyRoomDiscoveries();room.welcome(1,true);room.receive(1,"3,0,1,0")
        room.welcome(2,true)
        assertNull(room.snapshot(2));assertNull(room.snapshot(1))
        assertFalse(room.receive(1,"7,0,0,0"))
        room.receive(2,"3,0,0,0")
        assertEquals(setOf("Flitter"),room.snapshot(2)!!.active)
        room.reset();room.welcome(3,true);room.receive(3,"1,0,0,0")
        assertEquals(setOf("Cavernfish"),room.snapshot(3)!!.active)
    }
    @Test fun `missing capability and malformed room data never retain permission`() {
        val room=PartyRoomDiscoveries();room.welcome(1,false)
        assertFalse(room.receive(1,"1,0,0,0"));assertNull(room.snapshot(1))
        room.welcome(2,true);room.receive(2,"1,0,0,0")
        for (bad in listOf("-1,0,0,0","1,128,0,0","1,0,0","x,0,0,0")) {
            assertFalse(room.receive(2,bad));assertNull(room.snapshot(2))
        }
    }
    @Test fun `all upstream wire positions decode the correct caught species`() {
        val names=listOf("Cavernfish","Flitter","Shyworm","Driftling","Chuckwalla","Rockmite","Scrappy","Snoozle","Gemzie",
            "Strongarm","Tepid","Polaris","Shuddersquid","Billygoat","Mantis Shrimp","Nozzlenose","Troodon","Wumpa",
            "Areita","Bloodbat","Duplico","Gazer","Litterbug","Solsnatcher","Gimmiegold","Hideonwall","Hideyho","Doomspiral",
            "Foxtrot","Bluebird","Honeybug","Treefrog","Woodchucker","Fluffling","Hideonfloor","Parakeet","Macaw")
        names.forEachIndexed { i,name ->
            val room=PartyRoomDiscoveries();room.welcome(1,true)
            val low=if(i<30) 1 shl i else 0;val high=if(i>=30) 1 shl(i-30) else 0
            room.receive(1,"$low,$high,0,0")
            assertEquals(setOf(name),room.snapshot(1)!!.active,"wire index $i")
            room.receive(1,"$low,$high,$low,$high")
            assertTrue(room.snapshot(1)!!.active.isEmpty())
        }
    }
}
