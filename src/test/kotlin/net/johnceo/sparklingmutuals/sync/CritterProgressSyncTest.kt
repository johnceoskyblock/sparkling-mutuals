package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SharedCritterProgress
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CritterProgressSyncTest {
    private val done = SharedCritterProgress.Evidence(true, true, true, 3, 3)
    private fun party(vararg needed: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needed.toSet())))
    }
    @Test fun `shared current run proof drives all three modes without creating catches or local reports`() {
        val run = SafariRun(0); val party = party("Scrappy")
        run.sharedCritters.replace("peer", 1, mapOf("Scrappy" to done))
        assertTrue(run.uniqueComplete("Scrappy", party)); assertTrue(run.sparklingComplete("Scrappy", party))
        assertTrue(run.fullClearComplete("Scrappy")); assertFalse(run.missing(SafariRoster.named("Scrappy")!!, true))
        assertFalse(run.uniqueComplete("Scrappy", party, true)); assertFalse(run.sparklingComplete("Scrappy", party, true))
        assertFalse(run.captureComplete("Scrappy")); assertEquals(0, run.count("Scrappy")); assertEquals(0, run.personalCount("Scrappy"))
        assertEquals(3, run.checkedProgress("Scrappy")); assertEquals(3, run.captureProgress("Scrappy"))
        val exported = CritterProgressProtocol.decode(CritterProgressProtocol.encode(run, party))!!.getValue("Scrappy")
        assertEquals(SharedCritterProgress.Evidence(false, false, false, 0, 0), exported, "never echo received proof")
        assertFalse(SafariRun(1).sparklingComplete("Scrappy", party), "new run is isolated")
    }
    @Test fun `maxima deduplicate shared checks and latest false snapshot revokes old proof`() {
        var now = 100L; val shared = SharedCritterProgress { now }
        shared.replace("a", 1, mapOf("Scrappy" to done.copy(checks = 2)))
        shared.replace("b", 1, mapOf("Scrappy" to done.copy(checks = 2)))
        assertEquals(2, shared.checks("Scrappy"))
        assertFalse(shared.replace("a", 1, emptyMap()))
        shared.replace("a", 2, emptyMap()); shared.replace("b", 2, emptyMap())
        assertFalse(shared.complete("Scrappy", SafariMode.SPARKLING))
        shared.replace("b", 3, mapOf("Scrappy" to done), 40_000)
        now += 5000; assertFalse(shared.complete("Scrappy", SafariMode.SPARKLING))
        assertFalse(shared.replace("b", 2, mapOf("Scrappy" to done)), "expired old sequence cannot revive")
    }
    @Test fun `positive local sources and live remainders override peer completion`() {
        val run = SafariRun(0); val party = party("Rockmite", "Snoozle")
        run.sharedCritters.replace("peer", 1, mapOf("Rockmite" to done, "Snoozle" to done, "Scrappy" to done))
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        assertFalse(run.sparklingComplete("Rockmite", party)); assertFalse(run.uniqueComplete("Snoozle", party))
        assertFalse(run.fullClearComplete("Rockmite"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Scrappy"), true)
        assertFalse(run.fullClearComplete("Scrappy")); assertTrue(run.sparklingComplete("Scrappy", party))
    }
    @Test fun `wire rejects malformed versions booleans row lengths and out of range counts`() {
        val good = CritterProgressProtocol.encode(SafariRun(0), party())
        assertEquals(37, CritterProgressProtocol.decode(good)!!.size)
        for (bad in listOf(good.replace("\"version\":1", "\"version\":2"), "{}", "[]", good.replaceFirst("[0,0,0]", "[true,0,0]"),
            good.replaceFirst("[0,0,0]", "[8,0,0]"), good.replaceFirst("[0,0,0]", "[0,257,0]"))) assertNull(CritterProgressProtocol.decode(bad))
        val extra = JsonParser.parseString(good).asJsonObject.apply { addProperty("reporter", "other") }
        assertNull(CritterProgressProtocol.decode(extra.toString()))
    }
    @Test fun `shared completed biome changes presentation but cannot create history split or local announcement`() {
        val run = SafariRun(0); val party = party()
        run.sharedCritters.replace("peer", 1, SafariBiome.CAVERN.critters.associate { it.name to done })
        assertTrue(run.biomeComplete(SafariMode.FULL_CLEAR, SafariBiome.CAVERN, party))
        assertFalse(run.biomeComplete(SafariMode.FULL_CLEAR, SafariBiome.CAVERN, party, includeShared = false))
        assertNull(run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.FULL_CLEAR, party))
        assertTrue(run.biomeComplete(SafariMode.FULL_CLEAR, SafariBiome.CAVERN, party), "history read restores display state")
    }
    @Test fun `missing list and fixed counter consume shared mode evidence`() {
        val oldSpark = net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode
        val oldFull = net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode
        try {
            val run = SafariRun(0); val party = party("Scrappy")
            run.sharedCritters.replace("peer", 1, mapOf("Scrappy" to done))
            net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = true
            net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = false
            assertTrue(net.johnceo.sparklingmutuals.safari.ui.SafariPanels.missingCritters(run, SafariBiome.CAVERN, party).isEmpty())
            assertEquals("3/3 checked", net.johnceo.sparklingmutuals.safari.ui.SafariPanels.fixedCounter("Scrappy", run, SafariMode.SPARKLING, party))
            net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = false
            net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = true
            assertFalse(net.johnceo.sparklingmutuals.safari.ui.SafariPanels.missingCritters(run, SafariBiome.CAVERN, party).any { it.name == "Scrappy" })
        } finally {
            net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = oldSpark
            net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = oldFull
        }
    }
    @Test fun `boss render alone is never exported as completed boss or capture`() {
        val run = SafariRun(0)
        run.sparklingChecks.scan(SafariBiome.ICY, -112.0, -54.0, listOf("Wumpa" to java.util.UUID.randomUUID()))
        val proof = CritterProgressProtocol.decode(CritterProgressProtocol.encode(run, party("Wumpa")))!!.getValue("Wumpa")
        assertFalse(proof.checked); assertFalse(proof.unique); assertFalse(proof.fullClear); assertEquals(0, proof.captures)
    }
    @Test fun `a peer first catch does not waive receivers needed source checks`() {
        val run = SafariRun(0)
        run.sharedCritters.replace("peer", 1, listOf("Rockmite", "Snoozle", "Honeybug").associateWith { done.copy(checked = false) })
        assertTrue(run.uniqueComplete("Snoozle", party()))
        assertFalse(run.uniqueComplete("Snoozle", party("Snoozle")))
        assertFalse(run.uniqueComplete("Rockmite", party("Rockmite")))
        assertEquals(0, run.completed(SafariMode.UNIQUE, listOf(SafariRoster.named("Honeybug")!!), party("Honeybug")))
        assertTrue(net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules.showNestCounter(SafariMode.UNIQUE, run, party("Honeybug")))
    }
}
