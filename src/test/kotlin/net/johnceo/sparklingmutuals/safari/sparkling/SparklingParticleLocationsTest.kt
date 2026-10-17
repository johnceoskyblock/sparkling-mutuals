package net.johnceo.sparklingmutuals.safari.sparkling

import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparklingParticleLocationsTest {
    @Test fun `wax on is accepted only in Safari and valid map coordinates`() {
        val points = SparklingParticleLocations()
        assertNull(points.observe(false, true, 0.0, 50.0, -60.0, 0))
        assertNull(points.observe(true, false, 0.0, 50.0, -60.0, 0))
        assertNull(points.observe(true, true, Double.NaN, 50.0, -60.0, 0))
        assertNull(points.observe(true, true, 1000.0, 50.0, -60.0, 0))
        assertTrue(points.unknown().isEmpty())
        assertEquals(SafariBiome.HAUNTED, points.observe(true, true, 0.0, 50.0, -60.0, 0)!!.biome)
    }
    @Test fun `repeated bursts refresh one last seen location without a biome distance filter`() {
        val points = SparklingParticleLocations()
        val first = points.observe(true, true, -100.0, 40.0, 50.0, 0)!!
        val second = points.observe(true, true, -99.5, 40.5, 50.0, 100)!!
        assertEquals(first.id, second.id)
        assertEquals(100, second.seenAt)
        assertEquals(-99.5, points.unknown().single().x)
        assertEquals(SafariBiome.CAVERN, first.biome)
        points.observe(true, true, 0.0, 60.0, -60.0, 200)
        assertEquals(2, points.unknown().size)
        assertTrue(SparklingParticleLocations().unknown().isEmpty(), "new run owns new observations")
    }
    @Test fun `independent identification retires only matching particle waypoint`() {
        val points = SparklingParticleLocations()
        points.observe(true, true, 0.0, 50.0, -60.0, 0)
        points.observe(true, true, 10.0, 50.0, -60.0, 0)
        points.identify(0.0, 49.0, -60.0)
        assertEquals(10.0, points.unknown().single().x)
        points.observe(true, true, 0.1, 50.0, -60.0, 100)
        assertEquals(1, points.unknown().size, "lingering bursts must not reopen an identified location")
    }
}
