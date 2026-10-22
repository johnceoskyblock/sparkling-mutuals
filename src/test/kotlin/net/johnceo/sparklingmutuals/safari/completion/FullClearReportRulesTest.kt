package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FullClearReportRulesTest {
    private fun caught(run: SafariRun, name: String, count: Int) = repeat(count) {
        run.record(SafariCatch(SafariRoster.named(name)!!, false), now = 0L)
    }
    @Test fun `variable species wait for report and real minimum captures without a local UUID survey`() {
        for ((name, word) in listOf("Driftling" to "cd", "Treefrog" to "fd", "Bloodbat" to "hd")) {
            val run = SafariRun(0); val minimum = SafariFullClear.minimum(name)
            caught(run, name, minimum - 1)
            run.partyFullClear("Party > Friend: $word")
            assertFalse(run.fullClearComplete(name))
            caught(run, name, 1)
            assertTrue(run.fullClearComplete(name), name)
            assertEquals(0, run.personalCount(name))
            val unreported = SafariRun(0)
            caught(unreported, name, minimum)
            unreported.updateCaptureEvidence(SafariRoster.named(name)!!.biome, emptySet(), false)
            assertTrue(unreported.captureComplete(name), "local proof can send our own report")
            assertFalse(unreported.fullClearComplete(name), "variable progress still waits for that report")
        }
    }
    @Test fun `fixed spawn progress uses catches while missing panel still needs local proof`() {
        ConfigManager.fullClearMode = true; ConfigManager.sparklingMode = false
        for ((name, minimum) in SafariFullClear.fixedCaptureTargets) {
            val run = SafariRun(0); val species = SafariRoster.named(name)!!
            caught(run, name, minimum - 1)
            assertFalse(run.fullClearComplete(name))
            caught(run, name, 1)
            assertTrue(run.fullClearComplete(name))
            assertTrue(SafariPanels.missing(run, species.biome, false, 0).rows.any { it.label == name })
            val zone = SafariCompletionZones.forBiome(species.biome)[name]
            run.updateCaptureEvidence(species.biome, emptySet(), false,
                playerX = zone?.x ?: 0.0, playerZ = zone?.z ?: 0.0)
            assertFalse(SafariPanels.missing(run, species.biome, false, 0).rows.any { it.label == name })
            assertEquals(0xFF55FF55.toInt(), SafariPanels.captures(run, species.biome).rows.single { it.label == name }.valueColor)
        }
    }
    @Test fun `positive reentry evidence reopens only its species until remaining captures arrive`() {
        val run = SafariRun(0)
        caught(run, "Driftling", 3); caught(run, "Gemzie", 3)
        run.partyFullClear("Party > Friend: cd")
        assertTrue(run.fullClearComplete("Driftling")); assertTrue(run.fullClearComplete("Gemzie"))
        run.visitBiome(SafariBiome.CAVERN)
        assertTrue(run.fullClearComplete("Driftling"), "entry alone and unknown coverage are not contradictions")
        run.observePopulation(List(3) { "Driftling" to UUID.randomUUID() })
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertFalse(run.fullClearComplete("Driftling")); assertTrue(run.fullClearComplete("Gemzie"))
        run.partyFullClear("Party > Friend: cd")
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        assertFalse(run.fullClearComplete("Driftling"), "unloading and another report do not capture known remaining critters")
        caught(run, "Driftling", 3)
        assertTrue(run.fullClearComplete("Driftling"), "captures resolve it without seeing old UUIDs again")
    }
    @Test fun `escape replacement UUIDs never accumulate more than the current observed population`() {
        val run = SafariRun(0); caught(run, "Driftling", 3)
        run.partyFullClear("Party > Friend: cd"); run.visitBiome(SafariBiome.CAVERN)
        repeat(10) { run.observePopulation(listOf("Driftling" to UUID.randomUUID())) }
        assertFalse(run.fullClearComplete("Driftling"))
        caught(run, "Driftling", 1)
        assertTrue(run.fullClearComplete("Driftling"))
    }
    @Test fun `Macaw one catch exception ignores additional Macaws found after the report`() {
        val run = SafariRun(0); caught(run, "Macaw", 1)
        run.partyFullClear("Party > Friend: fd"); run.visitBiome(SafariBiome.FOREST)
        run.observePopulation(List(12) { "Macaw" to UUID.randomUUID() })
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false, macawsInRange = 12)
        run.inventoryEvidence(emptyList(), 10000)
        caught(run, "Bluebird", 3)
        run.scanBirdSources(0, List(11) { "Macaw" })
        assertTrue(run.fullClearComplete("Macaw"))
        run.observeSparkling(UUID.randomUUID(), "Macaw")
        assertTrue(run.fullClearComplete("Macaw"), "detection cannot override the normal one-Macaw exception")
    }
    @Test fun `report trust does not waive positively observed unbroken spawn sources`() {
        val run = SafariRun(0)
        run.partyFullClear("Party > Friend: cd")
        assertTrue(run.fullClearComplete("Rockmite")); assertTrue(run.fullClearComplete("Snoozle"))
        val site = Triple(-100, 40, 50)
        run.moundSurvey.scan(setOf(site)) { true }
        run.observeCavernSources(true, true, false)
        assertFalse(run.fullClearComplete("Rockmite")); assertFalse(run.fullClearComplete("Snoozle"))
        run.moundSurvey.scan(emptySet()) { true }
        run.observeCavernSources(false, false, true)
        assertTrue(run.fullClearComplete("Rockmite")); assertTrue(run.fullClearComplete("Snoozle"))
    }
    @Test fun `a Forest report alone cannot invent the no Macaw outcome`() {
        val run = SafariRun(0)
        run.partyFullClear("Party > Friend: fd")
        assertFalse(run.fullClearComplete("Macaw"))
        caught(run, "Macaw", 1)
        caught(run, "Bluebird", 8)
        assertTrue(run.fullClearComplete("Macaw"))
    }
    @Test fun `positive remainder seen from another biome reopens reported species until captured`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        caught(run, "Driftling", 3); caught(run, "Gemzie", 3)
        run.partyFullClear("Party > Friend: cd")
        assertTrue(run.fullClearComplete("Driftling"))
        val id = UUID.randomUUID()
        run.observePopulation(listOf("Driftling" to id))
        assertFalse(run.fullClearComplete("Driftling"))
        assertTrue(run.fullClearComplete("Gemzie"))
        run.observePopulation(emptyList())
        run.partyFullClear("Party > Friend: cd")
        assertFalse(run.fullClearComplete("Driftling"), "unloading or another report does not catch it")
        caught(run, "Driftling", 1)
        assertTrue(run.fullClearComplete("Driftling"))
    }
    @Test fun `known held food and extra coins cannot be waived by an old completion report`() {
        val birds = SafariRun(0)
        caught(birds, "Macaw", 1)
        caught(birds, "Bluebird", 8)
        birds.partyFullClear("Party > Friend: fd")
        assertTrue(birds.fullClearComplete("Macaw"))
        birds.inventoryEvidence(listOf("Yogi Berry" to 1), 10000)
        assertFalse(birds.fullClearComplete("Macaw"))
        assertFalse(birds.fullClearComplete("Bluebird"))
        val gold = SafariRun(0)
        gold.inventoryEvidence(emptyList(), 0)
        caught(gold, "Gimmiegold", 3)
        gold.partyFullClear("Party > Friend: hd")
        assertTrue(gold.fullClearComplete("Gimmiegold"))
        gold.inventoryEvidence(listOf("Shining Coin" to 1), 10000)
        assertFalse(gold.fullClearComplete("Gimmiegold"))
        repeat(4) { gold.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin on the ground!") }
        repeat(4) { gold.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!") }
        gold.inventoryEvidence(emptyList(), 14000)
        assertFalse(gold.fullClearComplete("Gimmiegold"), "the extra actual coin increases required catches")
        caught(gold, "Gimmiegold", 1)
        assertTrue(gold.fullClearComplete("Gimmiegold"))
    }

}
