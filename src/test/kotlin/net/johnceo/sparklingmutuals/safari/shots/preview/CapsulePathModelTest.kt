package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapsulePathModelTest {
    private val origin = ShotVector(1.0, 2.0, 3.0)
    private val aim = ShotVector(1.0, 0.0, 0.0)

    @Test fun `first observed parameter matches fixed spatial curve`() {
        val p = CapsulePathModel.point(origin, aim, 2.0)!!
        assertEquals(2.6964964499885896, p.x, 1e-12)
        assertEquals(1.9799144466888192, p.y, 1e-12)
        assertEquals(3.0, p.z, 1e-12)
    }

    @Test fun `scaled aim and translated origins preserve the curve`() {
        val p = CapsulePathModel.point(ShotVector(101.0, 202.0, 303.0), ShotVector(8.0, 0.0, 0.0), 2.0)!!
        assertEquals(102.69649644998859, p.x, 1e-12)
        assertEquals(201.97991444668882, p.y, 1e-12)
        assertEquals(303.0, p.z, 1e-12)
        val diagonal = CapsulePathModel.point(origin, ShotVector(3.0, 0.0, 4.0), 2.0)!!
        assertEquals(2.0178978699931536, diagonal.x, 1e-12)
        assertEquals(4.357197159990872, diagonal.z, 1e-12)
    }

    @Test fun `bounded path includes both endpoints with finite increasing range`() {
        val points = CapsulePathModel.points(origin, aim)
        assertEquals(64, points.size)
        assertEquals(2.6964964499885896, points.first().x, 1e-12)
        assertEquals(16.0, points.last().x, 1e-12)
        assertTrue(points.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
        assertTrue(points.zipWithNext().all { (a,b) -> b.x > a.x && b.y < a.y })
        assertEquals(points, CapsulePathModel.points(origin, aim))
        assertNotSame(points, CapsulePathModel.points(origin, aim))
    }

    @Test fun `invalid inputs cannot produce geometry`() {
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            for (v in listOf(ShotVector(bad,0.0,0.0), ShotVector(0.0,bad,0.0), ShotVector(0.0,0.0,bad))) {
                assertNull(CapsulePathModel.point(v, aim, 2.0))
                assertNull(CapsulePathModel.point(origin, v, 2.0))
                assertTrue(CapsulePathModel.points(v, aim).isEmpty())
                assertTrue(CapsulePathModel.points(origin, v).isEmpty())
            }
            assertNull(CapsulePathModel.point(origin, aim, bad))
        }
        assertTrue(CapsulePathModel.points(origin, ShotVector(0.0,0.0,0.0)).isEmpty())
        assertNull(CapsulePathModel.point(origin, aim, 1.999))
        assertNull(CapsulePathModel.point(origin, aim, CapsulePathModel.endParameter + .001))
    }
}
