package net.johnceo.sparklingmutuals.safari.sparkling

import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparklingParticleLocationsTest {
    @Test fun `confirmed Foxtrot catch clears its anonymous trail but preserves other biomes and future sightings`() {
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(0)
        val points = run.sparklingParticles
        points.observe(true, true, -14.0, 66.0, 61.0, 0)
        points.observe(true, true, -6.0, 66.0, 64.0, 100)
        points.observe(true, true, 0.0, 50.0, -60.0, 100)
        assertEquals(3, points.unknown().size)
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse("CAPTURE! You caught a Foxtrot!")!!, 500)
        assertEquals(3, points.unknown().size, "ordinary catches cannot resolve particle detections")
        run.sparklingBroadcast("SPARKLING! Friend caught a SPARKLING Foxtrot!", 1000)
        assertEquals(SafariBiome.HAUNTED, points.unknown().single().biome)
        assertNull(points.observe(true, true, -6.0, 66.0, 64.0, 1200), "ignore trailing capture particles")
        val later = points.observe(true, true, -6.0, 66.0, 64.0, 3000)!!
        // The loot-share reward is the same capture, not a second reason to clear a new sighting.
        run.record(net.johnceo.sparklingmutuals.safari.SafariCatch.parse(
            "LOOT SHARE! You received a Rainbow Feather from Friend catching a SPARKLING Foxtrot!")!!, 3100)
        assertTrue(points.unknown().any { it.id == later.id })
    }
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
