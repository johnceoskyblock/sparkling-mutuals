package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariIssue19FollowupTest {
    @TempDir lateinit var dir: Path
    private fun party(needs: Set<String>) = PartySparklingState().apply {
        select(setOf("one")); accept(setOf("one"), mapOf("one" to SafariRoster.all.map { it.name }.toSet() - needs))
    }
    private fun row(run: SafariRun, needs: Set<String>) = SafariPanels.progress(run, true, 1000, party(needs)).rows.single { it.label == "Icy" }.value
    @Test fun `needed Wumpa switches icy progress from sightings to captures`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        val run = SafariRun(0)
        run.sparklingChecks.scan(SafariBiome.ICY, -112.0, -54.0, emptyList())
        assertEquals("0/2", row(run, setOf("Wumpa", "Troodon")))
        run.record(SafariCatch(SafariRoster.named("Troodon")!!, personal = false))
        assertEquals("1/2", row(run, setOf("Wumpa", "Troodon")))
        assertEquals("1/1", row(run, setOf("Troodon")))
        assertEquals(0, run.personalCount("Troodon"))
    }
    @Test fun `Wumpa capture completes needed icy progress but not other biomes or PBs`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        val run = SafariRun(0)
        run.record(SafariCatch(SafariRoster.named("Wumpa")!!, personal = false))
        assertEquals("3/3", row(run, setOf("Wumpa", "Troodon", "Polaris")))
        assertFalse(run.sparklingChecks.checked("Gemzie"))
        assertTrue(run.biomeClears.isEmpty()); assertTrue(run.uniqueBiomeClears.isEmpty())
        assertEquals("0/2", row(SafariRun(0), setOf("Wumpa", "Troodon")))
    }
    @Test fun `exact global completion messages check only their species in Sparkling mode`() {
        val messages = mapOf("A rumbling sound can be heard, and the door at the back of the chamber opens..." to "Gemzie",
            "The darkness in the Haunted Biome fades away..." to "Doomspiral", "The cave is collapsing..." to "Wumpa")
        for ((message, species) in messages) {
            val run = SafariRun(0)
            SafariMessages.lines("Party > Player: $message", run.sparklingChecks)
            assertFalse(run.sparklingChecks.checked(species))
            run.sparklingChecks.chat(message)
            assertFalse(run.sparklingChecks.checked(species))
            run.sparklingChecks.chat("§a$message", manualAllowed = true)
            assertTrue(run.sparklingChecks.checked(species))
            assertEquals(1, SafariRoster.all.count { run.sparklingChecks.checked(it.name) })
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
