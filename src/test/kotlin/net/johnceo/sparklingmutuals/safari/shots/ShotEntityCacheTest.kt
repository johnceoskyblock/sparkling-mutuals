package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotEntityCacheTest {
    @Test fun `unidentified teleport advances packet decoding base before delayed metadata replay`() {
        for(flags in listOf(emptyList(),listOf("X","Z"))) {
            val c=ShotEntityCache();val origin=ShotVector(100.0,20.0,30.0)
            c.pendingSpawn(3,"private",origin,100)
            val teleport=ShotPacketUpdate.Teleport(3,ShotVector(10.0,50.0,2.0),flags)
            c.remember(teleport,200)
            val expected=if(flags.isEmpty()) ShotVector(10.0,50.0,2.0) else ShotVector(110.0,50.0,32.0)
            assertEquals(expected,c.packetBase(3))
            val end=ShotVector(expected.x+1,expected.y,expected.z)
            c.remember(ShotPacketUpdate.Move(3,listOf(ShotPathPoint(ShotVector(1.0,0.0,0.0))),resolvedEnd=end),300)
            val e=c.discover(3,"private",ShotVariant.ORDINARY,null,ShotDiscovery.METADATA,400)!!
            val p=c.takePending(3)!!
            val replay=p.updates.map { (time,u) -> ShotPacketAdapter.translate(e,u,origin,time) }
            assertEquals(end-origin,replay.last().resolvedEnd)
            assertEquals(listOf(200L,300L),replay.map { it.observedMicros })
            assertTrue(replay.all { it.baseKnown })
        }
    }
    @Test fun `window expiry finishes tracks once and never rediscovers the same lifetime`() {
        val c=ShotEntityCache()
        c.discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0)
        assertEquals(1,c.expireWindow().size)
        assertTrue(c.expireWindow().isEmpty())
        assertNull(c.discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,9_000_000))
        c.newWindow()
        assertNull(c.discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,10_000_000))
        assertNotNull(c.discover(1,"replacement",ShotVariant.MASTERFUL,null,ShotDiscovery.SPAWN,10_000_001))
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
