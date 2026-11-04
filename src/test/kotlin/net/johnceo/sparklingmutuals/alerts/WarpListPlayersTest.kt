package net.johnceo.sparklingmutuals.alerts

import java.io.StringReader
import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WarpListPlayersTest {
    @Test fun `player flags reuse old critter lists and persist empty player cards`() {
        WarpList.load(Properties().apply { setProperty("warpList.Foxtrot","Alice Bob");setProperty("warpList.Polaris","Bob Alice") })
        assertEquals(setOf("Alice","Bob"),WarpList.players().toSet())
        WarpList.addPlayer("Carol")
        WarpList.select("alice","Foxtrot",false)
        WarpList.select("Carol","Tepid",true)
        val saved=Properties();WarpList.save(saved);WarpList.load(saved)
        assertFalse(WarpList.selected("ALICE","Foxtrot"))
        assertEquals(listOf("Bob","Alice"),WarpList.names("Polaris"),"Unedited critter invite order must remain intact")
        assertTrue(WarpList.selected("Carol","Tepid"))
        assertTrue(WarpList.players().contains("Alice"),"Disabling flags must not discard the player card")
        WarpList.removePlayer("bOB")
        assertFalse(WarpList.critters.any { WarpList.selected("Bob",it) })
    }
    @Test fun `CSV append retains existing cards and adds imported flags and row priorities`() {
        WarpList.load(Properties());WarpList.addPlayer("OldPlayer");WarpList.select("OldPlayer","Foxtrot",true)
        val imported=WarpListCsv.read(StringReader("Foxtrot,Polaris\nAlice,Bob\nBob,\n"))
        WarpList.append(imported)
        WarpList.append(imported)
        assertEquals(listOf("OldPlayer","Bob","Alice"),WarpList.players(),"Existing cards survive the new append import")
        assertEquals(listOf("OldPlayer","Alice","Bob"),WarpList.names("Foxtrot"))
        assertEquals(listOf("Bob"),WarpList.names("Polaris"))
        assertTrue(WarpList.players().contains("OldPlayer"))
        assertEquals(1,WarpList.priority("Alice","Foxtrot"))
        assertEquals(2,WarpList.priority("Bob","Foxtrot"))
    }
}
