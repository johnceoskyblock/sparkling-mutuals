package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotEntityCacheTest {
    @Test fun `window expiry finishes tracks once and blocks rediscovery until the next local input`() {
        val c=ShotEntityCache()
        c.discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0)
        assertEquals(1,c.expireWindow().size)
        assertTrue(c.expireWindow().isEmpty())
        assertNull(c.discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,9_000_000))
        c.newWindow()
        assertNotNull(c.discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,10_000_000))
    }
    @Test fun `reused runtime id gets a new generation and bootstrap never invents an observed spawn`() {
        val c=ShotEntityCache()
        val first=c.discover(7,"private-a",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0)!!
        assertNull(first.base);assertNull(first.spawnMicros)
        c.remove(7)
        val second=c.discover(7,"private-b",ShotVariant.ORDINARY,null,ShotDiscovery.SPAWN,100)!!
        assertNotEquals(first.token,second.token); assertTrue(second.generation>first.generation)
        assertEquals(100L,second.spawnMicros)
    }
    @Test fun `metadata discovery retains observed spawn anchor but never seeds base from a smooth sample`() {
        val c=ShotEntityCache();val p=ShotVector(100.0,50.0,90.0)
        c.pendingSpawn(3,"private",p,20)
        val e=c.discover(3,"private",ShotVariant.MASTERFUL,null,ShotDiscovery.METADATA,40)!!
        assertEquals(p,e.base); assertEquals(20L,e.spawnMicros);assertEquals(ShotDiscovery.METADATA,e.discovery)
        assertNull(c.discover(4,"other",ShotVariant.ORDINARY,null,ShotDiscovery.METADATA,40)!!.base)
    }
    @Test fun `cache bounds active capsules targets and unidentified work`() {
        val c=ShotEntityCache()
        repeat(17) { c.discover(it,"c$it",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0) }
        assertEquals(16,c.entries().size)
        repeat(65) { c.discover(100+it,"t$it",null,"Honeybug",ShotDiscovery.BOOTSTRAP,0) }
        assertEquals(64,c.entries().count { it.species!=null })
        repeat(400) { c.pendingSpawn(1000+it,"p$it",ShotVector(0.0,0.0,0.0),0) }
        assertTrue(c.pendingCount<=256)
        assertTrue(c.takeGaps().containsAll(listOf(ShotGap.CAPSULE_LIMIT,ShotGap.TARGET_LIMIT,ShotGap.CACHE_BACKLOG)))
    }
}
