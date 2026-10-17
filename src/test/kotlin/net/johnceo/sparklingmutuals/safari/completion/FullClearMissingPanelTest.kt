package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FullClearMissingPanelTest {
    private fun caught(run: SafariRun, name: String, amount: Int) = repeat(amount) {
        run.record(SafariCatch(SafariRoster.named(name)!!, false))
    }

    @Test fun `every ordinary zoned species needs minimum catches and an empty scan inside its radius`() {
        for (biome in SafariBiome.entries) {
            for ((name, zone) in SafariCompletionZones.forBiome(biome).filterKeys { it != "Wumpa" }) {
                val run = SafariRun(0)
                run.visitBiome(biome)
                caught(run, name, SafariFullClear.minimum(name) - 1)
                run.updateCaptureEvidence(biome, emptySet(), false, playerX = zone.x, playerZ = zone.z)
                assertFalse(run.fullClearMissingComplete(name), "$name still needs its minimum")
                caught(run, name, 1)
                run.updateCaptureEvidence(biome, emptySet(), false,
                    playerX = zone.x + zone.fullClearRadius + 1, playerZ = zone.z)
                assertFalse(run.fullClearMissingComplete(name), "$name cannot first clear outside its radius")
                run.updateCaptureEvidence(biome, setOf(name), false, playerX = zone.x, playerZ = zone.z)
                assertFalse(run.fullClearMissingComplete(name), "$name still rendered")
                run.updateCaptureEvidence(biome, emptySet(), false, playerX = zone.x, playerZ = zone.z)
                assertTrue(run.fullClearMissingComplete(name), "$name clears without a party report")
                assertTrue(run.fullClearComplete(name), "$name local progress needs no party report")
                run.updateCaptureEvidence(biome, emptySet(), false,
                    playerX = zone.x + zone.fullClearRadius + 50, playerZ = zone.z)
                assertTrue(run.fullClearMissingComplete(name), "$name retains its qualified check")
                assertTrue(run.fullClearComplete(name), "$name retains local progress after leaving its radius")
                run.visitBiome(SafariBiome.entries.first { it != biome })
                assertTrue(run.fullClearComplete(name), "$name retains earned progress after leaving its biome")
            }
        }
    }

    @Test fun `Billygoat missing proof survives travel but a new sighting requires a new qualified clear`() {
        ConfigManager.fullClearMode = true; ConfigManager.sparklingMode = false
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.ICY)
        caught(run, "Billygoat", 2)
        run.partyFullClear("Party > Friend: id")
        assertTrue(run.fullClearComplete("Billygoat"))
        assertTrue(SafariPanels.missingCritters(run, SafariBiome.ICY).any { it.name == "Billygoat" })
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false, playerX = -121.0, playerZ = -55.0)
        assertFalse(SafariPanels.missingCritters(run, SafariBiome.ICY).any { it.name == "Billygoat" })
        run.visitBiome(SafariBiome.FOREST)
        run.visitBiome(SafariBiome.ICY)
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false, playerX = -50.0, playerZ = -55.0)
        assertTrue(run.fullClearMissingComplete("Billygoat"))
        run.observePopulation(listOf("Billygoat" to UUID.randomUUID()))
        run.updateCaptureEvidence(SafariBiome.ICY, setOf("Billygoat"), false, playerX = -50.0, playerZ = -55.0)
        assertFalse(run.fullClearMissingComplete("Billygoat"))
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false, playerX = -121.0, playerZ = -55.0)
        assertFalse(run.fullClearMissingComplete("Billygoat"), "unloading is not a catch")
        caught(run, "Billygoat", 1)
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false, playerX = -121.0, playerZ = -55.0)
        assertTrue(run.fullClearMissingComplete("Billygoat"))
        run.observeSparkling(UUID.randomUUID(), "Billygoat")
        assertFalse(run.fullClearMissingComplete("Billygoat"), "pending Sparkling still overrides local proof")
    }
}
