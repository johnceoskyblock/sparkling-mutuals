package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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

    @Test fun `earned source checks survive biome reentry before any fresh source scan`() {
        for (name in listOf("Honeybug", "Rockmite", "Snoozle", "Bluebird", "Parakeet", "Macaw", "Gimmiegold")) {
            val run = SafariRun(0)
            val biome = SafariRoster.named(name)!!.biome
            run.visitBiome(biome)
            run.inventoryEvidence(emptyList(), 10000)
            scan(run.sparklingChecks, name, List(3) { UUID.randomUUID() })
            assertTrue(run.sparklingChecks.locallyChecked(name), name)
            run.visitBiome(if (biome == SafariBiome.ICY) SafariBiome.CAVERN else SafariBiome.ICY)
            run.visitBiome(biome)
            assertTrue(run.sparklingChecks.locallyChecked(name), "$name lost proven source on entry")
        }
    }
    @Test fun `central Forest source recovery survives reentry and unknown unloaded markers`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(emptyList(), 10000)
        run.scanBirdSources(6.0, 51.0, List(9) { "Bluebird" to UUID.randomUUID() }, 0)
        assertTrue(run.birds.sparklingSpawned)
        run.visitBiome(SafariBiome.ICY)
        run.visitBiome(SafariBiome.FOREST)
        run.scanBirdSources(1000.0, 1000.0, emptyList(), -1)
        assertTrue(run.birds.sparklingSpawned, "unknown unloading is not new food")
        run.scanBirdSources(6.0, 51.0, emptyList(), 1)
        assertFalse(run.birds.sparklingSpawned, "a positive remaining source must still reopen")
    }
    @Test fun `Haunted source recovery survives reentry and unknown out of radius scan`() {
        val checks = SparklingChecks()
        checks.inventory(emptyList())
        scan(checks, "Gimmiegold", List(3) { UUID.randomUUID() })
        assertTrue(checks.locallyChecked("Gimmiegold"))
        checks.reenter(SafariBiome.HAUNTED)
        checks.scan(SafariBiome.HAUNTED, 1000.0, 1000.0, emptyList(), hauntedCoinDropsRemaining = -1)
        assertTrue(checks.locallyChecked("Gimmiegold"))
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedCoinDropsRemaining = 1)
        assertFalse(checks.locallyChecked("Gimmiegold"))
    }

    @Test fun `party report revalidation keeps earned checks but never fabricates Unique catches`() {
        val party = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - "Gemzie")))
        }
        val run = SafariRun(0)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertTrue(run.sparklingComplete("Gemzie", party))
        assertFalse(run.uniqueComplete("Gemzie", party))
        run.visitBiome(SafariBiome.CAVERN)
        assertFalse(run.sparklingComplete("Gemzie", party), "party report has no local check")
        scan(run.sparklingChecks, "Gemzie", List(3) { UUID.randomUUID() })
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Gemzie"), false)
        assertTrue(run.sparklingComplete("Gemzie", party))
        run.record(SafariCatch(SafariRoster.named("Gemzie")!!))
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING, SafariMode.FULL_CLEAR, SafariMode.UNIQUE)) {
            run.visitBiome(SafariBiome.ICY); run.visitBiome(SafariBiome.CAVERN)
            run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false, playerX = 1000.0, playerZ = 1000.0)
            run.sparklingChecks.scan(SafariBiome.CAVERN, 1000.0, 1000.0, emptyList())
            assertEquals(if (mode == SafariMode.FULL_CLEAR) 0 else 1,
                run.completed(mode, listOf(SafariRoster.named("Gemzie")!!), party))
        }
        run.observeSparkling(UUID.randomUUID(), "Gemzie")
        assertFalse(run.sparklingComplete("Gemzie", party))
        assertFalse(run.uniqueComplete("Gemzie", party))
        assertEquals(3, run.sparklingChecks.checkCount("Gemzie"))
        assertEquals(0, SafariRun(1).sparklingChecks.checkCount("Gemzie"))
    }

    @Test fun `recovered nest opportunity proof survives unloaded bees but not an extra nest`() {
        val run = SafariRun(0)
        run.record(SafariCatch(SafariRoster.named("Honeybug")!!))
        run.updateNestEvidence(false, 0, 3, 0)
        run.liveHoneybugs(2)
        assertTrue(run.nestsResolved)
        assertTrue(run.sparklingChecks.locallyChecked("Honeybug"))
        run.liveHoneybugs(0)
        assertTrue(run.nestsResolved, "unloading cannot erase reconciled nest opportunities")
        assertTrue(run.sparklingChecks.locallyChecked("Honeybug"))
        run.updateNestEvidence(false, 0, 4, 0)
        assertFalse(run.nestsResolved, "a newly identified fourth nest must reopen")
        assertFalse(run.sparklingChecks.locallyChecked("Honeybug"))
        run.liveHoneybugs(3)
        assertTrue(run.nestsResolved)
        assertFalse(SafariRun(1).nestsResolved)
    }

}
