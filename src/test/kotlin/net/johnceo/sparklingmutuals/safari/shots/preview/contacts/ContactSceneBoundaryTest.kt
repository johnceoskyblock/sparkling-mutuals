package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class ContactSceneBoundaryTest {
    @Test fun `crossed cells include exact negative chunk corner and endpoint`() {
        val cells=ContactSceneBoundary.cells(ShotVector(15.9,0.1,15.9),ShotVector(16.1,.1,16.1))
        assertTrue(cells.contains(Triple(15,0,15)));assertTrue(cells.contains(Triple(16,0,16)))
        assertTrue(ContactSceneBoundary.cells(ShotVector(-.1,.1,-.1),ShotVector(.1,.1,.1)).contains(Triple(-1,0,-1)))
    }
    @Test fun `partial slab leaves space above while inside origin contacts immediately`() {
        val slab=ShotBounds(ShotVector(0.0,0.0,0.0),ShotVector(1.0,.5,1.0))
        assertNull(ContactIntersections.entry(ShotVector(-.1,.75,.5),ShotVector(1.1,.75,.5),slab))
        assertEquals(0.0,ContactIntersections.entry(ShotVector(.5,.25,.5),ShotVector(.5,1.0,.5),slab))
    }
    @Test fun `loaded preflight completes before any terrain query`() {
        var queries=0
        val result=ContactSceneBoundary.preflight(ShotVector(15.9,.1,0.0),ShotVector(16.1,.1,0.0),{x,_,_->x<16}){_,_,_->queries++}
        assertFalse(result);assertEquals(0,queries)
    }
}
