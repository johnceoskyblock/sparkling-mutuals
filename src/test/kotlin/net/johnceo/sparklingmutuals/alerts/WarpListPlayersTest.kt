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
    @Test fun `CSV imports merge flags without duplicating names or overwriting preferences`() {
        WarpList.load(Properties());WarpList.addPlayer("OldPlayer");WarpList.select("OldPlayer","Foxtrot",true)
        val preferences = WarpList.InviteSettings(priority = 2, alwaysInvite = true)
        WarpList.configure("OLDPLAYER", preferences)
        val imported=WarpListCsv.read(StringReader("Foxtrot,Polaris\nAlice,Bob\noldplayer,\n"))
        WarpList.append(imported)
        WarpList.append(imported)
        WarpList.load(Properties().also(WarpList::save))
        assertEquals(setOf("OldPlayer","Bob","Alice"),WarpList.players().toSet())
        assertEquals(listOf("OldPlayer","Alice"),WarpList.names("Foxtrot"))
        assertEquals(listOf("Bob"),WarpList.names("Polaris"))
        assertEquals(preferences, WarpList.settings("OldPlayer"))
        assertEquals(WarpList.InviteSettings(), WarpList.settings("Alice"))
        WarpList.append(WarpListCsv.read(StringReader("Foxtrot,Polaris\nOLDPLAYER,\n,OldPlayer\n")))
        WarpList.load(Properties().also(WarpList::save))
        assertEquals(1, WarpList.priority("OldPlayer", "Foxtrot"))
        assertEquals(2, WarpList.priority("OldPlayer", "Polaris"))
        assertEquals(listOf("OldPlayer","Alice"), WarpList.names("Foxtrot"))
        assertEquals(setOf("OldPlayer","Bob","Alice"),WarpList.players().toSet())
        assertEquals(preferences, WarpList.settings("OldPlayer"))
        WarpList.setPriority("OldPlayer", "Foxtrot", 7)
        WarpList.select("OldPlayer", "Foxtrot", false)
        WarpList.load(Properties().also(WarpList::save))
        assertEquals(7, WarpList.priority("OldPlayer", "Foxtrot"))
        assertEquals(2, WarpList.priority("OldPlayer", "Polaris"))
        assertFalse(WarpList.selected("OldPlayer", "Foxtrot"))
    }
}
