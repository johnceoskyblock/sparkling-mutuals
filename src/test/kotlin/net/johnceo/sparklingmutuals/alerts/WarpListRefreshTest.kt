package net.johnceo.sparklingmutuals.alerts

import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WarpListRefreshTest {
    @Test fun `daily checks persist through reload and are independent for new players`() {
        WarpList.load(Properties())
        WarpList.addPlayer("Alice")
        assertTrue(WarpList.refreshDue("alice",1000))
        WarpList.checked("ALICE",1000,"00000000-0000-0000-0000-000000000001")
        val saved=Properties().also(WarpList::save)
        WarpList.load(saved)
        assertFalse(WarpList.refreshDue("Alice",1000+WarpList.DISCOVERY_REFRESH_MS-1))
        assertTrue(WarpList.refreshDue("Alice",1000+WarpList.DISCOVERY_REFRESH_MS))
        assertEquals("00000000000000000000000000000001",WarpList.uuid("alice"))
        WarpList.addPlayer("Bob")
        assertTrue(WarpList.refreshDue("Bob",1001))
        WarpList.removePlayer("alice");WarpList.addPlayer("Alice")
        assertTrue(WarpList.refreshDue("Alice",1001))
    }
    @Test fun `acquired flags are removed and remaining per-critter priorities close gaps preserving ties`() {
        WarpList.load(Properties())
        listOf("Alice","Bob","Carol","Dan").forEach { WarpList.select(it,"Foxtrot",true) }
        WarpList.setPriority("Alice","Foxtrot",1)
        WarpList.setPriority("Bob","Foxtrot",3)
        WarpList.setPriority("Carol","Foxtrot",3)
        WarpList.setPriority("Dan","Foxtrot",10)
        WarpList.select("Alice","Mantis Shrimp",true)
        WarpList.select("Alice","Polaris",true)
        WarpList.setPriority("Alice","Polaris",7)
        WarpList.configure("Alice",WarpList.InviteSettings(alwaysInvite=true))
        assertEquals(setOf("Foxtrot","Mantis Shrimp"),WarpList.removeAcquired("ALICE",setOf("FOXTROT","MANTIS_SHRIMP")))
        assertFalse(WarpList.selected("Alice","Foxtrot"))
        assertFalse(WarpList.selected("Alice","Mantis Shrimp"))
        assertTrue(WarpList.selected("Alice","Polaris"))
        assertEquals(7,WarpList.priority("Alice","Polaris"))
        assertEquals(1,WarpList.priority("Bob","Foxtrot"))
        assertEquals(1,WarpList.priority("Carol","Foxtrot"))
        assertEquals(2,WarpList.priority("Dan","Foxtrot"))
        assertTrue(WarpList.settings("Alice").alwaysInvite)
        WarpList.load(Properties().also(WarpList::save))
        assertFalse(WarpList.selected("Alice","Foxtrot"))
        assertEquals(2,WarpList.priority("Dan","Foxtrot"))
    }
    @Test fun `empty discoveries and an attempted check never erase invite selections`() {
        WarpList.load(Properties())
        WarpList.select("Alice","Flitter",true)
        WarpList.setPriority("Alice","Flitter",6)
        assertTrue(WarpList.removeAcquired("Alice",emptySet()).isEmpty())
        WarpList.checked("Alice",1234)
        assertTrue(WarpList.selected("Alice","Flitter"))
        assertEquals(6,WarpList.priority("Alice","Flitter"))
        assertFalse(WarpList.refreshDue("Alice",1235))
    }
}
