package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContactIntersectionsTest {
    private fun v(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0) = ShotVector(x,y,z)
    private val box = ShotBounds(v(-1.0,-1.0,.5),v(1.0,1.0,.6))
    @Test fun `slab entry has independently calculated fraction`() {
        assertEquals(.25, ContactIntersections.entry(v(),v(z=2.0),box)!!,1e-12)
    }
    @Test fun `inside starts and negative direction use earliest entry`() {
        assertEquals(0.0,ContactIntersections.entry(v(z=.55),v(z=2.0),box))
        assertEquals(.7,ContactIntersections.entry(v(z=2.0),v(),box)!!,1e-12)
    }
    @Test fun `parallel and vertical rays do not divide by zero`() {
        assertNull(ContactIntersections.entry(v(x=2.0),v(x=2.0,z=2.0),box))
        assertEquals(.25,ContactIntersections.entry(v(y=-2.0,z=.55),v(y=2.0,z=.55),box)!!,1e-12)
        assertEquals(0.0,ContactIntersections.entry(v(z=.55),v(z=.55),box))
    }
    @Test fun `degenerate inverted and nonfinite boxes are unavailable`() {
        assertNull(ContactIntersections.entry(v(),v(z=2.0),ShotBounds(v(),v())))
        assertNull(ContactIntersections.entry(v(),v(z=2.0),ShotBounds(box.max,box.min)))
        assertNull(ContactIntersections.entry(v(x=Double.NaN),v(z=2.0),box))
    }
}
