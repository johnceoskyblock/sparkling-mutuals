package net.johnceo.sparklingmutuals.safari.ui

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BiomeObjectivesCleanupTest {
    @TempDir lateinit var dir: Path
    private fun party()=PartySparklingState().apply { select(setOf("me"));accept(setOf("me"),mapOf("me" to emptySet())) }
    @Test fun `unfinished critters keep a mode instruction instead of claiming all objectives complete`() {
        ConfigManager.init(dir)
        val run=SafariRun(0).apply { visitBiome(SafariBiome.CAVERN) }
        for((mode,label) in listOf(SafariMode.UNIQUE to "Capture all uniques",SafariMode.FULL_CLEAR to "Capture all critters",
            SafariMode.SPARKLING to "Check needed critters")) {
            ConfigManager.fullClearMode=mode==SafariMode.FULL_CLEAR
            ConfigManager.sparklingMode=mode==SafariMode.SPARKLING
            val rows=SafariPanels.objectives(run,SafariBiome.CAVERN,true,0,party=party()).rows
            assertTrue(rows.any { it.label==label })
            assertFalse(rows.any { it.label=="All objectives complete!" })
        }
    }
    @Test fun `first Forest entry controls whether opted-out starting food counts`() {
        ConfigManager.init(dir)
        ConfigManager.announceStartingItems = false
        val food = listOf("Wriggleworm" to 1)
        for ((entryAt, expected) in listOf(30_000L to 4, 30_001L to 3)) {
            val run = SafariRun(0)
            run.inventoryEvidence(food, 0)
            run.visitBiome(SafariBiome.FOREST, entryAt)
            run.scanBirdSources(3)
            for (now in listOf(entryAt, 40_000L)) {
                run.inventoryEvidence(food, now)
                assertEquals(expected, run.knownBirdFoodLeftToUse)
                assertEquals(expected - 3, run.birds.held("Wriggleworm"))
            }
        }
    }
    @Test fun `Forest shows only positive food and spawned species and removes bird rows after mode completion`() {
        ConfigManager.init(dir);ConfigManager.fullClearMode=false;ConfigManager.sparklingMode=false
        val run=SafariRun(0).apply {
            visitBiome(SafariBiome.FOREST)
            inventoryEvidence(emptyList(),0)
            inventoryEvidence(listOf("Wriggleworm" to 1),10000)
            scanBirdSources(0)
        }
        fun rows()=SafariPanels.objectives(run,SafariBiome.FOREST,true,0,party=party()).rows
        assertTrue(rows().none { it.label=="Spawned" })
        assertEquals(1,rows().single { it.counters.isNotEmpty() }.counters.size)
        assertEquals(1,rows().single { it.counters.isNotEmpty() }.counters.single().count)
        run.birds.spawn("A Bluebird was attracted to the Birdfeeder!")
        run.scanBirdSources(0, listOf("Bluebird"), qualified = false)
        assertEquals(1,rows().single { it.label=="Spawned" }.counters.size)
        run.scanBirdSources(0, qualified = false)
        assertTrue(rows().none { it.label == "Spawned" })
        run.inventoryEvidence(emptyList(),11000)
        run.scanBirdSources(0,List(9) { "Bluebird" })
        run.record(SafariCatch(SafariRoster.named("Bluebird")!!))
        assertTrue(rows().none { it.label in setOf("Spawned","Feeder","Bird food left to use") || it.counters.isNotEmpty() })
        assertTrue(rows().any { it.label=="Capture all uniques" },"Other Forest critters are still missing")
        // Per-type source evidence can hide used seeds while other food is still on the floor.
        val birds=net.johnceo.sparklingmutuals.safari.resources.SafariBirdLedger()
        birds.floor(2);birds.floorTypes(mapOf("Bag of Seeds" to 0,"Wriggleworm" to 2,"Yogi Berry" to 0))
        assertEquals(0,birds.remaining("Bag of Seeds"));assertEquals(2,birds.remaining("Wriggleworm"))
        birds.floor(3); birds.floorTypes(null)
        assertEquals(2, birds.remaining("Wriggleworm"), "A partial scan must not replace classified counts with guesses")
        birds.floor(1); birds.floorTypes(mapOf("Bag of Seeds" to 0, "Wriggleworm" to 1, "Yogi Berry" to 0))
        assertEquals(1, birds.remaining("Wriggleworm"))
    }
}
