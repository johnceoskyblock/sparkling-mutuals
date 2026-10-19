package net.johnceo.sparklingmutuals.safari.integration

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.resources.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EarlyResourceIntegrationTest {
    private val self = "00000000-0000-0000-0000-000000000001"
    private val friend = "00000000-0000-0000-0000-000000000002"
    private fun party(vararg missing: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - missing.toSet()))
    }
    private fun sources(run: SafariRun, salt: Long = 1) {
        val displays = mutableListOf<FloorDisplay>()
        var id = 100
        for ((count, point) in listOf(6 to (-100.0 to 50.0), 9 to (-20.0 to 50.0),
                9 to (-20.0 to -50.0), 5 to (-100.0 to -50.0))) {
            repeat(count * 3) { displays.add(FloorDisplay(id++, point.first, 65.0, point.second, "minecraft:string:String")) }
        }
        val predictor = FloorDropPredictor()
        val groups = predictor.scan(displays, 0)
        run.floorCatalog.observe(groups, displays.associate { it.id to UUID(salt, it.id.toLong()) })
        assertTrue(run.floorCatalog.complete)
        assertEquals(4, run.floorCatalog.coinIds.size)
        run.resourceCoverage(run.floorCatalog.complete)
        run.floorCatalog.coinIds.forEach { run.gimmiegoldCheck.floor(it, true) }
    }
    private fun receiver(biome: SafariBiome) = SafariRun(0).apply {
        localPlayer("Me", self)
        inventoryEvidence(emptyList(), 0)
        startingMessage("HOTSPOT! Your Hunting Hotspot is the ${biome.label} Biome!", 0)
        resources.contributions.roster(setOf(self, friend))
        ownStartingReport()
        visitBiome(biome)
    }
    @Test fun `two clients share original duplicate food before sender clears and deduplicate legacy echoes`() {
        val sender = SafariRun(0).apply {
            localPlayer("Friend", friend)
            inventoryEvidence(listOf("Bag of Seeds" to 2), 0)
            startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 5000)
            inventoryEvidence(emptyList(), 5001)
            resources.pickup("FLOOR DROP! You found 1x Yogi Berry on the ground!")
            inventoryEvidence(listOf("Yogi Berry" to 1), 5500)
        }
        val receiver = receiver(SafariBiome.FOREST)
        sources(sender); sources(receiver)
        receiver.scanBirdSources(0, List(9) { "Bluebird" })
        assertFalse(receiver.sparklingComplete("Bluebird", party("Bluebird")), "missing report is not zero")
        var wire = ""
        assertFalse(sender.startingAnnouncement.dispatch(6999, true, sender.startingItems(sender.floorCatalog.scope)) { wire = it; true })
        assertTrue(sender.startingAnnouncement.dispatch(7000, true, sender.startingItems(sender.floorCatalog.scope)) { wire = it; true })
        assertTrue(wire.contains("2x Bag of Seeds")); assertFalse(wire.contains("Yogi Berry"))
        val inbox = StartingResourceInbox()
        repeat(2) { inbox.add(receiver, "Party > Friend: $wire", 7000) }
        inbox.drain(receiver, mapOf("friend" to friend), setOf(self, friend), 7001, receiver.floorCatalog::acceptsScope) { id, chat ->
            assertTrue(receiver.resources.reportedBy(id, chat.body))
        }
        receiver.resources.reportedBy(friend, "[SM] Use: 1x Bag of Seeds")
        assertTrue(receiver.resources.contributions.complete)
        receiver.scanBirdSources(0, List(9) { "Bluebird" })
        assertEquals(11, receiver.birds.feedingTarget)
        assertFalse(receiver.sparklingComplete("Bluebird", party("Bluebird")))
        receiver.scanBirdSources(0, List(11) { "Bluebird" })
        assertTrue(receiver.sparklingComplete("Bluebird", party("Bluebird")))
        assertEquals(0, receiver.count("Bluebird"), "a report/render never supplies catches")
    }
    @Test fun `remote hd requires current demand and cannot become valid merely by emptying held coins`() {
        val run = receiver(SafariBiome.FOREST)
        run.resources.reportedBy(friend, "[SM] Starting items: none")
        run.partyFullClear("Party > Friend: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")), "outside hd needs no local renders")
        run.resources.reportedBy(friend, "[SM] Starting items: 1x Shining Coin")
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.partyFullClear("Party > Friend: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.inventoryEvidence(listOf("Shining Coin" to 1), 10000)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.inventoryEvidence(emptyList(), 11000)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")), "a new positive resource contradiction needs updated proof after consumption")
        run.partyFullClear("Party > Friend: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.visitBiome(SafariBiome.HAUNTED)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")), "inside local evidence is authoritative")
    }
    @Test fun `qualified F plus S uses real captures and canonical current models instead of carrier history`() {
        val run = receiver(SafariBiome.HAUNTED)
        sources(run)
        run.resources.reportedBy(friend, "[SM] Starting items: 1x Shining Coin")
        run.gimmiegoldCheck.sourceEvidence(true, 0)
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false))
        val model = UUID.randomUUID(); val alias = UUID.randomUUID()
        run.observeSparkling(model, "Gimmiegold", setOf(alias))
        val others = List(2) { UUID.randomUUID() }
        run.scanGimmiegolds(listOf(model, alias) + others, true)
        assertEquals(5, run.gimmiegoldCheck.required)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")), "C1 plus U3 remains below F4 plus S1")
        run.scanGimmiegolds(listOf(model, alias) + others + UUID.randomUUID(), false)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.scanGimmiegolds(listOf(model, alias) + others + UUID.randomUUID(), true)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        assertEquals(1, run.count("Gimmiegold"))
        assertFalse(run.fullClearComplete("Gimmiegold"))
    }
    @Test fun `a new unclassified remaining source invalidates old outside hd after it disappears`() {
        val run = receiver(SafariBiome.FOREST)
        run.resources.reportedBy(friend, "[SM] Starting items: none")
        run.partyFullClear("Party > Friend: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.gimmiegoldCheck.sourceEvidence(false, 3)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.gimmiegoldCheck.sourceEvidence(false, 0)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")), "disappearance alone cannot revive the old proof")
        run.partyFullClear("Party > Friend: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
    }
    @Test fun `qualified all-opportunity proof survives unknown slot visibility without inventing pickup`() {
        val check = net.johnceo.sparklingmutuals.safari.completion.GimmiegoldSparklingCheck()
        check.floor(1, true)
        check.inventory(0)
        check.sourceEvidence(true, 0)
        check.scan(1, emptyList(), true)
        assertTrue(check.complete)
        assertEquals(1, check.remaining, "no actual pickup was fabricated")
        check.floor(1, null)
        check.sourceEvidence(true, null)
        check.scan(0, emptyList(), false)
        assertTrue(check.complete, "travel may retain already-earned proof")
        check.floor(2, null)
        assertFalse(check.complete, "a genuinely new slot invalidates it")
    }
    @Test fun `known model carrier aliases cannot inflate Full Clear actual capture demand`() {
        val run = receiver(SafariBiome.HAUNTED)
        sources(run)
        run.resources.reportedBy(friend, "[SM] Starting items: none")
        run.gimmiegoldCheck.sourceEvidence(true, 0)
        repeat(4) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        run.scanGimmiegolds(emptyList(), true)
        run.partyFullClear("Party > Friend: hd")
        val model = UUID.randomUUID(); val carrier = UUID.randomUUID()
        run.observeSparkling(model, "Gimmiegold", setOf(carrier))
        run.observePopulation(listOf("Gimmiegold" to model, "Gimmiegold" to carrier))
        assertFalse(run.fullClearComplete("Gimmiegold"))
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false))
        assertTrue(run.fullClearComplete("Gimmiegold"), "one extra model requires one actual catch, not two for its carriers")
    }
}
