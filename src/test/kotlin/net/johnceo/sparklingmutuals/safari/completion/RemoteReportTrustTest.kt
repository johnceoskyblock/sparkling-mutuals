package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RemoteReportTrustTest {
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to emptySet()))
    }
    private fun caught(run: SafariRun, name: String, amount: Int = 1) = repeat(amount) {
        run.record(SafariCatch(SafariRoster.named(name)!!, false))
    }

    @Test fun `remote reports preserve confirmed Sparkling catches without inventing shared captures`() {
        for ((name, word) in listOf("Wumpa" to "id", "Doomspiral" to "hd", "Gazer" to "hd", "Gimmiegold" to "hd")) {
            val run = SafariRun(0).apply { visitBiome(SafariBiome.FOREST) }
            run.sparklingBroadcast("SPARKLING! Friend caught a SPARKLING $name!", 1000)
            assertTrue(run.sparklingComplete(name, party()), name)
            assertTrue(run.uniqueComplete(name, party()), name)
            run.partyFullClear("Party > Clearer: $word")
            assertTrue(run.sparklingComplete(name, party()), "$name after report")
            assertTrue(run.uniqueComplete(name, party()), "$name after report")
            assertEquals(0, run.count(name), "global announcements do not fabricate shared captures")
        }
    }

    @Test fun `leaving a visited biome permits remote Unique trust while reentry restores local checking`() {
        val run = SafariRun(0); val party = party()
        run.visitBiome(SafariBiome.HAUNTED)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, setOf("Gazer", "Gimmiegold"), false)
        SafariBiome.HAUNTED.critters.forEach { caught(run, it.name) }
        run.visitBiome(SafariBiome.ICY)
        assertFalse(run.uniqueComplete("Gazer", party))
        run.partyFullClear("Party > Clearer: hd")
        assertEquals(10, run.completed(SafariMode.UNIQUE, SafariBiome.HAUNTED.critters, party))
        assertEquals(0, run.personalCount("Gazer"), "a report never invents personal capture credit")
        run.visitBiome(SafariBiome.HAUNTED)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, setOf("Gazer", "Gimmiegold"), false)
        assertFalse(run.uniqueComplete("Gazer", party))
        assertFalse(run.uniqueComplete("Gimmiegold", party))
    }

    @Test fun `remote Unique report can waive absent birds but cannot waive an ordinary first capture`() {
        val run = SafariRun(0); val party = party()
        run.visitBiome(SafariBiome.FOREST)
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Foxtrot", "Honeybug"), false)
        SafariBiome.FOREST.critters.filter { it.name !in setOf("Bluebird", "Parakeet", "Macaw", "Foxtrot") }
            .forEach { caught(run, it.name) }
        run.visitBiome(SafariBiome.CAVERN)
        run.partyFullClear("Party > Clearer: fd")
        assertEquals(8, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, party))
        caught(run, "Foxtrot")
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, party))
        assertEquals(0, run.count("Macaw"))
    }

    @Test fun `remote Full Clear report after leaving still requires each capture minimum`() {
        for ((name, word) in listOf("Driftling" to "cd", "Chuckwalla" to "cd", "Honeybug" to "fd", "Gimmiegold" to "hd", "Gazer" to "hd")) {
            val run = SafariRun(0)
            val biome = SafariRoster.named(name)!!.biome
            val minimum = SafariFullClear.minimum(name)
            run.visitBiome(biome)
            run.updateCaptureEvidence(biome, setOf(name), false)
            if (name == "Honeybug") repeat(8) { run.observeCritter(it, name, java.util.UUID(0, it.toLong())) }
            caught(run, name, minimum - 1)
            run.visitBiome(SafariBiome.entries.first { it != biome })
            run.partyFullClear("Party > Clearer: $word")
            assertFalse(run.fullClearComplete(name), name)
            caught(run, name)
            assertTrue(run.fullClearComplete(name), name)
        }
    }
}
