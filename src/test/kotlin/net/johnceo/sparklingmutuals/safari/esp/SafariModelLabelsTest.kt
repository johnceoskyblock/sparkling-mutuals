package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
    @Test fun `late frozen Driftling cannot inflate targets linger in ESP or block local completion`() {
        val motion = EspMotion()
        val labels = EspModelLabels()
        val run = SafariRun(0)
        repeat(5) { run.record(SafariCatch(SafariRoster.named("Driftling")!!, false)) }
        run.visitBiome(SafariBiome.CAVERN)
        run.partyFullClear("Party > Friend: cd")
        fun active(now: Long) = SafariEspRules.driftlingCurrent(labels.label(model) != null,
            labels.current(model, now), labels.visible(model), motion.current(model, now), motion.movementObserved(model))
        fun scan(now: Long) {
            val live = if (active(now)) listOf("Driftling" to model) else emptyList()
            run.observePopulation(live)
            run.updateCaptureEvidence(SafariBiome.CAVERN, live.map { it.first }.toSet(), false,
                playerX = -120.0, playerZ = 55.0)
        }
        motion.observe(model, "Driftling", -120.0, 50.0, 55.0, 25.0, 0)
        // No original name tag was loaded because this player arrived after the catch.
        labels.observe(model, "Driftling", emptyList(), 0)
        scan(0)
        assertFalse(active(0), "first sight of an unlabelled frozen head is not a live spawn")
        // A retained hidden custom name is not a visible live label either.
        labels.observe(model, "Driftling", listOf(tag(visible = false)), 2500)
        motion.observe(model, "Driftling", -120.0, 50.0, 55.0, 25.0, 2500)
        scan(2500)
        assertFalse(active(2500))
        for (sparkling in listOf(false, true))
            assertFalse(SafariEspRules.renderCurrent("Driftling", sparkling, active(2500), false, true))
        assertTrue(run.captureComplete("Driftling"))
        assertTrue(run.fullClearMissingComplete("Driftling"))
        assertEquals(5, run.count("Driftling"))
        // A genuinely moving model reopens remaining work without adding any capture.
        motion.observe(model, "Driftling", -119.0, 50.0, 55.0, 25.0, 2750)
        scan(2750)
        assertTrue(active(2750))
        assertFalse(run.fullClearMissingComplete("Driftling"))
        assertEquals(5, run.count("Driftling"))
    }
    @Test fun `a retained visible Driftling label cannot keep a frozen head alive`() {
        assertFalse(SafariEspRules.driftlingCurrent(true, true, true, false, false))
        assertFalse(SafariEspRules.driftlingCurrent(true, false, true, true, true))
    }
    @Test fun `Driftling movement expires beyond thirty blocks even with a retained visible name`() {
        val motion = EspMotion()
        motion.observe(model, "Driftling", 0.0, 0.0, 0.0, 2500.0, 0)
        motion.observe(model, "Driftling", 1.0, 0.0, 0.0, 2500.0, 250)
        assertTrue(SafariEspRules.driftlingCurrent(true, true, true, motion.recentMovement(model, 500), true))
        motion.observe(model, "Driftling", 1.0, 0.0, 0.0, 2500.0, 2500)
        assertTrue(motion.current(model, 2500), "legacy distance grace is unrelated to Driftling's fresh movement gate")
        assertFalse(SafariEspRules.driftlingCurrent(true, true, true, motion.recentMovement(model, 2500), true))
        motion.observe(model, "Driftling", 2.0, 0.0, 0.0, 2500.0, 2750)
        assertTrue(motion.recentMovement(model, 2750))
    }
    @Test fun `Chuckwalla needs a current visible label and never an orphaned or hidden name`() {
        assertFalse(SafariEspRules.chuckwallaCurrent(false, true, false))
        assertFalse(SafariEspRules.chuckwallaCurrent(true, true, false))
        assertFalse(SafariEspRules.chuckwallaCurrent(true, false, true))
        assertTrue(SafariEspRules.chuckwallaCurrent(true, true, true))
    }
    @Test fun `capture transition does not permanently increase Driftling or Chuckwalla target`() {
        for (species in listOf("Driftling", "Chuckwalla")) {
            var now = 0L
            val run = SafariRun(0) { now }
            run.visitBiome(SafariBiome.CAVERN)
            repeat(6) { run.record(SafariCatch(SafariRoster.named(species)!!, false), now) }
            run.partyFullClear("Party > Friend: cd")
            now = 500
            run.observePopulation(listOf(species to model))
            run.updateCaptureEvidence(SafariBiome.CAVERN, setOf(species), false, playerX = -120.0, playerZ = 45.0)
            assertFalse(run.fullClearMissingComplete(species), "positive remaining evidence still blocks during transition")
            assertFalse(run.fullClearComplete(species))
            now = 3000
            run.observePopulation(emptyList())
            run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false, playerX = -120.0, playerZ = 45.0)
            assertTrue(run.fullClearMissingComplete(species), "capture plus old head must not become a permanent seventh capture")
            run.observePopulation(listOf(species to java.util.UUID.randomUUID()))
            run.updateCaptureEvidence(SafariBiome.CAVERN, setOf(species), false)
            assertFalse(run.fullClearComplete(species), "a genuinely remaining critter after transition still blocks")
        }
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
