package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariFullClearProgressTest {
    @TempDir lateinit var dir: Path
    private fun row(run: SafariRun, biome: SafariBiome) = SafariPanels.progress(run, true, 0).rows.first { it.label == biome.label }.value
    @Test fun `full clear progress uses captures evidence and can reopen when critters return`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run = SafariRun(0); run.visitBiome(SafariBiome.CAVERN)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!)) }
        assertEquals("0/9", row(run, SafariBiome.CAVERN))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertEquals("0/9", row(run, SafariBiome.CAVERN))
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        assertEquals("1/9", row(run, SafariBiome.CAVERN))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertEquals("0/9", row(run, SafariBiome.CAVERN))
    }
    @Test fun `structure and absent bird completion match the captures panel`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run = SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Honeybug")!!)) }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, false)
        assertEquals("0/9", row(run, SafariBiome.FOREST))
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true)
        val expected = SafariBiome.FOREST.critters.count { run.captureComplete(it.name) }
        assertEquals("$expected/9", row(run, SafariBiome.FOREST)); assertEquals(1, expected)
    }
    @Test fun `all party done words complete only unvisited biomes without capture or PB changes`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        for ((word, biome) in mapOf("fD" to SafariBiome.FOREST, "CD." to SafariBiome.CAVERN, "id" to SafariBiome.ICY, "hd" to SafariBiome.HAUNTED)) {
            val run = SafariRun(0)
            assertTrue(SafariMessages.lines("Party > [MVP+] Player: $word", fullClearRun = run).isEmpty())
            assertEquals("${biome.critters.size}/${biome.critters.size}", row(run, biome))
            assertEquals(0, run.progress(SafariRoster.all, true)); assertTrue(run.biomeClears.isEmpty())
            assertTrue(SafariRoster.all.none { run.sparklingChecks.checked(it.name) })
            assertTrue(biome.critters.none { run.captureComplete(it.name) })
        }
    }
    @Test fun `visited biome retains party completion until contradictory local evidence`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run = SafariRun(0)
        SafariMessages.lines("Party > Player: cd", fullClearRun = run)
        assertEquals("9/9", row(run, SafariBiome.CAVERN))
        run.visitBiome(SafariBiome.CAVERN)
        SafariMessages.lines("Party > Player: cd", fullClearRun = run)
        assertEquals("9/9", row(run, SafariBiome.CAVERN))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertEquals("0/9", row(run, SafariBiome.CAVERN))
    }
    @Test fun `non party and multiline quotations cannot clear Full Clear progress`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run = SafariRun(0)
        for (message in listOf("Guild > Player: cd", "From Player: cd", "Player: cd", "Party > Player: cd please", "Party > Player: cd\nCAPTURE! You caught a Gemzie!", "Party > Player: cd\\nCAPTURE! You caught a Gemzie!")) {
            SafariMessages.lines(message, fullClearRun = run)
            assertEquals("0/9", row(run, SafariBiome.CAVERN))
        }
    }
    @Test fun `manual completion is run owned and Unique progress retains one catch rules`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run = SafariRun(0); SafariMessages.lines("Party > Player: cd", fullClearRun = run)
        assertEquals("0/9", row(SafariRun(1), SafariBiome.CAVERN))
        SafariFullClear.select(SafariMode.UNIQUE)
        assertEquals("0/9", row(run, SafariBiome.CAVERN))
        run.record(SafariCatch(SafariRoster.named("Driftling")!!))
        assertEquals("1/9", row(run, SafariBiome.CAVERN))
    }
}
