package net.johnceo.sparklingmutuals.safari.sparkling

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.SparklingChecks
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparklingDetectorTest {
    private fun model(id: Int = 1, name: String = "Duplico", texture: String? = SparklingSkins.textures[name]) =
        SparklingEntity(id, UUID.randomUUID(), -4.0, 71.0, -72.0, texture = texture, skinCarrier = true, body = true)
    private fun label(id: Int = 2, name: String = "Duplico") =
        SparklingEntity(id, UUID.randomUUID(), -4.0, 73.0, -72.0, name = "Sparkling $name")
    @Test fun `sheet covers 36 distinct skins with Areita spelling normalized and Tepid left unknown`() {
        assertEquals(36, SparklingSkins.textures.size)
        assertEquals(36, SparklingSkins.textures.values.toSet().size)
        assertEquals(SafariRoster.all.map { it.name }.toSet() - "Tepid", SparklingSkins.textures.keys)
        SparklingSkins.textures.forEach { (name, hash) -> assertEquals(name, SparklingSkins.species(hash)) }
        assertNull(SparklingSkins.species(null)); assertNull(SparklingSkins.species("not-a-known-skin"))
    }
    @Test fun `skin and nearby label share one detected critter and one capture obligation in either arrival order`() {
        for (skinFirst in listOf(true, false)) {
            val body = model(); val tag = label(); val run = SafariRun(0); val memory = SparklingMemory()
            val first = if (skinFirst) SparklingDetector.detect(listOf(body)) else SparklingDetector.detect(listOf(tag))
            val firstMatch = first.single()
            val firstId = run.observeSparkling(firstMatch.aliases.first(), firstMatch.species, firstMatch.aliases)
            assertTrue(memory.alert(firstId, true))
            val both = SparklingDetector.detect(listOf(body, tag)).single()
            assertEquals(body.id, both.body); assertEquals(tag.id, both.label)
            val canonical = run.observeSparkling(body.uuid, both.species, both.aliases)
            assertEquals(firstId, canonical); assertFalse(memory.alert(canonical, true))
            run.sparklingBroadcast("SPARKLING! Friend caught a SPARKLING Duplico!", 1000)
            assertFalse(run.sparklePending("Duplico"))
            run.observeSparkling(body.uuid, both.species, both.aliases)
            assertFalse(run.sparklePending("Duplico"))
        }
    }
    @Test fun `delayed skin replacement on same UUID is reread and reopens progress`() {
        val ordinary = model(texture = "ordinary-skin")
        val run = SafariRun(0)
        assertTrue(SparklingDetector.detect(listOf(ordinary)).isEmpty())
        val changed = ordinary.copy(texture = SparklingSkins.textures.getValue("Duplico"))
        val detected = SparklingDetector.detect(listOf(changed)).single()
        run.observeSparkling(changed.uuid, detected.species, detected.aliases)
        assertTrue(run.sparklePending("Duplico"))
        assertEquals("Duplico", detected.species)
    }
    @Test fun `real players wearing matching skins are ignored while NPC skin detection exceeds 80 blocks`() {
        val npc = model()
        assertTrue(SparklingDetector.detect(listOf(npc.copy(realPlayer = true, name = "Sparkling Duplico"))).isEmpty())
        val observedDistance = kotlin.math.sqrt(69.0*69 + 6.0*6 + 97.0*97)
        assertTrue(observedDistance > 119 && observedDistance < 120)
        assertEquals("Duplico", SparklingDetector.detect(listOf(npc)).single().species)
        assertTrue(SparklingDetector.detect(emptyList()).isEmpty())
    }
    @Test fun `Tepid retains name fallback and model with its own name never pairs to a neighbor`() {
        val named = model(name = "Tepid", texture = null).copy(name = "Sparkling Tepid")
        val neighbor = model(id = 3).copy(x = -3.0)
        val found = SparklingDetector.detect(listOf(named, neighbor))
        assertEquals(named.id, found.single { it.species == "Tepid" }.body)
        assertEquals(2, found.size)
    }
    @Test fun `a skin carrier with its own Sparkling name remains one critter despite a nearby body`() {
        val named = model().copy(name = "Sparkling Duplico")
        val neighbor = model(id = 3, texture = "ordinary-skin").copy(x = -3.0)
        val found = SparklingDetector.detect(listOf(named, neighbor)).single()
        assertEquals(named.id, found.body)
        assertEquals(named.id, found.label)
    }
    @Test fun `an authoritative Sparkling name wins when skin metadata disagrees`() {
        val renamed = model().copy(name = "Sparkling Tepid")
        assertEquals("Tepid", SparklingDetector.detect(listOf(renamed)).single().species)
    }
    @Test fun `positive detector evidence blocks each mode without replacing ordinary scan circles`() {
        val run = SafariRun(0); val body = model(); val found = SparklingDetector.detect(listOf(body)).single()
        val party = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
        }
        run.record(SafariCatch(SafariRoster.named("Duplico")!!))
        run.observeSparkling(body.uuid, found.species, found.aliases)
        SafariMode.entries.forEach { assertEquals(0, run.completed(it, listOf(SafariRoster.named("Duplico")!!), party)) }
        val checks = SparklingChecks()
        checks.scan(SafariBiome.HAUNTED, 500.0, 500.0, List(2) { "Duplico" to UUID.randomUUID() })
        assertFalse(checks.locallyChecked("Duplico"))
    }
}
