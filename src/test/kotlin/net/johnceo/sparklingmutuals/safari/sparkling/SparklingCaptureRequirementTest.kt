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
    private fun needsAll() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to emptySet()))
    }
    @Test fun `Sparkling capture completes every species in Unique and Sparkling panels despite missing ordinary checks`() {
        ConfigManager.init(dir)
        val state = needsAll()
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING)) for (species in SafariRoster.all) {
            SafariFullClear.select(mode)
            val run = SafariRun(0)
            run.visitBiome(species.biome)
            run.observeSparkling(UUID.randomUUID(), species.name)
            assertEquals(0, run.completed(mode, listOf(species), state))
            run.record(SafariCatch.parse("LOOT SHARE! You received a Rainbow Feather from Friend catching a SPARKLING ${species.name}!")!!, 1000)
            assertEquals(1, run.completed(mode, listOf(species), state), species.name)
            assertFalse(SafariPanels.missing(run, species.biome, mode == SafariMode.UNIQUE, 2, party = state).rows.any { it.label == species.name }, species.name)
            val targets = species.biome.critters.size
            assertEquals("1/$targets", SafariPanels.progress(run, true, 0, state).rows.single { it.label == species.biome.label }.value, species.name)
            run.visitBiome(SafariBiome.entries.first { it != species.biome }); run.visitBiome(species.biome)
            assertEquals(1, run.completed(mode, listOf(species), state), "completion persists on reentry")
            run.observeSparkling(UUID.randomUUID(), species.name)
            assertEquals(0, run.completed(mode, listOf(species), state), "a second detected Sparkling reopens completion")
        }
    }
    @Test fun `Sparkling capture never waives Full Clear populations or spawn sources`() {
        val state = needsAll()
        for (name in listOf("Gazer", "Honeybug", "Rockmite", "Macaw")) {
            val species = SafariRoster.named(name)!!
            val run = SafariRun(0)
            run.visitBiome(species.biome)
            run.updateCaptureEvidence(species.biome, setOf(name), false)
            run.observeSparkling(UUID.randomUUID(), name)
            run.record(SafariCatch.parse("CAPTURE! You caught a SPARKLING $name!")!!, 1000)
            assertFalse(run.sparklePending(name))
            assertEquals(0, run.completed(SafariMode.FULL_CLEAR, listOf(species), state), name)
            assertFalse(run.captureComplete(name), name)
        }
    }
    @Test fun `Full Clear panels complete after Sparkling and remaining captures in either message order`() {
        ConfigManager.init(dir)
        SafariFullClear.select(SafariMode.FULL_CLEAR)
        for (broadcastFirst in listOf(false, true)) for (personal in listOf(false, true)) {
            val run = SafariRun(0); val state = needsAll()
            val species = SafariRoster.named("Gazer")!!
            val id = UUID.randomUUID()
            run.visitBiome(SafariBiome.HAUNTED)
            run.updateCaptureEvidence(SafariBiome.HAUNTED, setOf("Gazer"), false)
            run.observeSparkling(id, "Gazer")
            val caught = if (personal) "CAPTURE! You caught a SPARKLING Gazer!"
                else "LOOT SHARE! You received a Rainbow Feather from Friend catching a SPARKLING Gazer!"
            val player = if (personal) "Me" else "Friend"
            val broadcast = "SPARKLING! $player caught a SPARKLING Gazer!"
            if (broadcastFirst) { run.sparklingBroadcast(broadcast, 1000); run.record(SafariCatch.parse(caught)!!, 1100, player) }
            else { run.record(SafariCatch.parse(caught)!!, 1000, player); run.sparklingBroadcast(broadcast, 1100) }
            run.observeSparkling(id, "Gazer") // A lingering captured label must stay resolved.
            assertFalse(run.sparklePending("Gazer"))
            assertFalse(run.captureComplete("Gazer"))
            repeat(3) { run.record(SafariCatch(species), 2000 + it.toLong()) }
            run.observePopulation(listOf("Gazer" to id)) // A lingering captured body must not invent a fifth Gazer.
            run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), false, playerX = -4.0, playerZ = -64.0)
            assertTrue(run.captureComplete("Gazer"))
            assertEquals(1, run.completed(SafariMode.FULL_CLEAR, listOf(species), state))
            assertFalse(SafariPanels.missing(run, SafariBiome.HAUNTED, false, 0, party = state).rows.any { it.label == "Gazer" })
            assertEquals(0xFF55FF55.toInt(), SafariPanels.captures(run, SafariBiome.HAUNTED).rows.single { it.label == "Gazer" }.valueColor)
            assertEquals("1/10", SafariPanels.progress(run, true, 0, state).rows.single { it.label == "Haunted" }.value)
            assertFalse(run.sparklingCaptures.anyPending())
        }
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
            assertFalse(run.uniqueComplete("Gazer", needsAll()))
            assertFalse(run.sparklingComplete("Gazer", needsAll()))
            run.sparklingBroadcast(broadcast, 10000)
            run.record(catch, 10100)
            assertFalse(run.sparklePending("Gazer"))
            assertTrue(run.uniqueComplete("Gazer", needsAll()))
            assertTrue(run.sparklingComplete("Gazer", needsAll()))
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
