package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class SparklingEspChecksTest {
    @TempDir lateinit var dir: Path
    private fun party(vararg needs: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - needs.toSet()))
    }
    @Test fun `Wumpa check lists uncaught Icy prerequisites even after the center check`() {
        ConfigManager.init(dir); ConfigManager.sparklingMode = true
        val run = SafariRun(0); val state = party("Wumpa")
        run.sparklingChecks.scan(SafariBiome.ICY, -112.0, -54.0, emptyList())
        fun names() = SafariPanels.missing(run, SafariBiome.ICY, true, 0, party = state).rows.map { it.label }
        assertEquals(listOf("Strongarm", "Tepid", "Polaris", "Shuddersquid", "Billygoat", "Mantis Shrimp", "Nozzlenose", "Troodon", "Wumpa"), names())
        run.record(SafariCatch(SafariRoster.named("Strongarm")!!))
        assertFalse("Strongarm" in names()); assertTrue("Troodon" in names())
        run.sparklingChecks.chat("Party > Friend: id", manualAllowed = true)
        assertEquals(listOf("All checked!"), names())
    }
    @Test fun `each needed UUID gets ten seconds even after its species center check`() {
        val run = SafariRun(0); val state = party("Flitter"); val first = UUID.randomUUID(); val second = UUID.randomUUID()
        run.sparklingChecks.scan(SafariBiome.CAVERN, -114.0, 49.0, emptyList())
        fun visible(id: UUID, now: Long) = SafariEspRules.neededForSparkling("Flitter", false, state, false, run, id, now)
        assertTrue(visible(first, 1000)); assertTrue(visible(first, 10999))
        assertFalse(visible(first, 11000)); assertTrue(visible(second, 11000))
        assertFalse(visible(first, 20000)); assertTrue(visible(second, 20999)); assertFalse(visible(second, 21000))
        assertTrue(SafariEspRules.neededForSparkling("Flitter", false, state, false, SafariRun(30000), first, 30000))
        assertEquals(0, run.count("Flitter")); assertTrue(run.biomeClears.isEmpty())
    }
    @Test fun `Wumpa prerequisites stop after capture unless profitable highlighting is enabled`() {
        val run = SafariRun(0); val state = party("Wumpa"); val tepid = UUID.randomUUID(); val shrimp = UUID.randomUUID()
        fun visible(name: String, id: UUID, now: Long, profitable: Boolean = false) =
            SafariEspRules.neededForSparkling(name, false, state, profitable, run, id, now)
        assertTrue(visible("Tepid", tepid, 1000)); assertTrue(visible("Tepid", tepid, 21000))
        run.record(SafariCatch(SafariRoster.named("Tepid")!!))
        assertFalse(visible("Tepid", tepid, 21001))
        run.record(SafariCatch(SafariRoster.named("Mantis Shrimp")!!))
        assertFalse(visible("Mantis Shrimp", shrimp, 22000)); assertTrue(visible("Mantis Shrimp", shrimp, 22000, true))
        run.sparklingChecks.chat("Party > Friend: id", manualAllowed = true)
        assertFalse(visible("Troodon", UUID.randomUUID(), 23000))
    }
    @Test fun `profitable Cavern ESP bypasses lifetime only while enabled`() {
        val run = SafariRun(0); val state = party("Chuckwalla"); val id = UUID.randomUUID()
        assertTrue(SafariEspRules.neededForSparkling("Chuckwalla", false, state, false, run, id, 0))
        assertFalse(SafariEspRules.neededForSparkling("Chuckwalla", false, state, false, run, id, 10000))
        assertTrue(SafariEspRules.neededForSparkling("Chuckwalla", false, state, true, run, id, 10000))
    }
    @Test fun `Rockmite mound checks stay visible until the structures are checked`() {
        val run = SafariRun(0); val state = party("Rockmite"); val id = UUID.randomUUID()
        assertTrue(SafariEspRules.neededForSparkling("Rockmite", true, state, false, run, id, 0))
        assertTrue(SafariEspRules.neededForSparkling("Rockmite", true, state, false, run, id, 20000))
        run.sparklingChecks.chat("Party > Friend: cd", manualAllowed = true)
        assertFalse(SafariEspRules.neededForSparkling("Rockmite", true, state, false, run, id, 21000))
    }
}
