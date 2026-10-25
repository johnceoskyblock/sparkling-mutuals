package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GimmiegoldProvidedLogRegressionTest {
    @Test fun `JohnFounder six spent coins retain earlier checks when one Gimmiegold unloads before final pickup`() {
        val run = SafariRun(0).apply { visitBiome(SafariBiome.HAUNTED) }
        (1..6).forEach { run.gimmiegoldCheck.floor(it, true) }
        coins(run, 5)
        val firstFive = List(5) { "Gimmiegold" to UUID.randomUUID() }
        scan(run, firstFive)
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "one coin slot is still uncollected")
        repeat(2) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!)) }
        scan(run, firstFive.takeLast(2))
        coins(run, 1)
        val last = "Gimmiegold" to UUID.randomUUID()
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        scan(run, listOf(firstFive.last(), last))
        assertEquals(3, run.count("Gimmiegold"))
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        assertTrue(run.uniqueComplete("Gimmiegold", party()))
        assertEquals(0, run.coinsLeftToUse)
        assertFalse(run.captureComplete("Gimmiegold"), "checks must not fabricate Full Clear catches")
        run.gimmiegoldCheck.inventory(1)
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "held coins still block completion")
    }
    private val live = listOf(
        "Gimmiegold" to UUID.fromString("fb6d1f04-d5d8-b124-de98-581d4b89a3e0"),
        "Gimmiegold" to UUID.fromString("0ca20a18-6f87-440d-9df2-0a46da4911d0"))
    private fun party() = PartySparklingState().apply {
        select(setOf("local")); accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - "Gimmiegold"))
    }
    private val picked = mutableMapOf<SafariRun, Int>()
    private fun coins(run: SafariRun, amount: Int) = repeat(amount) {
        if (run.gimmiegoldCheck.floorMinimum == 0) (1..3).forEach { run.gimmiegoldCheck.floor(it, true) }
        val id = (picked[run] ?: 0) + 1
        picked[run] = id
        run.gimmiegoldCheck.floor(id, false)
        run.sparklingChecks.chat("FLOOR DROP! You found a Shining Coin!")
        run.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
    }
    private fun scan(run: SafariRun, seen: List<Pair<String, UUID>> = live) {
        run.inventoryEvidence(emptyList(), 10000)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, seen.map { it.first }.toSet(), false, playerX = 0.0, playerZ = -64.0)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, 0.0, -64.0, seen, hauntedCoinDropsRemaining = 11)
        run.scanGimmiegolds(seen.map { it.second }, true)
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
        run.gimmiegoldCheck.floor(4, false)
        coins(run, 1)
        scan(run, listOf("Gimmiegold" to UUID.randomUUID()))
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "old UUID history cannot satisfy the new floor-derived total")
        scan(run, List(4) { "Gimmiegold" to UUID.randomUUID() })
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
    }

    @Test fun `starting coin held coin and extra party notices cannot be waived by the marker override`() {
        val run = SafariRun(0); coins(run, 2)
        run.resources.headStart("HEAD START! You started with a Shining Coin!")
        run.sparklingChecks.startingCoins(1)
        run.sparklingChecks.chat("A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!")
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        scan(run)
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "Starting coins do not replace baseline floor pickups")
        coins(run, 1)
        scan(run, List(3) { "Gimmiegold" to UUID.randomUUID() })
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.inventoryEvidence(listOf("Shining Coin" to 1), 20000)
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
        coins(run, 1)
        scan(run, oldIds.take(2))
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        scan(run, listOf("Gimmiegold" to UUID.randomUUID()))
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "one fresh sighting cannot stand in for four current-or-captured critters")
        scan(run, oldIds.take(4))
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
    }

}
