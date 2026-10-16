package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotPacketAdapterTest {
    private val origin=ShotVector(100.0,40.0,-20.0)
    @Test fun `raw relative path stays relative while recorder base advances independently of tick samples`() {
        val entry=ShotEntityCache().discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0)!!
        val delta=listOf(ShotPathPoint(ShotVector(1.0,2.0,3.0),2))
        val missing=ShotPacketAdapter.translate(entry,ShotPacketUpdate.Move(1,delta),origin,10)
        assertFalse(missing.baseKnown);assertEquals(delta,missing.points);assertNull(entry.base)
        ShotPacketAdapter.translate(entry,ShotPacketUpdate.PositionSync(1,listOf(ShotPathPoint(origin))),origin,20)
        val move=ShotPacketAdapter.translate(entry,ShotPacketUpdate.Move(1,delta),origin,30)
        assertEquals(ShotVector(0.0,0.0,0.0),move.base);assertEquals(delta,move.points)
        assertEquals(ShotVector(101.0,42.0,-17.0),entry.base)
    }
    @Test fun `mixed teleport flags preserve deltas and motion is never translated as a position`() {
        val e=ShotEntityCache().discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0)!!
        e.base=origin
        val p=ShotPacketAdapter.translate(e,ShotPacketUpdate.Teleport(1,ShotVector(5.0,45.0,7.0),listOf("X","Z")),origin,30)
        assertEquals(ShotVector(5.0,5.0,7.0),p.points.single().position)
        assertEquals(ShotVector(105.0,45.0,-13.0),e.base)
        val velocity=ShotVector(.1,.2,.3)
        assertEquals(velocity,ShotPacketAdapter.translate(e,ShotPacketUpdate.Motion(1,velocity),origin,40).motion)
    }
    @Test fun `stepped path timing is copied and bounded with explicit truncation`() {
        val e=ShotEntityCache().discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0)!!
        val points=(0..99).map { ShotPathPoint(ShotVector(it.toDouble(),0.0,0.0),it) }
        val p=ShotPacketAdapter.translate(e,ShotPacketUpdate.PositionSync(1,points),origin,20)
        assertEquals(64,p.points.size);assertTrue(p.truncated);assertEquals(99,p.points.last().tickOffset)
    }
    @Test fun `network base reconstruction follows installed codec quantization rather than smooth addition`() {
        val e=ShotEntityCache().discover(1,"private",ShotVariant.ORDINARY,null,ShotDiscovery.BOOTSTRAP,0)!!
        e.base=ShotVector(100.123456,40.222222,-20.333333)
        val codec=net.minecraft.network.protocol.game.VecDeltaCodec().also { it.setBase(net.minecraft.world.phys.Vec3(e.base!!.x,e.base!!.y,e.base!!.z)) }
        val expected=net.johnceo.sparklingmutuals.compat.ShotPacketCompat.vector(codec.decode(10,0,-10))
        ShotPacketAdapter.translate(e,ShotPacketUpdate.Move(1,listOf(ShotPathPoint(ShotVector(10.0/4096,0.0,-10.0/4096)))),origin,50)
        assertEquals(expected,e.base)
    }
}
