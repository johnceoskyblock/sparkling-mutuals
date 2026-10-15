package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class HauntedDropAndPanelFixTest {
    @TempDir lateinit var dir: Path
    private fun party(vararg missing: String) = PartySparklingState().apply {
        select(setOf("local"))
        accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - missing.toSet()))
    }
    private fun group(id: Int, type: FloorDropType?) = PredictedFloorDrop(id, -4.0, 65.0, -64.0, SafariBiome.HAUNTED, type)
    private fun scan(run: SafariRun, coins: Int, live: Int = 0) {
        val seen = List(live) { "Gimmiegold" to UUID.randomUUID() }
        run.observePopulation(seen)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, if (live > 0) setOf("Gimmiegold") else emptySet(), false,
            playerX = -4.0, playerZ = -64.0)
        run.sparklingChecks.inventory(emptyList())
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, seen,
            hauntedDropsRemaining = 1, hauntedCoinDropsRemaining = coins)
    }
    private fun spentRun(coins: Int = 3) = SafariRun(0).apply {
        repeat(coins) {
            sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
            sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
        }
    }
    @Test fun `four actual incense pickups hide incense in all modes even when Gimmiegold is missing`() {
        for (mode in SafariMode.entries) {
            val state = FloorDropState(); val missing = party("Gimmiegold", "Doomspiral")
            state.startingGrant(FloorDropType.INCENSE)
            state.inventory(listOf("Soothing Incense" to 4))
            state.record("FLOOR DROP! You found 3x Soothing Incense!", SafariBiome.HAUNTED)
            assertTrue(state.needed(FloorDropType.INCENSE, mode, missing))
            state.record("FLOOR DROP! Soothing Incense Shard", SafariBiome.HAUNTED)
            assertTrue(state.needed(FloorDropType.INCENSE, mode, missing))
            state.record("FLOOR DROP! You found a Soothing Incense!", SafariBiome.HAUNTED)
            state.inventory(emptyList()); state.visit(SafariBiome.CAVERN); state.visit(SafariBiome.HAUNTED)
            assertFalse(state.needed(FloorDropType.INCENSE, mode, missing))
            assertTrue(state.needed(FloorDropType.COIN, mode, missing))
        }
    }
    @Test fun `coin presence excludes only complete typed incense groups and includes hidden unknown markers`() {
        val incense = group(100, FloorDropType.INCENSE)
        val coin = group(103, FloorDropType.COIN)
        assertEquals(0, HauntedCoinDrops.remaining(listOf(incense.marker()), listOf(incense)))
        assertEquals(1, HauntedCoinDrops.remaining(listOf(incense.marker(), coin.marker()), listOf(incense, coin)))
        assertEquals(1, HauntedCoinDrops.remaining(listOf(incense.marker()), emptyList()))
        for (type in listOf(null, FloorDropType.GENERIC, FloorDropType.COIN)) {
            val uncertain = group(100, type)
            assertEquals(1, HauntedCoinDrops.remaining(listOf(uncertain.marker()), listOf(uncertain)))
        }
        incense.actual = FloorPickup(FloorDropType.COIN, 1)
        assertEquals(1, HauntedCoinDrops.remaining(listOf(incense.marker()), listOf(incense)))
    }
    @Test fun `Gimmiegold capture row turns green with all coin spawns caught despite remaining incense`() {
        ConfigManager.init(dir); ConfigManager.fullClearMode = true
        val run = spentRun()
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        scan(run, 0)
        assertTrue(run.captureComplete("Gimmiegold"))
        assertEquals(0xFF55FF55.toInt(), SafariPanels.captures(run, SafariBiome.HAUNTED).rows.single { it.label == "Gimmiegold" }.valueColor)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
    }
    @Test fun `spawned but uncaught Gimmiegolds satisfy checking only and keep Full Clear red`() {
        val run = spentRun()
        scan(run, 0, live = 3)
        assertFalse(run.captureComplete("Gimmiegold"))
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        scan(run, 1, live = 3)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
    }
    @Test fun `extra spawned Gimmiegold must also be caught and held coins remain blockers`() {
        val run = spentRun(4)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!)) }
        scan(run, 0)
        assertFalse(run.captureComplete("Gimmiegold"))
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        scan(run, 0)
        assertTrue(run.captureComplete("Gimmiegold"))
        run.sparklingChecks.inventory(listOf("Shining Coin" to 1))
        assertFalse(run.captureComplete("Gimmiegold"))
    }
    @Test fun `every missing panel lists plain colored species with only applicable gray counters`() {
        ConfigManager.init(dir)
        for (mode in SafariMode.entries) {
            ConfigManager.fullClearMode = mode == SafariMode.FULL_CLEAR
            ConfigManager.sparklingMode = mode == SafariMode.SPARKLING
            val run = SafariRun(0)
            val missing = party(*SafariRoster.all.map { it.name }.toTypedArray())
            for (biome in SafariBiome.entries) {
                val rows = SafariPanels.missing(run, biome, mode == SafariMode.UNIQUE, 8, 9,
                    WallSummary(List(5) { WallState.INTACT }), missing).rows
                rows.filter { it.label.isNotBlank() && it.label !in setOf("Bee nests to punch", "Snooper walls to break", "Mounds to break") }.forEach { row ->
                    val species = SafariRoster.named(row.label)
                    assertNotNull(species, row.label)
                    assertEquals(species!!.color, row.color)
                    assertEquals(HudRow.GRAY, row.valueColor)
                    if (row.value != null) assertTrue(row.value.matches(Regex("0/[34] (captured|checked)")), row.value)
                }
                if (biome == SafariBiome.HAUNTED) {
                    assertEquals(1, rows.count { it.label == "Gazer" })
                    assertEquals(if (mode == SafariMode.FULL_CLEAR) "0/4 captured" else "0/4 checked", rows.single { it.label == "Gazer" }.value)
                }
            }
        }
    }
    @Test fun `Unique pending Gazer check stays as one plain Gazer row after the first catch`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        run.record(SafariCatch(SafariRoster.named("Gazer")!!))
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, listOf("Gazer" to UUID.randomUUID()))
        val rows = SafariPanels.missing(run, SafariBiome.HAUNTED, true, 0, party = party("Gazer")).rows
        assertEquals("2/4 checked", rows.single { it.label == "Gazer" }.value)
        assertFalse(rows.any { it.label.startsWith("Gazers") || it.label.contains("still needs") })
    }
}
