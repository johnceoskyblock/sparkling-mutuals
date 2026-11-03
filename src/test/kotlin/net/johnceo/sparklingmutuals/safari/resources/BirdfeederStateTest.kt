package net.johnceo.sparklingmutuals.safari.resources

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BirdfeederStateTest {
    @Test fun `slot packets and spawn messages count a consumption once in either order`() {
        val slotFirst=BirdfeederState().apply { observe("Bag of Seeds",3,0) }
        slotFirst.observe("Bag of Seeds",2,100)
        slotFirst.spawn(150)
        assertEquals(2,slotFirst.count)
        val chatFirst=BirdfeederState().apply { observe("Bag of Seeds",3,0) }
        chatFirst.spawn(100)
        chatFirst.observe("Bag of Seeds",3,150) // Unchanged stale contents cannot restore consumed food.
        assertEquals(2,chatFirst.count)
        chatFirst.observe("Bag of Seeds",2,200)
        assertEquals(2,chatFirst.count)
        chatFirst.spawn(300)
        assertEquals(1,chatFirst.count)
        chatFirst.observe(null,0,400)
        chatFirst.spawn(450)
        assertEquals(0,chatFirst.count)
        assertNull(chatFirst.food)
        chatFirst.newMenu()
        chatFirst.observe("Bag of Seeds",3,500)
        assertEquals(3,chatFirst.count)
    }
    @Test fun `closing rejected cursor food remains held until server returns it without double counting other stacks`() {
        val guard=BirdCursorGuard()
        val cursor=mapOf("Bag of Seeds" to 1)
        assertEquals(cursor,guard.extra(mapOf("Bag of Seeds" to 3),cursor,true,"Yogi Berry",0))
        assertEquals(cursor,guard.extra(mapOf("Bag of Seeds" to 3),emptyMap(),false,null,5000))
        assertTrue(guard.extra(mapOf("Bag of Seeds" to 4),emptyMap(),false,null,6000).isEmpty())
        assertTrue(guard.extra(mapOf("Bag of Seeds" to 4),emptyMap(),false,null,7000).isEmpty())
    }
    @Test fun `accepted cursor transfers and expired resync guards cannot retain phantom food`() {
        val guard=BirdCursorGuard()
        guard.extra(emptyMap(),mapOf("Wriggleworm" to 2),true,"Wriggleworm",0)
        assertEquals(2,guard.extra(emptyMap(),emptyMap(),true,"Wriggleworm",100)["Wriggleworm"])
        assertTrue(guard.extra(emptyMap(),emptyMap(),true,"Wriggleworm",251).isEmpty())
        assertTrue(guard.extra(emptyMap(),emptyMap(),false,null,300).isEmpty())
        guard.extra(emptyMap(),mapOf("Wriggleworm" to 2),true,"Yogi Berry",1000)
        assertEquals(2,guard.extra(emptyMap(),emptyMap(),false,null,2000)["Wriggleworm"])
        assertTrue(guard.extra(emptyMap(),emptyMap(),false,null,13001).isEmpty())
    }
}
