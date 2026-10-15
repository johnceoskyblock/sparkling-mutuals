package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.esp.EspDrop
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.johnceo.sparklingmutuals.safari.resources.HauntedCoinDrops
import net.johnceo.sparklingmutuals.safari.resources.PredictedFloorDrop
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GimmiegoldCompletionRecoveryTest {
    @TempDir lateinit var dir: Path
    private fun party() = PartySparklingState().apply {
        select(setOf("local")); accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - "Gimmiegold"))
    }
    private fun run(coins: Int = 3) = SafariRun(0).apply {
        repeat(coins) { sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!") }
        inventoryEvidence(emptyList(), 10000)
    }
    private fun scan(run: SafariRun, live: List<Pair<String, UUID>>, drops: Int = 0, x: Double = -4.0) {
        run.observePopulation(live)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, live.map { it.first }.toSet(), false, playerX = x, playerZ = -64.0)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, x, -64.0, live, hauntedCoinDropsRemaining = drops)
    }
    @Test fun `sewer shared catch plus current live population completes Unique and Sparkling without every spawn message`() {
        val run = run(6)
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false))
        val live = List(5) { "Gimmiegold" to UUID.randomUUID() }
        scan(run, live)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        assertTrue(run.uniqueComplete("Gimmiegold", party()))
        assertFalse(run.captureComplete("Gimmiegold"))
        scan(run, emptyList(), x = -30.0)
        assertTrue(run.sparklingComplete("Gimmiegold", party()), "Ordinary movement must not discard an already validated check")
    }
    @Test fun `all confirmed captures turn Full Clear green despite missed coin spawn chat`() {
        ConfigManager.init(dir); ConfigManager.fullClearMode = true
        val run = run(6)
        repeat(6) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        scan(run, emptyList())
        assertTrue(run.captureComplete("Gimmiegold"))
        scan(run, emptyList(), x = -30.0)
        assertTrue(run.captureComplete("Gimmiegold"), "Verified coin-source absence survives ordinary movement")
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        assertEquals(0xFF55FF55.toInt(), SafariPanels.captures(run, SafariBiome.HAUNTED).rows.single { it.label == "Gimmiegold" }.valueColor)
    }
    @Test fun `population below target duplicate UUIDs held coins and remaining floor markers block recovery`() {
        val run = run()
        val id = UUID.randomUUID()
        scan(run, List(3) { "Gimmiegold" to id })
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        val live = List(3) { "Gimmiegold" to UUID.randomUUID() }
        run.inventoryEvidence(listOf("Shining Coin" to 1), 10000)
        scan(run, live)
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        run.inventoryEvidence(emptyList(), 10000)
        for (remaining in listOf(1, -1)) {
            scan(run, live, drops = remaining)
            assertFalse(run.sparklingComplete("Gimmiegold", party()))
        }
        scan(run, live)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
    }
    @Test fun `new coin and Haunted reentry require a fresh full population check`() {
        val run = run(); run.visitBiome(SafariBiome.HAUNTED)
        val firstThree = List(3) { "Gimmiegold" to UUID.randomUUID() }
        scan(run, firstThree)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
        run.inventoryEvidence(emptyList(), 10000)
        scan(run, firstThree)
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        scan(run, listOf("Gimmiegold" to UUID.randomUUID()))
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.visitBiome(SafariBiome.CAVERN); run.visitBiome(SafariBiome.HAUNTED)
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        scan(run, emptyList())
        assertTrue(run.sparklingComplete("Gimmiegold", party()), "Revalidate source conditions while retaining the checked UUIDs")
    }
    @Test fun `complete incense triple across block boundaries never masquerades as uncollected coins`() {
        val group = PredictedFloorDrop(100, -4.01, 65.0, -64.01, SafariBiome.HAUNTED, FloorDropType.INCENSE)
        val raw = listOf(EspDrop(100, -5, 65, -65), EspDrop(101, -4, 65, -64), EspDrop(102, -5, 65, -64))
        assertEquals(0, HauntedCoinDrops.remaining(raw, listOf(group)))
        assertEquals(3, HauntedCoinDrops.remaining(raw, emptyList()), "Unclassified/partial groups still block")
    }
    @Test fun `extra coin after mid run completion requires another Gimmiegold rather than reusing the old three`() {
        val run = SafariRun(0)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!)) }
        run.inventoryEvidence(emptyList(), 10000)
        scan(run, emptyList())
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
        run.inventoryEvidence(emptyList(), 10000)
        scan(run, emptyList())
        assertFalse(run.captureComplete("Gimmiegold"))
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        scan(run, emptyList())
        assertTrue(run.captureComplete("Gimmiegold"))
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
    }

}
