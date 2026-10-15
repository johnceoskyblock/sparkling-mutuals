package net.johnceo.sparklingmutuals.safari.sparkling

import java.nio.file.Path
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SparklingCaptureRequirementTest {
    @TempDir lateinit var dir: Path
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
    }
    @Test fun `every mode retains seen Sparkling obligations across reentry regardless of party discoveries`() {
        ConfigManager.init(dir)
        val state = party()
        for (mode in SafariMode.entries) for (biome in SafariBiome.entries) {
            SafariFullClear.select(mode)
            val species = biome.critters.first()
            val run = SafariRun(0)
            biome.critters.forEach { run.record(SafariCatch(it)) }
            run.partyFullClear("Party > Friend: " + when(biome) {
                SafariBiome.FOREST -> "fd"; SafariBiome.CAVERN -> "cd"; SafariBiome.ICY -> "id"; SafariBiome.HAUNTED -> "hd"
            })
            val id = UUID.randomUUID()
            repeat(3) { run.observeSparkling(id, species.name) }
            run.visitBiome(biome); run.sparklingChecks.reenter(biome)
            assertTrue(run.sparklePending(species.name))
            assertFalse(run.uniqueComplete(species.name, state))
            assertFalse(run.sparklingComplete(species.name, state))
            assertFalse(run.captureComplete(species.name, speciesInRange = emptySet()))
            assertEquals(0, run.completed(mode, listOf(species), state))
            assertTrue(SafariPanels.missingCritters(run, biome, state).contains(species))
            assertNull(run.completion.announcement(run, biome, mode, state))
            assertFalse(run.completion.alert(run, mode, state, true))
            run.record(SafariCatch(species)) // A normal catch of the same species cannot discharge it.
            assertTrue(run.sparklePending(species.name))
            run.record(SafariCatch.parse("LOOT SHARE! You received a Rainbow Feather from Friend catching a SPARKLING ${species.name}!")!!, 1000)
            assertFalse(run.sparklePending(species.name))
            run.observeSparkling(id, species.name) // Lingering captured label is already known.
            assertFalse(run.sparklePending(species.name))
            assertFalse(SafariRun(0).sparklingCaptures.anyPending())
        }
    }
    @Test fun `two Sparkling UUIDs require two catches and paired chat messages count once in either order`() {
        for (broadcastFirst in listOf(true, false)) {
            val run = SafariRun(0)
            repeat(2) { run.observeSparkling(UUID.randomUUID(), "Gazer") }
            val catch = SafariCatch.parse("CAPTURE! You caught a SPARKLING Gazer!")!!
            val broadcast = "SPARKLING! Me caught a SPARKLING Gazer!"
            if (broadcastFirst) { run.sparklingBroadcast(broadcast, 1000); run.record(catch, 1100) }
            else { run.record(catch, 1000); run.sparklingBroadcast(broadcast, 1100) }
            assertTrue(run.sparklePending("Gazer"))
            run.sparklingBroadcast(broadcast, 10000)
            run.record(catch, 10100)
            assertFalse(run.sparklePending("Gazer"))
        }
    }
    @Test fun `different players catching at once do not merge separate Sparkling captures`() {
        val run = SafariRun(0)
        repeat(2) { run.observeSparkling(UUID.randomUUID(), "Gazer") }
        run.sparklingBroadcast("SPARKLING! Friend caught a SPARKLING Gazer!", 1000)
        run.record(SafariCatch.parse("CAPTURE! You caught a SPARKLING Gazer!")!!, 1100, "Me")
        assertFalse(run.sparklePending("Gazer"))
    }
    @Test fun `new Sparkling reopens a previously announced biome and run alert`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.HAUNTED)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), true)
        assertEquals("hd", run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
        run.completion.sent(SafariBiome.HAUNTED)
        run.observeSparkling(UUID.randomUUID(), "Gazer")
        assertNull(run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
        run.sparklingBroadcast("SPARKLING! Friend found a SPARKLING Gazer!", 1000)
        assertEquals("hd", run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
    }
}
