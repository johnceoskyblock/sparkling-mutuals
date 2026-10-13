package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariPartyClearTest {
    private fun catch(run: SafariRun, name: String, amount: Int, personal: Boolean = false) =
        repeat(amount) { run.record(SafariCatch(SafariRoster.named(name)!!, personal)) }
    @Test fun `party-cleared Forest uses minimums and rendered critters after first entry`() {
        val ledger = SafariLedger().apply { arrive(0) }; val run = ledger.current!!
        catch(run, "Honeybug", 3)
        assertFalse(run.captureComplete("Honeybug"))
        ledger.update(SafariLocation.INSIDE, SafariBiome.FOREST, 100)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertTrue(run.captureComplete("Honeybug"))
        assertTrue(run.partyNestsHandled)
        assertTrue(run.captureComplete("Bluebird")) // Food and feeding happened before arrival.
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Honeybug", "Bluebird"), false)
        assertFalse(run.captureComplete("Honeybug")); assertFalse(run.captureComplete("Bluebird"))
        assertFalse(run.captureComplete("Treefrog")) // Minimum still applies.
        assertFalse(run.captureComplete("Honeybug", personalOnly = true, speciesInRange = emptySet()))
    }
    @Test fun `party-cleared Cavern display does not require local structure checks`() {
        val ledger = SafariLedger().apply { arrive(0) }; val run = ledger.current!!
        catch(run, "Driftling", 3)
        ledger.update(SafariLocation.INSIDE, SafariBiome.CAVERN, 100)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        assertTrue(run.captureComplete("Rockmite")); assertTrue(run.captureComplete("Snoozle"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite", "Snoozle"), false)
        assertFalse(run.captureComplete("Rockmite")); assertFalse(run.captureComplete("Snoozle"))
        assertFalse(run.captureComplete("Rockmite", personalOnly = true, speciesInRange = emptySet()))
    }
    @Test fun `only party captures before the first visit relax biome requirements`() {
        val ledger = SafariLedger().apply { arrive(0) }; val run = ledger.current!!
        ledger.update(SafariLocation.INSIDE, SafariBiome.FOREST, 100)
        catch(run, "Honeybug", 3)
        ledger.update(SafariLocation.INSIDE, SafariBiome.CAVERN, 200)
        ledger.update(SafariLocation.INSIDE, SafariBiome.FOREST, 300)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertFalse(run.captureComplete("Honeybug"))
        assertFalse(run.partyNestsHandled)
        val own = SafariLedger().apply { arrive(0) }; val ownRun = own.current!!
        catch(ownRun, "Treefrog", 1, true); catch(ownRun, "Honeybug", 3)
        own.update(SafariLocation.INSIDE, SafariBiome.FOREST, 100)
        ownRun.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertTrue(ownRun.captureComplete("Honeybug"))
        ledger.enter(1000); ledger.update(SafariLocation.INSIDE, SafariBiome.FOREST, 1100)
        ledger.current!!.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertFalse(ledger.current!!.captureComplete("Honeybug"))
        assertFalse(ledger.current!!.partyNestsHandled)
    }
    @Test fun `green inherited Forest display cannot create a personal full clear PB`() {
        val ledger = SafariLedger().apply { arrive(0) }; val run = ledger.current!!
        mapOf("Foxtrot" to 6, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
            "Fluffling" to 1, "Hideonfloor" to 1).forEach { (name, amount) -> catch(run, name, amount) }
        ledger.update(SafariLocation.INSIDE, SafariBiome.FOREST, 100)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertTrue(SafariBiome.FOREST.critters.all { run.captureComplete(it.name) })
        assertNull(SafariFullClear.recordClear(run, SafariBiome.FOREST,
            BiomeClearEvidence(observed = true, nestsChecked = true), SafariPersonalBests(), 1000))
        assertTrue(run.biomeClears.isEmpty())
    }
}
