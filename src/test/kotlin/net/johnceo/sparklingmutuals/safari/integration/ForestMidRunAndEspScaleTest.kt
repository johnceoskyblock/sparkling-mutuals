package net.johnceo.sparklingmutuals.safari.integration

import java.nio.file.Path
import java.util.Properties
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ForestMidRunAndEspScaleTest {
    @TempDir lateinit var dir: Path
    private val birds = listOf("Bluebird", "Parakeet", "Macaw")
    private fun party() = PartySparklingState().apply {
        select(setOf("local"))
        accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - birds.toSet()))
    }
    private fun caught(run: SafariRun, name: String, n: Int, personal: Boolean = false) {
        repeat(n) { run.record(SafariCatch(SafariRoster.named(name)!!, personal)) }
    }
    private fun scan(run: SafariRun, live: List<Pair<String, UUID>> = emptyList(), drops: Int = 0,
        x: Double = 6.0, held: List<Pair<String, Int>> = emptyList()) {
        run.inventoryEvidence(held, 10000)
        run.observePopulation(live)
        run.updateCaptureEvidence(SafariBiome.FOREST, live.map { it.first }.toSet(), false,
            macawsInRange = live.count { it.first == "Macaw" }, playerX = x, playerZ = 51.0)
        run.scanBirdSources(x, 51.0, live, drops)
        run.sparklingChecks.scan(SafariBiome.FOREST, x, 51.0, live, birdsSpawned = run.birds.sparklingSpawned)
    }
    private fun cleared() = SafariRun(0).apply {
        caught(this, "Bluebird", 5); caught(this, "Parakeet", 3); caught(this, "Macaw", 2)
    }

    @Test fun `mid run shared catches resolve missing feeding history without inventing personal evidence`() {
        val run = cleared()
        scan(run)
        birds.forEach { assertTrue(run.captureComplete(it), it); assertTrue(run.sparklingComplete(it, party()), it) }
        assertEquals(0, run.birds.used)
        assertEquals(0, run.birds.collected)
        assertFalse(run.birdFoodsComplete)
        assertFalse(run.captureComplete("Macaw", personalOnly = true))
    }
    @Test fun `current live birds and catches satisfy Sparkling but retain Full Clear catch requirements`() {
        val run = SafariRun(0)
        caught(run, "Bluebird", 2); caught(run, "Parakeet", 3); caught(run, "Macaw", 1)
        val live = List(3) { "Bluebird" to UUID.randomUUID() } + ("Macaw" to UUID.randomUUID())
        scan(run, live)
        birds.forEach { assertTrue(run.sparklingComplete(it, party()), it) }
        assertFalse(run.captureComplete("Bluebird"))
        assertTrue(run.captureComplete("Parakeet"))
        assertTrue(run.captureComplete("Macaw"))
        assertTrue(run.uniqueComplete("Macaw", party()))
    }
    @Test fun `below minimum out of radius unknown and remaining drops cannot prove food resolution`() {
        for (drops in listOf(-1, 1)) {
            val run = cleared(); scan(run, drops = drops)
            assertFalse(run.birds.feedingResolved)
            assertFalse(run.sparklingComplete("Bluebird", party()))
        }
        val outside = cleared(); scan(outside, x = 21.01)
        assertFalse(outside.birds.feedingResolved)
        scan(outside, x = 21.0)
        assertTrue(outside.birds.feedingResolved)
        val few = SafariRun(0); caught(few, "Bluebird", 8); scan(few)
        assertFalse(few.birds.feedingResolved)
    }
    @Test fun `duplicate and historical UUIDs and Macaw pairs cannot inflate nine feeding opportunities`() {
        val run = SafariRun(0)
        val id = UUID.randomUUID()
        scan(run, List(9) { "Bluebird" to id })
        assertFalse(run.birds.feedingResolved)
        repeat(10) { scan(run, listOf("Bluebird" to UUID.randomUUID())) }
        assertFalse(run.birds.feedingResolved)
        scan(run, List(9) { "Macaw" to UUID.randomUUID() })
        assertFalse(run.birds.feedingResolved)
        scan(run, List(18) { "Macaw" to UUID.randomUUID() })
        assertTrue(run.birds.feedingResolved)
        assertFalse(run.uniqueComplete("Macaw", party()))
    }
    @Test fun `known held and starting food block the baseline until additional population is accounted for`() {
        val run = cleared()
        run.birds.startingGrant("Yogi Berry")
        scan(run)
        assertFalse(run.birds.feedingResolved)
        val extra = listOf("Bluebird" to UUID.randomUUID())
        scan(run, extra, held = listOf("Yogi Berry" to 1))
        assertFalse(run.birds.feedingResolved)
        scan(run, extra)
        assertTrue(run.birds.feedingResolved)
    }
    @Test fun `new pickups cannot reuse old population and Forest reentry requires another qualifying scan`() {
        val run = cleared(); run.visitBiome(SafariBiome.FOREST); scan(run)
        assertTrue(run.birds.feedingResolved)
        run.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.FOREST, 10000)
        scan(run)
        assertFalse(run.birds.feedingResolved)
        scan(run, listOf("Bluebird" to UUID.randomUUID()))
        assertTrue(run.birds.feedingResolved)
        run.visitBiome(SafariBiome.CAVERN); run.visitBiome(SafariBiome.FOREST)
        assertFalse(run.birds.feedingResolved)
    }
    @Test fun `party extra food notice requires new population and then normal catch completion`() {
        val run = cleared(); scan(run)
        assertTrue(run.resources.partyNotice("Party > Other: [SM] Unused starting item: 1x Yogi Berry -> Forest (Other)"))
        scan(run)
        assertFalse(run.captureComplete("Macaw"))
        assertFalse(run.sparklingComplete("Macaw", party()))
        scan(run, listOf("Bluebird" to UUID.randomUUID()))
        assertTrue(run.captureComplete("Macaw"))
        assertTrue(run.sparklingComplete("Macaw", party()))
        assertFalse(run.captureComplete("Bluebird"))
    }
    @Test fun `ESP scale defaults larger applies through GUI and persists across restart`() {
        ConfigManager.init(dir); ContestConfig.init(dir)
        assertEquals(2f, SafariEspConfig.textScale)
        assertEquals(.05f, SafariEspConfig.labelWorldScale)
        val settings = SafariSettings()
        settings.esp.textScale = 3.25f
        settings.apply()
        ConfigManager.init(dir)
        assertEquals(3.25f, SafariEspConfig.textScale)
        assertEquals(.025f * 3.25f, SafariEspConfig.labelWorldScale)
    }
    @Test fun `ESP scale rejects non finite input and bounds invalid saved values`() {
        for ((raw, expected) in mapOf("NaN" to 2f, "Infinity" to 2f, "wrong" to 2f, "99" to 4f, "-1" to .5f)) {
            SafariEspConfig.load(Properties().apply { setProperty("esp.textScale", raw) })
            assertEquals(expected, SafariEspConfig.textScale, raw)
        }
    }
}
