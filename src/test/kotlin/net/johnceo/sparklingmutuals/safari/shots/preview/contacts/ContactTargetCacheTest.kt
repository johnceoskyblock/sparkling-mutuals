package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class ContactTargetCacheTest {
    @Test fun `metadata churn retains progress through more than one discovery allowance`() {
        val cache=ContactTargetCache();val world=Any()
        val slots=List(600){ContactDiscovery("local_$it",1)}.iterator()
        var inspected=0
        repeat(3) {
            cache.metadataChanged()
            cache.tick(world,slots){inspected++;null}
        }
        assertEquals(600,inspected)
        assertFalse(cache.snapshot().second.complete)
        cache.tick(world,emptyList<ContactDiscovery>().iterator()){null}
        assertTrue(cache.snapshot().second.complete)
    }
    @Test fun `late relevant critter replaces off path entries without exceeding slots or declaring coverage`() {
        var wanted="late"
        val cache=ContactTargetCache{if(it.key==wanted)0.0 else 100.0};val world=Any()
        val discoveries=List(400){ContactDiscovery(if(it==399)"late" else "local_$it",1)}
        val sweep=discoveries.iterator();var calls=0
        repeat(3){cache.tick(world,sweep){calls++;target(it)}}
        assertTrue(cache.snapshot().first.any{it.key=="late"})
        assertEquals(64,cache.snapshot().first.size);assertFalse(cache.snapshot().second.complete)
        assertTrue(calls<=3*240)
        wanted="local_398"
        val turned=discoveries.iterator()
        repeat(3){cache.tick(world,turned,::target)}
        assertTrue(cache.snapshot().first.any{it.key==wanted})
        assertFalse(cache.snapshot().second.complete)
    }
    private fun target(d:ContactDiscovery)=ContactTarget(d.key,d.generation,"Foxtrot",ShotBounds(ShotVector(0.0,0.0,0.0),ShotVector(1.0,1.0,1.0)),"client_body_v1")
    @Test fun `empty cache charges 240 discoveries and unfinished sweep is unknown`() {
        val c=ContactTargetCache();var calls=0
        c.tick(Any(),List(400){ContactDiscovery("local_$it",1)}.iterator()){calls++;null}
        assertEquals(240,calls);assertFalse(c.snapshot().second.complete)
    }
    @Test fun `null slots and retained real fastutil map iterator share budget`() {
        val map=it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap<ContactDiscovery>()
        repeat(400){map.put(it,ContactDiscovery("local_$it",1))}
        val iterator=map.values.iterator();val c=ContactTargetCache();val world=Any()
        c.tick(world,iterator){null};map.clear()
        var calls=0;c.tick(world,iterator){calls++;target(it)}
        assertEquals(0,calls);assertTrue(c.snapshot().second.complete)
    }
    @Test fun `overflow and revalidation do not exceed shared tick allowance`() {
        val c=ContactTargetCache();val world=Any()
        c.tick(world,List(65){ContactDiscovery("local_$it",1)}.iterator(),::target)
        assertEquals(64,c.snapshot().first.size);assertFalse(c.snapshot().second.complete)
        var calls=0
        c.tick(world,List(400){ContactDiscovery("new_$it",1)}.iterator()){calls++;target(it)}
        assertEquals(240,calls)
    }
    @Test fun `stale entries release slots for entrants and reused generations replace old body`() {
        val c=ContactTargetCache();val world=Any()
        c.tick(world,listOf(ContactDiscovery("old",1)).iterator(),::target)
        c.tick(world,listOf(ContactDiscovery("old",2),ContactDiscovery("entrant",1)).iterator()){if(it.generation==1 && it.key=="old")null else target(it)}
        assertEquals(setOf("old","entrant"),c.snapshot().first.map{it.key}.toSet())
        assertEquals(2,c.snapshot().first.first{it.key=="old"}.generation)
        c.tick(Any(),emptyList<ContactDiscovery>().iterator(),::target)
        assertTrue(c.snapshot().first.isEmpty());assertTrue(c.snapshot().second.complete)
    }
    @Test fun `overflow persists until entire retained sweep ends`() {
        val c=ContactTargetCache();val w=Any();val slots=List(400){ContactDiscovery("local_$it",1)}.iterator()
        c.tick(w,slots,::target)
        c.tick(w,slots){if(it.key.substringAfter('_').toInt()<240)target(it) else null}
        assertFalse(c.snapshot().second.complete)
    }
    @Test fun `shrinking live fastutil map invalidates sweep without escaping client tick`() {
        val map=it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap<ContactDiscovery>()
        repeat(400){map.put(it,ContactDiscovery("local_$it",1))}
        val slots=map.values.iterator();val c=ContactTargetCache();val world=Any()
        c.tick(world,slots){null}
        repeat(390){map.remove(it)}
        assertDoesNotThrow{c.tick(world,slots){null}}
        assertFalse(c.snapshot().second.complete)
    }
    @Test fun `real inspection errors still escape guarded iterator boundary`() {
        val cache=ContactTargetCache()
        assertThrows(ArrayIndexOutOfBoundsException::class.java){cache.tick(Any(),listOf(ContactDiscovery("x",1)).iterator()){throw ArrayIndexOutOfBoundsException("inspection")}}
    }
    @Test fun `mutation detected while checking sweep completion stays incomplete`() {
        var calls=0
        val slots=object:Iterator<ContactDiscovery?> {
            override fun hasNext():Boolean { if(++calls==1)return false;throw java.util.ConcurrentModificationException() }
            override fun next():ContactDiscovery?=null
        }
        val cache=ContactTargetCache();cache.tick(Any(),slots){null}
        assertFalse(cache.snapshot().second.complete)
    }
}
