package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariBiomeClearTest {
    private fun icy(personal: Boolean = true) = SafariRun(1000).apply {
        listOf("Strongarm" to 6, "Tepid" to 6, "Polaris" to 2, "Shuddersquid" to 3, "Billygoat" to 2,
            "Mantis Shrimp" to 3, "Nozzlenose" to 2, "Troodon" to 3, "Wumpa" to 1).forEach { (name, amount) ->
            repeat(amount) { record(SafariCatch(SafariRoster.named(name)!!, personal)) }
        }
    }
    @Test fun `biome PB requires personal minimums fresh observation and no outstanding entities`() {
        val ready = BiomeClearEvidence(observed = true)
        assertTrue(SafariFullClear.eligible(icy(), SafariBiome.ICY, ready))
        assertFalse(SafariFullClear.eligible(icy(false), SafariBiome.ICY, ready))
        assertFalse(SafariFullClear.eligible(icy(), SafariBiome.ICY, ready.copy(observed = false)))
        assertFalse(SafariFullClear.eligible(icy(), SafariBiome.ICY, ready.copy(nearbyCritters = 1)))
        assertFalse(SafariFullClear.eligible(icy().also { it.endedAt = 63000 }, SafariBiome.ICY, ready))
        val missed = SafariRun(1000)
        assertFalse(SafariFullClear.eligible(missed, SafariBiome.ICY, ready))
    }
    @Test fun `cavern minimums alone cannot bypass mounds or unknown walls`() {
        val run = SafariRun(0)
        listOf("Cavernfish" to 4, "Flitter" to 6, "Shyworm" to 4, "Driftling" to 3, "Chuckwalla" to 2,
            "Scrappy" to 3, "Gemzie" to 3).forEach { (name, amount) ->
            repeat(amount) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
        }
        repeat(20) { run.recordMound("The mound falls apart, but nothing is inside...") }
        val ready = BiomeClearEvidence(observed = true, moundsCleared = true, wallsCleared = true)
        assertTrue(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready.copy(moundsCleared = false)))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready.copy(wallsCleared = false)))
        run.observeCritter(100, "Snoozle")
        assertTrue(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready.copy(nearbyCritters = 1)))
        run.record(SafariCatch(SafariRoster.named("Snoozle")!!))
        assertTrue(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready))
        run.observeCritter(101, "Snoozle")
        assertTrue(SafariFullClear.eligible(run, SafariBiome.CAVERN, ready))
        assertFalse(SafariFullClear.eligible(SafariRun(0), SafariBiome.CAVERN, ready))
    }
    @Test fun `forest requires green bird requirements and every honey nest`() {
        val run = SafariRun(0)
        listOf("Foxtrot" to 6, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
            "Fluffling" to 1, "Hideonfloor" to 1).forEach { (name, amount) ->
            repeat(amount) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
        }
        val ready = BiomeClearEvidence(observed = true, nestsChecked = true)
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        repeat(8) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        repeat(2) { run.record(SafariCatch(SafariRoster.named("Macaw")!!)) }
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) run.recordBirdFood("FLOOR DROP! $food") }
        repeat(8) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        run.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        run.birds.inventory(emptyList())
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(nestsChecked = false)))
    }
    @Test fun `haunted requires four Gazers and its Doomspiral even after entities disappear`() {
        val run = SafariRun(0)
        listOf("Areita" to 3, "Bloodbat" to 3, "Duplico" to 2, "Gazer" to 3, "Litterbug" to 4,
            "Solsnatcher" to 4, "Gimmiegold" to 3, "Hideonwall" to 2, "Hideyho" to 1).forEach { (name, amount) ->
            repeat(amount) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
        }
        val ready = BiomeClearEvidence(observed = true)
        assertFalse(SafariFullClear.eligible(run, SafariBiome.HAUNTED, ready))
        run.record(SafariCatch(SafariRoster.named("Gazer")!!))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.HAUNTED, ready))
        run.record(SafariCatch(SafariRoster.named("Doomspiral")!!))
        assertTrue(SafariFullClear.eligible(run, SafariBiome.HAUNTED, ready))
    }
    private fun forest(personal: Boolean = true, pairs: Int = 0) = SafariRun(1000).apply {
        listOf("Foxtrot" to 6, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
            "Fluffling" to 1, "Hideonfloor" to 1, "Bluebird" to (8 - pairs), "Parakeet" to 1).forEach { (name, amount) ->
            repeat(amount) { record(SafariCatch(SafariRoster.named(name)!!, personal)) }
        }
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) recordBirdFood("FLOOR DROP! $food") }
        repeat(9 - pairs) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        repeat(pairs) { birds.spawn("Two Macaws were attracted to the Birdfeeder!") }
        birds.inventory(emptyList())
    }
    @Test fun `Forest PB accepts personally completed birds with all food collected`() {
        val ready = BiomeClearEvidence(observed = true, nestsChecked = true)
        assertTrue(SafariFullClear.eligible(forest(), SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(forest(false), SafariBiome.FOREST, ready))
        val sharedBirds = forest(false).apply {
            listOf("Foxtrot" to 6, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
                "Fluffling" to 1, "Hideonfloor" to 1).forEach { (name, amount) ->
                repeat(amount) { record(SafariCatch(SafariRoster.named(name)!!)) }
            }
        }
        assertFalse(SafariFullClear.eligible(sharedBirds, SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(forest(), SafariBiome.FOREST, ready.copy(nestsChecked = false)))
        val missed = forest().also { repeat(7) { id -> it.observeCritter(id, "Foxtrot") } }
        assertTrue(SafariFullClear.eligible(missed, SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(missed, SafariBiome.FOREST, ready.copy(nearbyCritters = 1)))
        missed.record(SafariCatch(SafariRoster.named("Foxtrot")!!))
        assertTrue(SafariFullClear.eligible(missed, SafariBiome.FOREST, ready))
    }
    @Test fun `Forest Macaw exception ignores only remaining Macaws and saves eligible PB`() {
        val run = forest(pairs = 2)
        repeat(2) { run.observeCritter(100 + it, "Macaw") }
        val ready = BiomeClearEvidence(observed = true, nearbyCritters = 3, nestsChecked = true, nearbyMacaws = 3)
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(nearbyCritters = 4)))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(observed = false)))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(nestsChecked = false)))
        assertFalse(SafariFullClear.eligible(forest(false), SafariBiome.FOREST, ready))
        // Old display identities do not impose hidden capture quotas.
        run.observeCritter(102, "Macaw")
        run.observeCritter(103, "Macaw")
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        val bests = SafariPersonalBests()
        assertNotNull(bests.recordBiome(SafariBiome.FOREST, run, 62000))
        val saved = java.util.Properties().also(bests::save)
        assertEquals("61000", saved.getProperty("pb.forestMillis"))
    }
}
