package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.resources.renderBird
import net.johnceo.sparklingmutuals.safari.resources.scanRenderedBirds

import java.nio.file.Path
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.resources.SafariBirdLedger
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SparklingChecksTest {
    @TempDir lateinit var dir: Path
    private fun seen(name: String, count: Int) = List(count) { name to UUID.randomUUID() }
    private fun party(needs: Set<String>) = PartySparklingState().apply {
        select(setOf("one")); accept(setOf("one"), mapOf("one" to SafariRoster.all.map { it.name }.toSet() - needs))
    }
    @Test fun `any Wumpa capture checks Icy and supplies shared Unique prerequisite proof`() {
        ConfigManager.init(dir)
        SafariFullClear.select(SafariMode.SPARKLING)
        val icy = SafariBiome.ICY
        val state = party(icy.critters.map { it.name }.toSet())
        for (personal in listOf(true, false)) {
            val run = SafariRun(0)
            run.visitBiome(icy)
            run.updateCaptureEvidence(icy, emptySet(), false)
            run.sparklingChecks.chat("The Wumpa has awoken.")
            assertFalse(run.sparklingComplete("Wumpa", state))
            run.record(SafariCatch(SafariRoster.named("Wumpa")!!, personal = personal), 1000)
            for (localOnly in listOf(true, false)) {
                assertEquals(icy.critters.size, run.completed(SafariMode.SPARKLING, icy.critters, state, localOnly))
            }
            assertTrue(SafariPanels.missingCritters(run, icy, state).isEmpty())
            assertEquals("id", run.completion.announcement(run, icy, SafariMode.SPARKLING, state))
            assertEquals(icy.critters.size, run.completed(SafariMode.UNIQUE, icy.critters, state))
            assertFalse(run.captureComplete("Billygoat"))
            assertFalse(run.sparklingComplete("Gazer", state))
            run.visitBiome(SafariBiome.FOREST)
            run.visitBiome(icy)
            assertEquals(icy.critters.size, run.completed(SafariMode.SPARKLING, icy.critters, state))
            assertTrue(SafariPanels.missingCritters(run, icy, state).isEmpty())
        }
    }
    @Test fun `Forest UUID zone checks do not check manually spawned species`() {
        val checks = SparklingChecks()
        checks.scan(SafariBiome.FOREST, 21.01, 51.0, emptyList())
        assertFalse(checks.checked("Foxtrot"))
        checks.scan(SafariBiome.FOREST, 21.0, 51.0, seen("Foxtrot", 6) + seen("Treefrog", 3))
        assertTrue(checks.checked("Foxtrot")); assertTrue(checks.checked("Treefrog"))
        assertFalse(checks.checked("Honeybug")); assertFalse(checks.checked("Bluebird"))
        assertFalse(checks.checked("Driftling"))
    }
    @Test fun `Haunted center alone does not check natural species`() {
        for ((biome, x, z) in listOf(Triple(SafariBiome.HAUNTED, -4.0, -64.0))) {
            val checks = SparklingChecks(); checks.scan(biome, x, z, emptyList())
            assertFalse(checks.checked(biome.critters.first().name))
            assertFalse(checks.checked(when (biome) { SafariBiome.CAVERN -> "Gemzie"; SafariBiome.ICY -> "Wumpa"; else -> "Doomspiral" }))
        }
    }
    @Test fun `qualified UUID sightings persist through unloading and deduplicate repeated sightings`() {
        val checks = SparklingChecks(); val gems = seen("Gemzie", 3)
        repeat(3) { checks.scan(SafariBiome.CAVERN, -140.0, 49.0, gems.take(1)) }
        checks.scan(SafariBiome.CAVERN, -140.0, 49.0, emptyList())
        checks.scan(SafariBiome.CAVERN, -140.0, 49.0, gems.take(2))
        assertFalse(checks.checked("Gemzie"))
        checks.scan(SafariBiome.CAVERN, -140.0, 49.0, gems.takeLast(1))
        assertTrue(checks.checked("Gemzie"))
        checks.scan(SafariBiome.CAVERN, -140.0, 49.0, gems)
        assertTrue(checks.checked("Gemzie"))
        assertFalse(SparklingChecks().checked("Gemzie"))
    }
    @Test fun `Honeybug requires complete nest evidence instead of a rendered bee minimum`() {
        val checks = SparklingChecks(); val bugs = seen("Honeybug", 3)
        checks.scan(SafariBiome.FOREST, 6.0, 51.0, bugs)
        assertFalse(checks.checked("Honeybug"))
        checks.scan(SafariBiome.FOREST, 6.0, 51.0, emptyList(), nestsChecked = true)
        assertTrue(checks.checked("Honeybug"))
        val partial = SparklingChecks()
        partial.scan(SafariBiome.FOREST, 6.0, 51.0, bugs.take(2), nestsChecked = true)
        assertTrue(partial.checked("Honeybug"))
    }
    @Test fun `mounds and walls need their complete survey evidence`() {
        val checks = SparklingChecks()
        checks.scan(SafariBiome.CAVERN, -114.0, 49.0, emptyList())
        assertFalse(checks.checked("Rockmite")); assertFalse(checks.checked("Snoozle"))
        checks.scan(SafariBiome.CAVERN, -114.0, 49.0, emptyList(), moundsCleared = true)
        assertTrue(checks.checked("Rockmite")); assertFalse(checks.checked("Snoozle"))
        checks.scan(SafariBiome.CAVERN, -114.0, 49.0, emptyList(), wallsCleared = true)
        assertTrue(checks.checked("Snoozle"))
    }
    @Test fun `boss checks require captures while four distinct Gazers suffice`() {
        val checks = SparklingChecks(); val gazers = seen("Gazer", 4)
        checks.scan(SafariBiome.ICY, -112.0, -54.0, seen("Wumpa", 1))
        assertFalse(checks.checked("Wumpa"))
        checks.capture("Wumpa")
        assertTrue(checks.checked("Wumpa"))
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, gazers.take(3) + seen("Doomspiral", 1))
        assertFalse(checks.checked("Doomspiral")); assertFalse(checks.checked("Gazer"))
        checks.capture("Doomspiral")
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, gazers)
        assertTrue(checks.checked("Gazer"))
        assertTrue(checks.checked("Doomspiral"))
    }
    @Test fun `boss sightings and completion messages cannot replace personal or shared capture`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        data class Boss(val name: String, val biome: SafariBiome, val x: Double, val z: Double, val slain: String, val done: String)
        for ((species, area, x, z, slain, done) in listOf(
            Boss("Wumpa", SafariBiome.ICY, -112.0, -54.0, "The cave opens up again...", "id"),
            Boss("Doomspiral", SafariBiome.HAUNTED, -4.0, -64.0, "The darkness in the Haunted Biome fades away...", "hd")
        )) {
            val state = party(setOf(species))
            for (personal in listOf(true, false)) {
                val run = SafariRun(0)
                run.sparklingChecks.scan(area, x, z, seen(species, 1))
                run.sparklingChecks.chat(slain, manualAllowed = true)
                run.sparklingChecks.chat("Party > Friend: $done", manualAllowed = true)
                run.partyFullClear("Party > Friend: $done")
                assertFalse(run.sparklingChecks.checked(species))
                assertFalse(run.sparklingComplete(species, state))
                assertEquals(0, run.completed(SafariMode.SPARKLING, listOf(SafariRoster.named(species)!!), state))
                assertTrue(SafariPanels.missingCritters(run, area, state).any { it.name == species })
                val capture = if (personal) "CAPTURE! You caught a $species!"
                    else "LOOT SHARE! You received a $species Shard from Friend catching a $species!"
                run.record(SafariCatch.parse(capture)!!)
                assertTrue(run.sparklingChecks.checked(species))
                assertTrue(run.sparklingComplete(species, state, localOnly = true))
                assertFalse(SafariPanels.missingCritters(run, area, state).any { it.name == species })
                run.sparklingChecks.reenter(area)
                run.sparklingChecks.scan(area, x, z, emptyList())
                assertTrue(run.sparklingComplete(species, state))
            }
        }
    }
    @Test fun `birds need all food spawned but no capture minimums`() {
        val birds = SafariBirdLedger(); val checks = SparklingChecks()
        for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) repeat(3) {
            birds.pickup("FLOOR DROP! You found $food", SafariBiome.FOREST)
        }
        birds.inventory(emptyList())
        birds.scan(0)
        repeat(8) { birds.renderBird("Bluebird") }
        checks.scan(SafariBiome.FOREST, 6.0, 51.0, emptyList(), birdsSpawned = birds.allSpawned)
        assertFalse(checks.checked("Macaw"))
        birds.renderBird("Macaw")
        checks.scan(SafariBiome.FOREST, 6.0, 51.0, emptyList(), birdsSpawned = birds.allSpawned)
        assertTrue(listOf("Bluebird", "Parakeet", "Macaw").all(checks::checked))
    }
    private fun coins(checks: SparklingChecks, count: Int) = repeat(count) {
        checks.chat("FLOOR DROP! You found a Shining Coin!")
    }
    private fun spend(checks: SparklingChecks, count: Int) = repeat(count) {
        checks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
    }
    @Test fun `Gimmiegold reconciles missing spawn chat but still needs fresh empty inventory and no coin drops`() {
        val checks = SparklingChecks(); val gold = seen("Gimmiegold", 3)
        coins(checks, 3); spend(checks, 2); checks.inventory(emptyList())
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, gold, hauntedDropsRemaining = 0)
        assertTrue(checks.checked("Gimmiegold")) // The third loaded spawn recovers its missing chat message.
        spend(checks, 1)
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, gold, hauntedDropsRemaining = 0)
        assertFalse(checks.checked("Gimmiegold")) // Inventory sample predates last spending message.
        checks.inventory(listOf("Shining Coin" to 1))
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, gold, hauntedDropsRemaining = 0)
        assertFalse(checks.checked("Gimmiegold"))
        checks.inventory(emptyList())
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, gold, hauntedDropsRemaining = 1)
        assertTrue(checks.checked("Gimmiegold")) // Confirmed baseline pickups/spawns outrank speculative markers.
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, gold, hauntedDropsRemaining = 0)
        assertTrue(checks.checked("Gimmiegold"))
    }
    @Test fun `Gimmiegold cannot complete from unseen drops outside the biome center`() {
        val checks = SparklingChecks(); coins(checks, 3); spend(checks, 3); checks.inventory(emptyList())
        checks.scan(SafariBiome.HAUNTED, -30.0, -64.0, seen("Gimmiegold", 3), hauntedDropsRemaining = 0)
        assertFalse(checks.checked("Gimmiegold"))
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = 0)
        assertFalse(checks.checked("Gimmiegold"))
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, seen("Gimmiegold", 3), hauntedDropsRemaining = 0)
        assertTrue(checks.checked("Gimmiegold"))
    }
    @Test fun `coin shards and quoted spawn messages do not prove spending`() {
        val checks = SparklingChecks()
        checks.chat("FLOOR DROP! You found a Shining Coin Shard!")
        checks.chat("Party > Player: A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
        checks.inventory(emptyList())
        checks.scan(SafariBiome.HAUNTED, -4.0, -64.0, seen("Gimmiegold", 2), hauntedDropsRemaining = 0)
        assertFalse(checks.checked("Gimmiegold"))
    }
    @Test fun `manual party completion supports each exact case insensitive biome abbreviation`() {
        for ((word, biome) in mapOf("fD" to SafariBiome.FOREST, "CD." to SafariBiome.CAVERN,
            "id" to SafariBiome.ICY, "hd" to SafariBiome.HAUNTED)) {
            val run = SafariRun(0)
            run.sparklingChecks.chat("§9Party > §a[MVP+] Player: $word", manualAllowed = true)
            assertTrue(biome.critters.all { run.sparklingChecks.checked(it.name) == (it.name !in setOf("Wumpa", "Doomspiral")) })
            assertEquals(0, run.progress(SafariRoster.all, true))
            assertTrue(run.biomeClears.isEmpty()); assertTrue(run.uniqueBiomeClears.isEmpty())
        }
    }
    @Test fun `manual completion ignores other modes channels or words`() {
        for (text in listOf("Party > Player: fd", "Guild > Player: fd", "Player: fd", "Party > Player: fd please")) {
            val checks = SparklingChecks(); checks.chat(text, manualAllowed = text != "Party > Player: fd")
            assertFalse(checks.checked("Foxtrot"))
        }
    }
    @Test fun `Sparkling progress and missing rows count needed checks rather than catches`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        val run = SafariRun(0); val state = party(setOf("Foxtrot", "Honeybug", "Gemzie"))
        run.record(SafariCatch(SafariRoster.named("Honeybug")!!))
        run.sparklingChecks.scan(SafariBiome.FOREST, 6.0, 51.0, seen("Foxtrot", 6))
        val progress = SafariPanels.progress(run, true, 1000, state)
        assertEquals("1/2", progress.rows.single { it.label == "Forest" }.value)
        assertEquals("0/1", progress.rows.single { it.label == "Cavern" }.value)
        assertEquals("0/0", progress.rows.single { it.label == "Icy" }.value)
        assertEquals(1f, progress.rows.single { it.label == "Icy" }.fraction)
        val missing = SafariPanels.missing(run, SafariBiome.FOREST, true, 3, party = state)
        assertTrue(missing.rows.any { it.label == "Honeybug" })
        assertFalse(missing.rows.any { it.label == "Foxtrot" || it.label == "Treefrog" })
    }
    @Test fun `unknown API discoveries never present a confirmed progress denominator`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        val progress = SafariPanels.progress(SafariRun(0), true, 1000, PartySparklingState())
        assertEquals("Loading discoveries", progress.rows.first().label)
        assertEquals("?", progress.rows.single { it.label == "Forest" }.value)
    }
    @Test fun `party done hides Sparkling helpers but leaves Unique but keeps Full Clear requirements`() {
        val run = SafariRun(0); val state = party(setOf("Rockmite", "Snoozle"))
        run.sparklingChecks.chat("Party > Player: cd", manualAllowed = true)
        assertFalse(SafariHelperRules.needed("Rockmite", SafariMode.SPARKLING, run, state))
        assertFalse(SafariHelperRules.needed("Snoozle", SafariMode.SPARKLING, run, state))
        assertTrue(SafariHelperRules.needed("Rockmite", SafariMode.UNIQUE, run, state))
        assertTrue(SafariHelperRules.needed("Rockmite", SafariMode.FULL_CLEAR, run, state))
    }
}
