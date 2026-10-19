package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariChatFilter
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SparklingBossCheckIsolationTest {
    @TempDir lateinit var dir: Path
    private fun party(needs: Set<String>) = PartySparklingState().apply {
        select(setOf("one")); accept(setOf("one"), mapOf("one" to SafariRoster.all.map { it.name }.toSet() - needs))
    }
    private fun row(run: SafariRun, needs: Set<String>) = SafariPanels.progress(run, true, 1000, party(needs)).rows.single { it.label == "Icy" }.value
    @Test fun `needed Wumpa does not change other species count checks`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        val run = SafariRun(0)
        run.sparklingChecks.scan(SafariBiome.ICY, -112.0, -54.0, List(3) { "Troodon" to java.util.UUID.randomUUID() })
        assertEquals("1/2", row(run, setOf("Wumpa", "Troodon")))
        run.record(SafariCatch(SafariRoster.named("Troodon")!!, personal = false))
        assertEquals("1/2", row(run, setOf("Wumpa", "Troodon")))
        assertEquals("1/1", row(run, setOf("Troodon")))
        assertEquals(0, run.personalCount("Troodon"))
    }
    @Test fun `Wumpa capture completes all Icy checks after scanning`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        val run = SafariRun(0)
        run.record(SafariCatch(SafariRoster.named("Wumpa")!!, personal = false))
        run.sparklingChecks.scan(SafariBiome.ICY, -110.0, -78.0, emptyList())
        assertEquals("3/3", row(run, setOf("Wumpa", "Troodon", "Polaris")))
        assertFalse(run.sparklingChecks.checked("Gemzie"))
        assertTrue(run.biomeClears.isEmpty()); assertTrue(run.uniqueBiomeClears.isEmpty())
        assertEquals("0/2", row(SafariRun(0), setOf("Wumpa", "Troodon")))
    }
    @Test fun `global slain messages cannot replace confirmed boss captures in Sparkling mode`() {
        val messages = mapOf(
            "The darkness in the Haunted Biome fades away..." to "Doomspiral", "The cave opens up again..." to "Wumpa")
        for ((message, species) in messages) {
            val run = SafariRun(0)
            SafariMessages.lines("Party > Player: $message", run.sparklingChecks)
            assertFalse(run.sparklingChecks.checked(species))
            run.sparklingChecks.chat(message)
            assertFalse(run.sparklingChecks.checked(species))
            run.sparklingChecks.chat("§a$message", manualAllowed = true)
            assertFalse(run.sparklingChecks.checked(species))
            assertEquals(0, SafariRoster.all.count { run.sparklingChecks.checked(it.name) })
            assertEquals(0, run.count(species)); assertTrue(run.biomeClears.isEmpty())
        }
    }
    @Test fun `mound noise filter respects toggle and leaves useful global evidence visible`() {
        for (message in listOf("Small cracks begin to form in the mound...", "The cracks seem to be getting larger, keep hitting it!",
            "Chunks of the mound begin falling away...", "The mound is about to fall to pieces! Keep going!")) {
            assertTrue(SafariChatFilter.hidden("§a$message", true))
            assertFalse(SafariChatFilter.hidden(message, false))
            assertFalse(SafariChatFilter.hidden("Party > Player: $message", true))
        }
        assertFalse(SafariChatFilter.hidden("The cave is collapsing...", true))
    }
}
