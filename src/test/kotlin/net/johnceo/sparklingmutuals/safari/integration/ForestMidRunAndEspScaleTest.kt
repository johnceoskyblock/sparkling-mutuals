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
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
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
    private fun scan(run: SafariRun, drops: Int = 0, held: List<Pair<String, Int>> = emptyList()) {
        run.inventoryEvidence(held, 10000)
        run.scanBirdSources(drops)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        run.sparklingChecks.scan(SafariBiome.FOREST, 6.0, 51.0, emptyList(), birdsSpawned = run.birds.sparklingSpawned)
    }
    private fun feed(run: SafariRun, amount: Int = 9) {
        repeat(amount) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
    }
    @Test fun `no Macaws completes all four scenarios only in Forest with drops and food resolved`() {
        val run = SafariRun(0)
        val state = party()
        run.visitBiome(SafariBiome.FOREST)
        scan(run)
        assertFalse(run.uniqueComplete("Macaw", state))
        feed(run, 8); scan(run)
        assertFalse(run.captureComplete("Macaw"))
        feed(run, 1)
        for (drops in listOf(-1, 1)) {
            scan(run, drops = drops)
            assertFalse(run.uniqueComplete("Macaw", state))
            assertFalse(run.captureComplete("Macaw"))
            assertFalse(run.sparklingComplete("Macaw", state))
        }
        scan(run, held = listOf("Yogi Berry" to 1))
        assertFalse(run.uniqueComplete("Macaw", state))
        feed(run, 1) // Inventory revealed another starting food; it needs another feeding.
        scan(run)
        assertTrue(run.uniqueComplete("Macaw", state)) // Unique + Sparkling
        val owned = PartySparklingState().apply {
            select(setOf("local")); accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet()))
        }
        assertTrue(run.uniqueComplete("Macaw", owned))
        assertTrue(run.captureComplete("Macaw"))
        assertTrue(run.sparklingComplete("Macaw", state))
        ConfigManager.fullClearMode = true; ConfigManager.sparklingMode = false
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, false, 0, party = state).rows.any { it.label == "Macaw" })
        assertEquals(0xFF55FF55.toInt(), SafariPanels.captures(run, SafariBiome.FOREST).rows.single { it.label == "Macaw" }.valueColor)
        assertEquals("2/9", SafariPanels.progress(run, true, 0, state).rows.single { it.label == "Forest" }.value,
            "local no-Parakeet and no-Macaw outcomes both advance Full Clear without fd")
        ConfigManager.fullClearMode = false
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == "Macaw" })
        assertEquals("1/9", SafariPanels.progress(run, true, 0, state).rows.single { it.label == "Forest" }.value)
        ConfigManager.sparklingMode = true
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, false, 0, party = state).rows.any { it.label == "Macaw" })
        ConfigManager.sparklingMode = false
        assertEquals(0, run.count("Macaw")); assertEquals(0, run.personalCount("Macaw"))
        run.visitBiome(SafariBiome.CAVERN)
        assertTrue(run.uniqueUnavailable("Macaw"), "leaving Forest preserves a locally proven result")
        val remote = SafariRun(0); remote.visitBiome(SafariBiome.CAVERN)
        feed(remote); scan(remote)
        assertFalse(remote.uniqueUnavailable("Macaw"), "an outside scan cannot first establish the no-Macaw exception")
    }
    @Test fun `escape UUID history cannot inflate current nearby bird counts`() {
        val run = SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        caught(run, "Bluebird", 8)
        repeat(12) {
            val live = List(18) { "Macaw" to UUID.randomUUID() }
            run.observePopulation(live)
            run.sparklingChecks.scan(SafariBiome.FOREST, 6.0, 51.0, live)
        }
        scan(run)
        assertFalse(run.birds.feedingResolved)
        assertEquals(0, run.birds.used)
        assertEquals(0, run.sparklingChecks.checkCount("Macaw"))
    }
    @Test fun `shared captures plus current birds prove spawning without distinct UUID checks`() {
        val run = SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        caught(run, "Bluebird", 5)
        scan(run)
        assertFalse(run.birds.feedingResolved, "personal catches are already included in shared catches")
        run.scanBirdSources(0, listOf("Bluebird", "Parakeet", "Macaw", "Macaw"))
        assertEquals(8, run.birds.accountedFeedings) // Five captures, two normal birds, one Macaw pair.
        assertFalse(run.birds.feedingResolved)
        run.scanBirdSources(0, listOf("Bluebird", "Parakeet", "Parakeet", "Macaw", "Macaw"))
        assertEquals(9, run.birds.accountedFeedings)
        assertTrue(run.birds.feedingResolved)
        assertEquals(0, run.birds.used)
        run.scanBirdSources(0)
        assertFalse(run.birds.feedingResolved, "a past scan is not the current nearby population")
    }
    @Test fun `one captured Macaw and one scanned Macaw account for one food`() {
        val run = SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        caught(run, "Bluebird", 8)
        caught(run, "Macaw", 1)
        scan(run)
        assertEquals(8, run.birds.accountedFeedings, "half a pair cannot establish another feeding")
        run.scanBirdSources(0, listOf("Macaw"))
        assertEquals(9, run.birds.accountedFeedings)
        assertTrue(run.birds.feedingResolved)
        assertTrue(run.captureComplete("Macaw"), "the one-catch Full Clear exception still applies")
    }
    @Test fun `Macaw pairs consume one food and prevent the no Macaw exception`() {
        val run = SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        repeat(5) { run.birds.spawn("Two Macaws were attracted to the Birdfeeder!") }
        scan(run)
        assertEquals(10, run.birds.spawnedCount("Macaw"))
        assertFalse(run.birds.feedingResolved)
        feed(run, 4); scan(run)
        assertTrue(run.birds.feedingResolved)
        assertFalse(run.uniqueComplete("Macaw", party()))
        assertFalse(run.captureComplete("Macaw"))
        caught(run, "Macaw", 1)
        assertTrue(run.uniqueComplete("Macaw", party()))
        assertTrue(run.captureComplete("Macaw"))
    }
    @Test fun `starting food and later food require additional announcements not extra UUIDs`() {
        val run = SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        run.birds.startingGrant("Yogi Berry")
        feed(run); scan(run)
        assertFalse(run.birds.feedingResolved)
        feed(run, 1); scan(run)
        assertTrue(run.birds.feedingResolved)
        run.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.FOREST, 10000)
        scan(run)
        assertFalse(run.birds.feedingResolved)
        feed(run, 1); scan(run)
        assertTrue(run.birds.feedingResolved)
        run.visitBiome(SafariBiome.CAVERN); run.visitBiome(SafariBiome.FOREST)
        assertFalse(run.birds.feedingResolved)
        scan(run)
        assertTrue(run.birds.feedingResolved)
    }
    @Test fun `party extra food notice requires another feeder announcement`() {
        val run = SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        feed(run); scan(run)
        assertTrue(run.resources.partyNotice("Party > Other: [SM] Unused starting item: 1x Yogi Berry -> Forest (Other)"))
        caught(run, "Bluebird", 9); scan(run)
        assertFalse(run.uniqueComplete("Macaw", party()))
        assertTrue(run.resources.pending(SafariBiome.FOREST))
        feed(run, 1); scan(run)
        assertFalse(run.resources.pending(SafariBiome.FOREST))
        assertTrue(run.uniqueComplete("Macaw", party()))
        assertTrue(run.sparklingComplete("Macaw", party()))
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
