package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePathModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class ContactCurveSamplerTest {
    private val eye = ShotVector(1.0,2.0,3.0)
    @Test fun `launch begins at eye and preserves observed n two`() {
        val c=ContactCurveSampler.sample(eye,ShotVector(1.0,0.0,0.0),0.0)
        assertEquals(0.0,c.nodes.first().parameter)
        assertEquals(eye,c.nodes.first().position)
        val p=c.nodes.single{it.parameter==2.0}
        assertEquals(CapsulePathModel.point(eye,ShotVector(1.0,0.0,0.0),2.0),p.position)
        assertFalse(c.nodes.first().observedRegion)
        assertTrue(p.observedRegion)
        assertEquals("contact-guide-center-extrapolation-v1",c.hypothesis)
    }
    @Test fun `normalization and translation preserve sample geometry`() {
        val a=ContactCurveSampler.sample(eye,ShotVector(3.0,0.0,4.0),0.0)
        val b=ContactCurveSampler.sample(ShotVector(101.0,202.0,303.0),ShotVector(6.0,0.0,8.0),0.0)
        assertEquals(a.nodes.size,b.nodes.size)
        a.nodes.zip(b.nodes).forEach{(x,y)->
            assertEquals(100.0,y.position.x-x.position.x,1e-8)
            assertEquals(200.0,y.position.y-x.position.y,1e-8)
            assertEquals(300.0,y.position.z-x.position.z,1e-8)
        }
    }
    @Test fun `all directions respect arc chord deviation and work limits`() {
        for(aim in listOf(ShotVector(0.0,0.0,1.0),ShotVector(0.0,1.0,0.0),ShotVector(0.0,-1.0,0.0),ShotVector(-3.0,.4,2.0))) {
            val c=ContactCurveSampler.sample(eye,aim,35.0)
            assertTrue(c.nodes.size in 2..257)
            assertTrue(c.nodes.all{it.parameter in 0.0..128.0 && !it.observedRegion})
            var length=0.0
            c.nodes.zipWithNext().forEach{(a,b)->
                val chord=sqrt(a.position.distanceSquared(b.position)); length+=chord
                assertTrue(chord<=.5+1e-8,"chord $chord")
                val deviation=kotlin.math.abs(CapsulePathModel.VERTICAL_ACCELERATION)*(b.parameter-a.parameter)*(b.parameter-a.parameter)/8
                assertTrue(deviation<=.01+1e-8)
                assertTrue(b.parameter>a.parameter)
            }
            assertTrue(length<=64+1e-8,"length $length")
            assertTrue(c.stop in setOf(ContactStop.RANGE_LIMIT,ContactStop.ORDINAL_LIMIT,ContactStop.WORK_LIMIT))
        }
    }
    @Test fun `bad eye aim pitch and zero direction produce no geometry`() {
        for(bad in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
            assertEquals(ContactStop.INVALID,ContactCurveSampler.sample(eye,ShotVector(bad,0.0,0.0),0.0).stop)
            assertTrue(ContactCurveSampler.sample(ShotVector(bad,0.0,0.0),eye,0.0).nodes.isEmpty())
            assertTrue(ContactCurveSampler.sample(eye,eye,bad).nodes.isEmpty())
        }
        assertTrue(ContactCurveSampler.sample(eye,ShotVector(0.0,0.0,0.0),0.0).nodes.isEmpty())
    }
}
