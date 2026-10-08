package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class SafariModelLabelsTest {
    private val model = UUID(0, 1)
    private val label = UUID(0, 2)
    private fun tag(species: String = "Driftling", id: UUID = label, distance: Double = 1.0, visible: Boolean = true) =
        EspModelLabel(id, species, distance, visible)

    @Test fun `registered head model stops counting when its known critter label disappears`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.observe(model, "Driftling", emptyList(), 500)
        assertTrue(labels.current(model, 500))
        labels.observe(model, "Driftling", emptyList(), 1250)
        assertFalse(labels.current(model, 1250))
        labels.observe(model, "Driftling", emptyList(), 2000)
        assertFalse(labels.current(model, 2000))
    }
    @Test fun `a changed capturing label no longer keeps the old critter alive`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.observe(model, "Driftling", listOf(tag("CAPTURING Driftling")), 1250)
        assertFalse(labels.current(model, 1250))
    }
    @Test fun `another same species label cannot revive a frozen captured model`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.observe(model, "Driftling", listOf(tag(id = UUID(0, 3))), 1250)
        assertFalse(labels.current(model, 1250))
    }
    @Test fun `an active critter survives delayed label packets and reconnects to its original label`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.observe(model, "Driftling", emptyList(), 750)
        assertTrue(labels.current(model, 750))
        labels.observe(model, "Driftling", listOf(tag()), 1500)
        assertTrue(labels.current(model, 1500))
    }
    @Test fun `unpaired models remain conservative and never imply a capture`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag("Flitter"), tag(distance = 25.0)), 0)
        assertTrue(labels.current(model, 5000))
    }
    @Test fun `hiding a formerly visible name tag invalidates its model`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.observe(model, "Driftling", listOf(tag(visible = false)), 1250)
        assertFalse(labels.current(model, 1250))
    }
    @Test fun `labels whose visibility flag was always false are still supported`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag(visible = false)), 0)
        labels.observe(model, "Driftling", listOf(tag(visible = false)), 5000)
        assertTrue(labels.current(model, 5000))
    }
    @Test fun `failed capture allows a respawned model to bind to its new name tag`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.release(model)
        labels.observe(model, "Driftling", listOf(tag(id = UUID(0, 3))), 1250)
        assertTrue(labels.current(model, 1250))
        labels.observe(model, "Driftling", emptyList(), 2500)
        assertFalse(labels.current(model, 2500))
    }
    @Test fun `unlabelled mounds and normal mobs do not require a model label`() {
        assertFalse(SafariEspRules.requiresModelLabel("Rockmite", "item_display"))
        assertFalse(SafariEspRules.requiresModelLabel("Foxtrot", "fox"))
        assertTrue(SafariEspRules.requiresModelLabel("Driftling", "armor_stand"))
        assertTrue(SafariEspRules.requiresModelLabel("Flitter", "item_display"))
    }
    @Test fun `only exact roster names and sparkling prefixes can serve as active labels`() {
        assertEquals("Driftling", SafariEspRules.labelSpecies("§aDriftling"))
        assertEquals("Driftling", SafariEspRules.labelSpecies("§6SPARKLING Driftling"))
        assertNull(SafariEspRules.labelSpecies("CAPTURING Driftling"))
        assertNull(SafariEspRules.labelSpecies("Driftling Shard"))
    }
    @Test fun `an unrelated failed attempt does not release an old ghost binding`() {
        val labels = EspModelLabels()
        val attempts = EspCaptureMemory()
        val escaped = UUID(0, 4)
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.observe(escaped, "Driftling", listOf(tag(id = UUID(0, 5))), 0)
        labels.observe(model, "Driftling", emptyList(), 1250)
        attempts.aimed(escaped, "Driftling", 1000)
        attempts.escaped("Driftling", 1500)?.let(labels::release)
        assertFalse(labels.current(model, 1500))
        assertTrue(labels.current(escaped, 1500))
    }
    @Test fun `missing label releases red Driftling count without hiding a genuinely remaining copy`() {
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(tag()), 0)
        labels.observe(model, "Driftling", emptyList(), 1250)
        val run = SafariRun(0)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!)) }
        run.updateCaptureEvidence(SafariBiome.CAVERN, if (labels.current(model, 1250)) setOf("Driftling") else emptySet(), false)
        assertTrue(run.captureComplete("Driftling"))
        val other = UUID(0, 4)
        labels.observe(other, "Driftling", listOf(tag(id = UUID(0, 5))), 1250)
        run.updateCaptureEvidence(SafariBiome.CAVERN, if (labels.current(other, 1250)) setOf("Driftling") else emptySet(), false)
        assertFalse(run.captureComplete("Driftling"))
        labels.reset()
        assertTrue(labels.current(model, 1250))
    }
}
