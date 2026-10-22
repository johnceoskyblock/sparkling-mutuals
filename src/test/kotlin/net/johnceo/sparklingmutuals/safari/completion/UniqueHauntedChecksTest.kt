package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class UniqueHauntedChecksTest {
    @TempDir lateinit var dir: Path
    @BeforeEach fun uniqueMode() { ConfigManager.init(dir) }
    private fun party(vararg needed: String) = PartySparklingState().apply {
        val all = SafariRoster.all.map { it.name }.toSet()
        select(setOf("self", "friend"))
        accept(setOf("self", "friend"), mapOf("self" to all, "friend" to all - needed.toSet()))
    }
    private fun caught(run: SafariRun, name: String) = run.record(SafariCatch(SafariRoster.named(name)!!, false))
    private fun complete(run: SafariRun, name: String, state: PartySparklingState) =
        run.completed(SafariMode.UNIQUE, listOf(SafariRoster.named(name)!!), state) == 1
    private fun footer(run: SafariRun, state: PartySparklingState, label: String) =
        SafariPanels.missing(run, SafariBiome.HAUNTED, true, 0, party = state).rows.singleOrNull { it.label == label }

    @Test fun `Haunted catches leave the checklist but needed discoveries keep progress and reports pending`() {
        val state = party("Gazer", "Gimmiegold")
        val run = SafariRun(0).apply { visitBiome(SafariBiome.HAUNTED) }
        SafariBiome.HAUNTED.critters.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), false)
        assertTrue(SafariPanels.missingCritters(run, SafariBiome.HAUNTED, state).isEmpty())
        assertFalse(complete(run, "Gazer", state))
        assertFalse(complete(run, "Gimmiegold", state))
        assertEquals(SafariBiome.HAUNTED.critters.size - 2, run.completed(SafariMode.UNIQUE, SafariBiome.HAUNTED.critters, state))
        assertNull(run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.UNIQUE, state))
        assertEquals("3", footer(run, state, "Gazers left to check")?.value)
        assertEquals("?", footer(run, state, "Coins left to use")?.value)
        run.partyFullClear("Party > Friend: hd")
        assertFalse(complete(run, "Gazer", state), "local Haunted checking cannot be replaced by an hd")
        assertFalse(complete(run, "Gimmiegold", state))
    }

    @Test fun `Gazer footer counts retained distinct checks and clears only after four checks`() {
        val state = party("Gazer")
        val run = SafariRun(0).apply { visitBiome(SafariBiome.HAUNTED) }
        caught(run, "Gazer")
        val seen = List(3) { "Gazer" to UUID.randomUUID() }
        repeat(2) { run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, seen.take(2)) }
        assertEquals("1", footer(run, state, "Gazers left to check")?.value)
        assertFalse(complete(run, "Gazer", state))
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, seen)
        assertTrue(complete(run, "Gazer", state))
        assertNull(footer(run, state, "Gazers left to check"))
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList())
        assertTrue(complete(run, "Gazer", state), "unloading must retain qualified checks")
    }

    @Test fun `coin footer includes uncollected floor slots held coins and unfulfilled party extras`() {
        val state = party("Gimmiegold")
        val run = SafariRun(0).apply { visitBiome(SafariBiome.HAUNTED); localPlayer("Me") }
        run.inventoryEvidence(emptyList(), 0)
        (1..5).forEach { run.gimmiegoldCheck.floor(it, true) }
        assertEquals("5", footer(run, state, "Coins left to use")?.value)
        caught(run, "Gimmiegold")
        run.gimmiegoldCheck.floor(1, false)
        run.scanGimmiegolds(emptyList(), true)
        assertEquals("4", footer(run, state, "Coins left to use")?.value)
        (2..5).forEach { run.gimmiegoldCheck.floor(it, false) }
        run.inventoryEvidence(listOf("Shining Coin" to 4), 10001)
        assertEquals("4", footer(run, state, "Coins left to use")?.value)
        assertFalse(complete(run, "Gimmiegold", state), "pickup alone is not use")
        run.inventoryEvidence(emptyList(), 10002)
        run.scanGimmiegolds(List(4) { UUID.randomUUID() }, true)
        assertTrue(complete(run, "Gimmiegold", state))
        assertEquals(0, run.coinsLeftToUse)
        assertNull(footer(run, state, "Coins left to use"))
        run.resources.partyNotice("Party > Friend: [SM] Starting items: 2x Shining Coin")
        assertEquals("2", footer(run, state, "Coins left to use")?.value)
        assertFalse(complete(run, "Gimmiegold", state))
        run.scanGimmiegolds(List(6) { UUID.randomUUID() }, true)
        assertTrue(complete(run, "Gimmiegold", state))
        assertNull(footer(run, state, "Coins left to use"))
    }

    @Test fun `unknown or unloaded coin evidence never displays a false zero`() {
        val check = GimmiegoldSparklingCheck()
        assertNull(check.leftToUse)
        check.floor(1, true)
        assertNull(check.leftToUse)
        check.inventory(0)
        check.floor(1, null)
        check.scan(1, emptyList(), true)
        assertEquals(1, check.leftToUse)
        assertFalse(check.complete)
    }

    @Test fun `owned Haunted discoveries need only the catch and no checking footer`() {
        val run = SafariRun(0).apply { visitBiome(SafariBiome.HAUNTED) }
        val state = party()
        for (name in listOf("Gazer", "Gimmiegold")) {
            assertFalse(complete(run, name, state))
            caught(run, name)
            assertTrue(complete(run, name, state))
        }
        assertNull(footer(run, state, "Coins left to use"))
        assertNull(footer(run, state, "Gazers left to check"))
        assertFalse(complete(SafariRun(1), "Gazer", party("Gazer")))
    }

    @Test fun `new Haunted footers are confined to Unique mode`() {
        val state = party("Gazer", "Gimmiegold")
        val run = SafariRun(0).apply { visitBiome(SafariBiome.HAUNTED) }
        for (mode in listOf(SafariMode.FULL_CLEAR, SafariMode.SPARKLING)) {
            ConfigManager.fullClearMode = mode == SafariMode.FULL_CLEAR
            ConfigManager.sparklingMode = mode == SafariMode.SPARKLING
            assertNull(footer(run, state, "Coins left to use"))
            assertNull(footer(run, state, "Gazers left to check"))
        }
    }

    @Test fun `Unique Rockmite catch hides silverfish but retains mounds only for needed discoveries`() {
        for (state in listOf(party(), party("Rockmite"))) {
            val run = SafariRun(0)
            for (mound in listOf(false, true))
                assertTrue(SafariEspRules.neededForRun("Rockmite", mound, run, false, false, state))
            caught(run, "Rockmite")
            for (mound in listOf(false, true)) {
                assertEquals(mound && state.needs("Rockmite"),
                    SafariEspRules.neededForRun("Rockmite", mound, run, false, false, state))
                assertTrue(SafariEspRules.neededForRun("Rockmite", mound, run, true, false, state))
                assertTrue(SafariEspRules.neededForRun("Rockmite", mound, SafariRun(1), false, false, state))
            }
        }
    }
}
