package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class RetainedSparklingChecksTest {
    private fun position(name: String): Pair<Double, Double> {
        val critter = SafariRoster.named(name)!!
        return SafariCompletionZones.forBiome(critter.biome)[name]?.let { it.x to it.z } ?: when(critter.biome) {
            SafariBiome.FOREST -> 6.0 to 51.0
            SafariBiome.CAVERN -> -114.0 to 49.0
            SafariBiome.ICY -> -112.0 to -54.0
            SafariBiome.HAUNTED -> -4.0 to -64.0
        }
    }
    private fun scan(checks: SparklingChecks, name: String, ids: List<UUID>, source: Boolean = true) {
        val (x,z) = position(name)
        checks.scan(SafariRoster.named(name)!!.biome, x,z, ids.map { name to it },
            nestsChecked = source, moundsCleared = source, wallsCleared = source,
            birdsSpawned = source, hauntedCoinDropsRemaining = if(source) 0 else 1)
    }
    @Test fun `all fixed spawn counters retain distinct partial and complete run checks`() {
        for ((name, minimum) in mapOf("Scrappy" to 3, "Gemzie" to 3, "Troodon" to 3, "Gazer" to 4)) {
            val checks = SparklingChecks()
            repeat(minimum) { i ->
                val id = UUID.randomUUID()
                repeat(2) { scan(checks, name, listOf(id)) }
                scan(checks, name, emptyList())
                assertEquals(i + 1, checks.checkCount(name), name)
            }
            assertTrue(checks.locallyChecked(name), name)
            checks.reenter(SafariRoster.named(name)!!.biome)
            assertTrue(checks.locallyChecked(name), name)
            assertEquals(minimum, checks.checkCount(name), name)
        }
    }
    @Test fun `every critter retains qualified UUID checks despite rendering and biome entry changes`() {
        for (critter in SafariRoster.all) {
            val checks = SparklingChecks()
            checks.inventory(emptyList())
            val required = maxOf(1, SafariFullClear.minimum(critter.name))
            repeat(required) { scan(checks, critter.name, listOf(UUID.randomUUID())) }
            if (critter.name in setOf("Wumpa", "Doomspiral")) {
                assertFalse(checks.locallyChecked(critter.name), critter.name)
                checks.capture(critter.name)
            }
            assertTrue(checks.locallyChecked(critter.name), critter.name)
            scan(checks, critter.name, emptyList())
            assertEquals(required, checks.checkCount(critter.name), critter.name)
            checks.reenter(critter.biome)
            scan(checks, critter.name, emptyList())
            assertTrue(checks.locallyChecked(critter.name), critter.name)
            assertEquals(required, checks.checkCount(critter.name), critter.name)
        }
    }
    @Test fun `retained sightings never double count later catches and do not fabricate Full Clear captures`() {
        for (name in listOf("Scrappy", "Gemzie", "Troodon", "Gazer", "Gimmiegold")) {
            val run = SafariRun(0)
            scan(run.sparklingChecks, name, List(2) { UUID.randomUUID() })
            run.record(SafariCatch(SafariRoster.named(name)!!))
            scan(run.sparklingChecks, name, emptyList())
            assertEquals(2, run.sparklingChecks.checkCount(name), name)
            assertEquals(1, run.count(name), name)
            run.updateCaptureEvidence(SafariRoster.named(name)!!.biome, emptySet(), false)
            assertFalse(run.captureComplete(name), name)
        }
    }
    @Test fun `retained checking still requires valid scan areas and independent source conditions`() {
        val checks = SparklingChecks(); val id = UUID.randomUUID()
        checks.scan(SafariBiome.CAVERN, 1000.0,1000.0, listOf("Gemzie" to id))
        assertEquals(0, checks.checkCount("Gemzie"))
        scan(checks, "Gemzie", listOf(id))
        assertEquals(1, checks.checkCount("Gemzie"))
        for (name in listOf("Honeybug", "Rockmite", "Snoozle", "Bluebird", "Gimmiegold")) {
            val sourceChecks = SparklingChecks(); sourceChecks.inventory(emptyList())
            repeat(3) { scan(sourceChecks, name, listOf(UUID.randomUUID()), false) }
            assertFalse(sourceChecks.locallyChecked(name), name)
            assertEquals(3, sourceChecks.checkCount(name), name)
            scan(sourceChecks, name, emptyList(), true)
            assertTrue(sourceChecks.locallyChecked(name), name)
        }
    }
    @Test fun `Forest population based feeding proof survives ordinary unloading without waiving new food`() {
        val run = SafariRun(0)
        run.inventoryEvidence(emptyList(), 10000)
        val live = List(9) { "Bluebird" to UUID.randomUUID() }
        run.scanBirdSources(6.0, 51.0, live, 0)
        assertTrue(run.birds.sparklingSpawned)
        run.scanBirdSources(6.0, 51.0, emptyList(), 0)
        assertTrue(run.birds.sparklingSpawned)
        run.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.FOREST, 10000)
        run.scanBirdSources(6.0, 51.0, emptyList(), 0)
        assertFalse(run.birds.sparklingSpawned)
    }

}
