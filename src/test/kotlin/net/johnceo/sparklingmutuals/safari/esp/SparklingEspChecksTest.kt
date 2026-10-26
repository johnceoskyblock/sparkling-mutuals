package net.johnceo.sparklingmutuals.safari.esp

import java.nio.file.Path
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SparklingEspChecksTest : net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture() {
    @Test fun `profitable Gimmiegold enables Sparkle coin source ESP without needing its shiny`() {
        val quote = net.johnceo.sparklingmutuals.history.BazaarPrices
        prices(mapOf(quote.shardId("Gimmiegold") to doubleArrayOf(1000.0, 1000.0)))
        val state = net.johnceo.sparklingmutuals.safari.resources.FloorDropState()
        val coin = net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN
        val mode = net.johnceo.sparklingmutuals.safari.SafariMode.SPARKLING
        val shared = party()
        fun profitable(enabled: Boolean) = SafariEspRules.profitableEsp("Gimmiegold", enabled)
        assertFalse(state.needed(coin, mode, shared))
        assertTrue(state.needed(coin, mode, shared, profitable(true)))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, mode, true, null, shared, profitable(true)))
        assertFalse(state.enabledForMode(SafariBiome.HAUNTED, mode, false, null, shared, profitable(true)))
        assertFalse(state.needed(net.johnceo.sparklingmutuals.safari.resources.FloorDropType.INCENSE, mode, shared, profitable(true)))
        assertFalse(state.needed(coin, mode, shared, profitable(false)))
        assertFalse(state.enabledForMode(SafariBiome.HAUNTED, mode, true, null, shared, profitable(false)))
        prices(emptyMap())
        assertFalse(state.needed(coin, mode, shared, profitable(true)), "missing/non-top prices add no profitable source work")
        assertTrue(state.needed(coin, mode, party("Gimmiegold")), "needed shiny coins remain visible with the profitable option off")
    }
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
        assertTrue("Troodon" in names())
    }
    @Test fun `automatic checks never highlight ordinary critters even on first sight or a new UUID`() {
        val run = SafariRun(0)
        for (critter in SafariRoster.all.filter { it.name !in setOf("Wumpa", "Doomspiral") }) {
            val state = party(critter.name)
            for (now in listOf(0L, 1000L, 10999L, 11000L, 60000L)) {
                assertFalse(SafariEspRules.neededForSparkling(critter.name, false, state, false, run, UUID.randomUUID(), now), critter.name)
                assertFalse(SafariEspRules.neededForSparkling(critter.name, false, state, false), critter.name)
            }
        }
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
        assertTrue(visible("Troodon", UUID.randomUUID(), 23000))
    }
    @Test fun `profitable Cavern ESP remains independently selectable`() {
        val run = SafariRun(0); val state = party("Chuckwalla"); val id = UUID.randomUUID()
        assertFalse(SafariEspRules.neededForSparkling("Chuckwalla", false, state, false, run, id, 0))
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
