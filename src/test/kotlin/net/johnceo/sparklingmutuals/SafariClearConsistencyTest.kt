package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariClearConsistencyTest {
    @Test fun `Rockmite needs no captures after empty mounds but requires every revealed critter`() {
        val run = SafariRun(0)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        repeat(9) { run.recordMound("The mound falls apart, but nothing is inside...") }
        assertFalse(run.captureComplete("Rockmite"))
        run.moundSurvey.scan(emptySet()) { true }
        assertTrue(run.captureComplete("Rockmite"))
        run.recordMound("The mound fell apart, revealing a Rockmite hidden inside!")
        assertFalse(run.captureComplete("Rockmite"))
        run.record(SafariCatch(SafariRoster.named("Rockmite")!!))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite"), true)
        assertFalse(run.captureComplete("Rockmite"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        assertTrue(run.captureComplete("Rockmite"))
    }
    @Test fun `Strongarm turns green at four captures only after nearby entities clear`() {
        val run = SafariRun(0)
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Strongarm")!!)) }
        assertFalse(run.captureComplete("Strongarm"))
        run.record(SafariCatch(SafariRoster.named("Strongarm")!!))
        assertTrue(run.captureComplete("Strongarm"))
        run.updateCaptureEvidence(SafariBiome.ICY, setOf("Strongarm"), false)
        assertFalse(run.captureComplete("Strongarm"))
    }
    private fun forest() = SafariRun(1000).apply {
        mapOf("Foxtrot" to 6, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
            "Fluffling" to 1, "Hideonfloor" to 1, "Bluebird" to 7, "Macaw" to 1).forEach { (name, amount) ->
            repeat(amount) { record(SafariCatch(SafariRoster.named(name)!!)) }
        }
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) recordBirdFood("FLOOR DROP! $food") }
        repeat(7) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        repeat(2) { birds.spawn("Two Macaws were attracted to the Birdfeeder!") }
        birds.inventory(emptyList())
    }
    @Test fun `Honeybug minimum and empty scan cannot turn green before nests are punched`() {
        val run = forest()
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertFalse(run.captureComplete("Honeybug"))
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, allNestsChecked = true)
        assertTrue(run.captureComplete("Honeybug"))
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Honeybug"), false, allNestsChecked = true)
        assertFalse(run.captureComplete("Honeybug"))
    }
    @Test fun `Forest clear uses current entities instead of accumulated entity ids`() {
        val run = forest()
        repeat(40) { run.observeCritter(it, "Foxtrot"); run.observeCritter(100 + it, "Macaw") }
        val ready = BiomeClearEvidence(observed = true, nearbyCritters = 3, nearbyMacaws = 3, nestsChecked = true)
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(nestsChecked = false)))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(nearbyCritters = 4)))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(observed = false)))
    }
    @Test fun `Cavern requires all structures but old Snoozle sightings do not block an empty scan`() {
        val run = SafariRun(0)
        mapOf("Cavernfish" to 4, "Flitter" to 6, "Shyworm" to 4, "Driftling" to 3,
            "Chuckwalla" to 2, "Scrappy" to 3, "Gemzie" to 3).forEach { (name, amount) ->
            repeat(amount) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
        }
        repeat(20) { run.recordMound("The mound falls apart, but nothing is inside...") }
        run.observeCritter(10, "Snoozle")
        val ready = BiomeClearEvidence(observed = true, moundsCleared = true, wallsCleared = true)
        assertTrue(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready.copy(moundsCleared = false)))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready.copy(wallsCleared = false)))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready.copy(nearbyCritters = 1)))
    }
    @Test fun `green Forest captures record first clear time and persist PB after nests are checked`() {
        val run = forest()
        val bests = SafariPersonalBests()
        val ready = BiomeClearEvidence(observed = true, nearbyCritters = 3, nearbyMacaws = 3, nestsChecked = true)
        assertNull(SafariFullClear.recordClear(run, SafariBiome.FOREST, ready.copy(nestsChecked = false), bests, 60000))
        assertTrue(run.biomeClears.isEmpty())
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false, allNestsChecked = true, macawsInRange = 3)
        assertTrue(SafariBiome.FOREST.critters.all { run.captureComplete(it.name) })
        assertEquals("[SM] New Forest Full Clear PB: 1:01.000!", SafariFullClear.recordClear(run, SafariBiome.FOREST, ready, bests, 62000))
        assertTrue(SafariBiome.FOREST in run.biomeClears)
        assertNull(SafariFullClear.recordClear(run, SafariBiome.FOREST, ready, bests, 65000))
        val saved = java.util.Properties().also(bests::save)
        assertEquals("61000", saved.getProperty("pb.forestMillis"))
        assertEquals(61000L, SafariPersonalBests().also { it.load(saved) }.time("Forest"))
    }
    @Test fun `Icy and Haunted record PB when their last detected critter clears after minimums`() {
        val captures = mapOf(
            SafariBiome.ICY to mapOf("Strongarm" to 4, "Tepid" to 6, "Polaris" to 2, "Shuddersquid" to 3,
                "Billygoat" to 2, "Mantis Shrimp" to 3, "Nozzlenose" to 2, "Troodon" to 3, "Wumpa" to 1),
            SafariBiome.HAUNTED to mapOf("Areita" to 3, "Bloodbat" to 3, "Duplico" to 2, "Gazer" to 4,
                "Litterbug" to 4, "Solsnatcher" to 4, "Gimmiegold" to 3, "Hideonwall" to 2, "Hideyho" to 1, "Doomspiral" to 1))
        for ((biome, minimums) in captures) {
            val run = SafariRun(1000)
            val bests = SafariPersonalBests()
            val ready = BiomeClearEvidence(observed = true)
            minimums.forEach { (name, amount) -> repeat(amount) { run.record(SafariCatch(SafariRoster.named(name)!!)) } }
            assertNull(SafariFullClear.recordClear(run, biome, ready.copy(nearbyCritters = 1), bests, 40000))
            assertFalse(biome in run.biomeClears)
            assertNotNull(SafariFullClear.recordClear(run, biome, ready, bests, 45000))
            assertEquals(44000L, bests.time(biome.label))
            assertTrue(biome in run.biomeClears)
        }
    }
}
