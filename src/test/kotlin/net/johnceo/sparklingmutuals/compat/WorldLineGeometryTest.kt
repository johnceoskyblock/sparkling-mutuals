package net.johnceo.sparklingmutuals.compat

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorldLineGeometryTest {
    @Test fun `camera subtraction happens once and captured geometry is detached`() {
        val points=mutableListOf(Vec3(101.0,2.0,3.0),Vec3(102.0,2.0,3.0))
        val lines=WorldLineGeometry.segments(points,Vec3(100.0,0.0,0.0))
        points.clear()
        assertEquals(Vec3(1.0,2.0,3.0),lines.single().from)
        assertEquals(Vec3(2.0,2.0,3.0),lines.single().to)
        assertEquals(Vec3(1.0,0.0,0.0),lines.single().normal)
    }
    @Test fun `coincident and invalid points never emit invalid segments`() {
        assertTrue(WorldLineGeometry.segments(listOf(Vec3.ZERO,Vec3.ZERO),Vec3.ZERO).isEmpty())
        for(bad in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.MAX_VALUE)) {
            val p=Vec3(bad,0.0,0.0)
            assertTrue(WorldLineGeometry.segments(listOf(Vec3.ZERO,p),Vec3.ZERO).isEmpty())
            assertTrue(WorldLineGeometry.segments(listOf(Vec3.ZERO,Vec3(1.0,0.0,0.0)),p).isEmpty())
        }
        assertTrue(WorldLineGeometry.segments(listOf(Vec3.ZERO,Vec3(1e-100,0.0,0.0)),Vec3.ZERO).isEmpty())
    }
    @Test fun `emission is bounded and normals remain unit and finite`() {
        val lines=WorldLineGeometry.segments(List(1000){Vec3(it.toDouble(),it.toDouble(),0.0)},Vec3.ZERO)
        assertEquals(63,lines.size)
        assertEquals(Vec3(63.0,63.0,0.0),lines.last().to)
        for(line in lines) {
            assertEquals(1.0,line.normal.length(),1e-12)
            assertEquals(.7071067811865475,line.normal.x,1e-12)
            assertEquals(.7071067811865475,line.normal.y,1e-12)
        }
    }
    @Test fun `points coincident after float conversion are skipped`() {
        assertTrue(WorldLineGeometry.segments(listOf(Vec3(1e20,0.0,0.0),Vec3(1e20+1e6,0.0,0.0)),Vec3.ZERO).isEmpty())
    }
}
