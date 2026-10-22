package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariFullClearProgressTest {
    @TempDir lateinit var dir: Path
    private fun row(run: SafariRun, biome: SafariBiome) = SafariPanels.progress(run, true, 0).rows.first { it.label == biome.label }.value
    @Test fun `current biome progresses from local clear while an unvisited biome waits for its report`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val examples = listOf("Foxtrot", "Driftling", "Billygoat", "Bloodbat")
        val words = mapOf(SafariBiome.FOREST to "fd", SafariBiome.CAVERN to "cd",
            SafariBiome.ICY to "id", SafariBiome.HAUNTED to "hd")
        for ((index, name) in examples.withIndex()) {
            val run = SafariRun(0)
            val local = SafariRoster.named(name)!!
            val remote = SafariRoster.named(examples[(index + 1) % examples.size])!!
            run.visitBiome(local.biome)
            for (critter in listOf(local, remote)) repeat(SafariFullClear.minimum(critter.name)) {
                run.record(SafariCatch(critter, false))
            }
            assertFalse(run.fullClearComplete(name), "captures alone are not a local clear")
            val zone = SafariCompletionZones.forBiome(local.biome).getValue(name)
            run.updateCaptureEvidence(local.biome, emptySet(), false, playerX = zone.x, playerZ = zone.z)
            assertTrue(run.fullClearComplete(name))
            assertEquals("1/${local.biome.critters.size}", row(run, local.biome))
            assertFalse(run.fullClearComplete(remote.name), "unvisited biome still awaits party evidence")
            run.partyFullClear("Party > Friend: ${words.getValue(remote.biome)}")
            assertTrue(run.fullClearComplete(remote.name))
            run.visitBiome(remote.biome)
            assertTrue(run.fullClearComplete(name), "leaving preserves the already verified local clear")
        }
    }
    @Test fun `variable progress waits for report then positive remaining critters reopen it`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run=SafariRun(0); run.visitBiome(SafariBiome.CAVERN)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!), now = 0L) }
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        assertEquals("0/9", row(run, SafariBiome.CAVERN))
        run.partyFullClear("Party > Friend: cd")
        assertTrue(run.fullClearComplete("Driftling"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertFalse(run.fullClearComplete("Driftling"))
        run.record(SafariCatch(SafariRoster.named("Driftling")!!), now = 0L)
        assertTrue(run.fullClearComplete("Driftling"))
    }
    @Test fun `local Honeybug capture and source proof advances progress without waiting for report`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run=SafariRun(0); run.visitBiome(SafariBiome.FOREST)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Honeybug")!!)) }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true)
        assertTrue(run.captureComplete("Honeybug"))
        assertTrue(run.fullClearComplete("Honeybug"))
        run.partyFullClear("Party > Friend: fd")
        assertTrue(run.fullClearComplete("Honeybug"))
    }
    @Test fun `party reports never waive positive capture minima or fabricate PB evidence`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        for ((word,biome) in mapOf("fd" to SafariBiome.FOREST,"cd" to SafariBiome.CAVERN,"id" to SafariBiome.ICY,"hd" to SafariBiome.HAUNTED)) {
            val run=SafariRun(0)
            SafariMessages.lines("Party > Player: $word", fullClearRun=run)
            biome.critters.filter { SafariFullClear.minimum(it.name)>0 && it.name != "Honeybug" }.forEach { assertFalse(run.fullClearComplete(it.name),it.name) }
            if (biome == SafariBiome.FOREST) assertFalse(run.fullClearComplete("Honeybug"), "Full Clear still needs the retained nest population captured")
            assertEquals(0,run.progress(SafariRoster.all,true))
            assertTrue(run.biomeClears.isEmpty()); assertTrue(run.catches.isEmpty())
        }
    }
    @Test fun `reported completion survives entry without an exhaustive local survey`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run=SafariRun(0)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!,false)) }
        run.partyFullClear("Party > Player: cd")
        run.visitBiome(SafariBiome.CAVERN)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(),false)
        assertTrue(run.fullClearComplete("Driftling"))
        assertEquals(0,run.personalCount("Driftling"))
    }
    @Test fun `non party messages and multiline quotations cannot report completion`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run=SafariRun(0)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!)) }
        for (message in listOf("Guild > Player: cd","From Player: cd","Player: cd","Party > Player: cd please","Party > Player: cd\nCAPTURE! You caught a Gemzie!")) {
            SafariMessages.lines(message,fullClearRun=run)
            assertFalse(run.fullClearComplete("Driftling"))
        }
    }
    @Test fun `reports are run owned and Unique still advances on the first species catch`() {
        ConfigManager.init(dir); SafariFullClear.select(SafariMode.FULL_CLEAR)
        val run=SafariRun(0); run.partyFullClear("Party > Player: cd")
        assertEquals("0/9",row(SafariRun(1),SafariBiome.CAVERN))
        SafariFullClear.select(SafariMode.UNIQUE)
        assertEquals("2/9",row(run,SafariBiome.CAVERN))
        run.record(SafariCatch(SafariRoster.named("Driftling")!!))
        assertEquals("3/9",row(run,SafariBiome.CAVERN))
    }
}
