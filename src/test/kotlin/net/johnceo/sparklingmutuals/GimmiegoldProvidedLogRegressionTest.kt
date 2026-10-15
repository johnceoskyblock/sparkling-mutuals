package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class GimmiegoldProvidedLogRegressionTest {
    private val live = listOf(
        "Gimmiegold" to UUID.fromString("fb6d1f04-d5d8-b124-de98-581d4b89a3e0"),
        "Gimmiegold" to UUID.fromString("0ca20a18-6f87-440d-9df2-0a46da4911d0"))
    private fun party() = PartySparklingState().apply {
        select(setOf("local")); accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - "Gimmiegold"))
    }
    private fun coins(run: SafariRun, amount: Int) = repeat(amount) {
        run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
        run.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
    }
    private fun scan(run: SafariRun, seen: List<Pair<String, UUID>> = live) {
        run.inventoryEvidence(emptyList(), 10000)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, seen.map { it.first }.toSet(), false, playerX = 0.0, playerZ = -64.0)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, 0.0, -64.0, seen, hauntedCoinDropsRemaining = 11)
    }
    @Test fun `provided log one capture two live three picked spent and eleven predictions completes checks`() {
        val run = SafariRun(0); coins(run, 3)
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        scan(run)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        assertTrue(run.uniqueComplete("Gimmiegold", party()))
        assertFalse(run.captureComplete("Gimmiegold"))
        val debug = run.sparklingChecks.gimmiegoldDebug()
        assertTrue(debug.contains("sourcePath=confirmed-coins"), debug)
        assertTrue(debug.contains("liveUuids=2 captures=1"), debug)
        repeat(2) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!)) }
        scan(run, emptyList())
        assertTrue(run.captureComplete("Gimmiegold"))
    }
    @Test fun `retained UUID history is not treated as extra coin opportunities`() {
        val run = SafariRun(0); coins(run, 3)
        repeat(8) { scan(run, listOf("Gimmiegold" to UUID.randomUUID())) }
        assertEquals(3, run.sparklingChecks.gimmiegoldMinimum)
        assertTrue(run.sparklingChecks.gimmiegoldDebug().contains("accounted=3 retainedChecks=8"))
        run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
        assertEquals(4, run.sparklingChecks.gimmiegoldMinimum)
        // Observe the new fourth critter before its delayed coin-use message.
        val old = UUID.randomUUID()
        scan(run, listOf("Gimmiegold" to old))
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "The fourth known coin still needs consumption")
        run.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
        scan(run, listOf("Gimmiegold" to old))
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
    }
    @Test fun `starting coin held coin and extra party notices cannot be waived by the marker override`() {
        val run = SafariRun(0); coins(run, 2)
        run.sparklingChecks.startingCoins(1)
        run.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        scan(run)
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "Starting coins do not replace baseline floor pickups")
        coins(run, 1)
        scan(run, List(3) { "Gimmiegold" to UUID.randomUUID() })
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.sparklingChecks.inventory(listOf("Shining Coin" to 1))
        assertFalse(run.sparklingChecks.gimmiegoldReady())
        run.resources.scan(SafariBiome.HAUNTED, 4)
        assertTrue(run.resources.partyNotice("Party > Other: [SM] Unused starting item: 1x Shining Coin -> Haunted (Other)"))
        scan(run)
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
    }
    @Test fun `a later coin cannot reuse surplus historical UUIDs as its new check`() {
        val run = SafariRun(0); coins(run, 3)
        val oldIds = List(8) { "Gimmiegold" to UUID.randomUUID() }
        scan(run, oldIds)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
        run.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
        scan(run, oldIds.take(2))
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        scan(run, listOf("Gimmiegold" to UUID.randomUUID()))
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
    }

}
