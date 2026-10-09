package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class SafariMovingEspTest {
    @Test fun `legacy display applies to all requested moving species while sparkling keeps lifecycle checks`() {
        for (species in EspMotion.movingSpecies + "Shyworm") {
            assertTrue(SafariEspRules.renderCurrent(species, false, true, false, false), species)
            assertFalse(SafariEspRules.renderCurrent(species, true, true, false, false), species)
            assertTrue(SafariEspRules.renderCurrent(species, true, true, true, true), species)
            assertFalse(SafariEspRules.renderCurrent(species, false, false, true, true), species)
        }
    }
    @Test fun `Driftling keeps ESP when its known name tag stops rendering at distance`() {
        val model = UUID.randomUUID()
        val tag = UUID.randomUUID()
        val labels = EspModelLabels()
        labels.observe(model, "Driftling", listOf(EspModelLabel(tag, "Driftling", 1.0)), 0)
        labels.observe(model, "Driftling", listOf(EspModelLabel(tag, "Driftling", 1.0, false)), 1500)
        assertFalse(labels.current(model, 1500))
        assertTrue(SafariEspRules.renderCurrent("Driftling", false, true, true, labels.current(model, 1500)))
        assertFalse(SafariEspRules.renderCurrent("Driftling", true, true, true, labels.current(model, 1500)))
    }
    @Test fun `a moving critter suppressed nearby can still render after the player moves away`() {
        val id = UUID.randomUUID()
        val motion = EspMotion()
        motion.observe(id, "Driftling", -100.0, 50.0, 25.0, 100.0, 0)
        motion.observe(id, "Driftling", -100.0, 50.0, 25.0, 100.0, 2500)
        motion.observe(id, "Driftling", -100.0, 50.0, 25.0, 2500.0, 3000)
        assertFalse(motion.current(id, 3000))
        assertTrue(SafariEspRules.renderCurrent("Driftling", false, true, motion.current(id, 3000), true))
        assertFalse(SafariEspRules.renderCurrent("Driftling", true, true, motion.current(id, 3000), true))
    }
    @Test fun `legacy moving ESP still excludes captured removed transformed and unloaded models`() {
        val driftling = SafariEspRules.mobs.first { it.name == "Driftling" }.identifiers.first()
        for (valid in listOf(
            SafariEspRules.targetCurrent("Driftling", driftling, true, true, true),
            SafariEspRules.targetCurrent("Driftling", driftling, false, false, true),
            SafariEspRules.targetCurrent("Driftling", driftling, false, true, false),
            SafariEspRules.targetCurrent("Driftling", driftling.copy(texture = null), false, true, true)
        )) assertFalse(SafariEspRules.renderCurrent("Driftling", false, valid, true, true))
        assertFalse(SafariEspRules.renderCurrent("Gazer", false, true, true, false))
        assertTrue(SafariEspRules.renderCurrent("Shyworm", false, true, true, false))
    }
}
