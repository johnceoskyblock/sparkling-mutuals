package net.johnceo.sparklingmutuals.safari.integration

import net.johnceo.sparklingmutuals.safari.resources.knownStartingEvidence

import net.johnceo.sparklingmutuals.safari.resources.renderBird
import net.johnceo.sparklingmutuals.safari.resources.scanRenderedBirds

import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariEntryEvidence
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules
import net.johnceo.sparklingmutuals.safari.resources.FloorDisplay
import net.johnceo.sparklingmutuals.safari.resources.FloorDropPredictor
import net.johnceo.sparklingmutuals.safari.resources.FloorDropState
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.johnceo.sparklingmutuals.safari.resources.FloorPickup
import net.johnceo.sparklingmutuals.safari.resources.SafariBirdLedger
import net.johnceo.sparklingmutuals.safari.resources.SafariResources
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DocumentedSafariRulesTest {
    private fun party(vararg missing: String) = PartySparklingState().apply {
        select(setOf("local")); accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - missing.toSet()))
    }
    private fun caught(run: SafariRun, name: String, count: Int = 1) = repeat(count) { run.record(SafariCatch(SafariRoster.named(name)!!, false)) }
    private fun group(id: Int, x: Double, z: Double) = List(3) { FloorDisplay(id + it, x + it * .1, 70.0, z, "minecraft:string:String") }

    @Test fun `prediction follows ID gaps and retained anchors with five icy slots`() {
        val predictor = FloorDropPredictor()
        val displays = (0..5).flatMap { group(10 + it * 3, -110.0, 50.0) } +
            (0..8).flatMap { group(40 + it * 3, 0.0, 50.0) } +
            (0..7).flatMap { group(80 + it * 3, 0.0, -50.0) } +
            (0..5).flatMap { group(110 + it * 3, -110.0, -50.0) }
        val result = predictor.scan(displays)
        assertEquals(2, result.count { it.type == FloorDropType.ORANGE })
        assertEquals(3, result.count { it.type == FloorDropType.SEEDS })
        assertEquals(5, result.count { it.type == FloorDropType.INCENSE })
        assertEquals(3, result.count { it.type == FloorDropType.COIN })
        assertEquals(5, result.count { it.type == FloorDropType.ICEBREAKER })
        assertEquals(FloorDropType.GENERIC, result.last().type)
        assertEquals(FloorDropType.ORANGE, predictor.scan(group(25, -110.0, 50.0)).single().type)
        predictor.reset()
        assertEquals(FloorDropType.LIME, predictor.scan(group(25, -110.0, 50.0)).single().type)
    }

    @Test fun `partial unequal and distant display groups are not predicted`() {
        val p = FloorDropPredictor()
        assertTrue(p.scan(group(1, 0.0, 50.0).take(2)).isEmpty())
        assertTrue(p.scan(group(1, 0.0, 50.0).mapIndexed { i, d -> if (i == 1) d.copy(description = "other") else d }).isEmpty())
        assertTrue(p.scan(group(1, 0.0, 50.0).mapIndexed { i, d -> if (i == 2) d.copy(x = 40.0) else d }).isEmpty())
        assertNull(p.scan(group(1, 500.0, 500.0)).single().type)
    }

    @Test fun `each gem color stops independently and resources persist after consumption`() {
        val s = FloorDropState()
        s.record("FLOOR DROP! You found an Orange Gem on the ground!", SafariBiome.CAVERN)
        assertFalse(s.needed(FloorDropType.ORANGE, SafariMode.FULL_CLEAR, party()))
        assertTrue(s.needed(FloorDropType.LIME, SafariMode.FULL_CLEAR, party()))
        s.inventory(emptyList()); s.visit(SafariBiome.ICY); s.visit(SafariBiome.CAVERN)
        assertFalse(s.needed(FloorDropType.ORANGE, SafariMode.UNIQUE, party("Gemzie")))
        s.reset()
        assertTrue(s.needed(FloorDropType.ORANGE, SafariMode.FULL_CLEAR, party()))
    }

    @Test fun `haunted retains needed coins but caps owned discovery coins and incense separately`() {
        val s = FloorDropState()
        s.inventory(listOf("Soothing Incense" to 1))
        s.record("FLOOR DROP! You found 3x Soothing Incense on the ground!", SafariBiome.HAUNTED)
        s.record("FLOOR DROP! You found Shining Coin x1 on the ground!", SafariBiome.HAUNTED)
        s.inventory(emptyList())
        for (mode in SafariMode.entries) {
            assertEquals(mode != SafariMode.SPARKLING, s.needed(FloorDropType.INCENSE, mode, party("Gimmiegold")))
            assertFalse(s.needed(FloorDropType.INCENSE, mode, party()))
            assertEquals(mode == SafariMode.FULL_CLEAR, s.needed(FloorDropType.COIN, mode, party()))
            assertTrue(s.needed(FloorDropType.COIN, mode, party("Gimmiegold")))
        }
        assertNull(FloorPickup.parse("FLOOR DROP! You found Shining Coin Shard on the ground!"))
        assertNull(FloorPickup.parse("Party > Friend: FLOOR DROP! Shining Coin"))
    }

    @Test fun `icy offers one Icebreaker only in full clear`() {
        val s = FloorDropState()
        assertTrue(s.needed(FloorDropType.ICEBREAKER, SafariMode.FULL_CLEAR, party()))
        assertFalse(s.needed(FloorDropType.ICEBREAKER, SafariMode.UNIQUE, party()))
        assertFalse(s.needed(FloorDropType.ICEBREAKER, SafariMode.SPARKLING, party()))
        s.inventory(listOf("Icebreaker" to 1)); s.inventory(emptyList())
        assertFalse(s.needed(FloorDropType.ICEBREAKER, SafariMode.FULL_CLEAR, party()))
    }

    @Test fun `Gemzie announcement alone is pending and distinct run UUID checks persist`() {
        val run = SafariRun(0).knownStartingEvidence()
        run.sparklingChecks.chat("A rumbling sound can be heard, and the door at the back of the chamber opens...", true)
        assertFalse(run.sparklingChecks.checked("Gemzie"))
        val first = UUID.randomUUID()
        repeat(4) { run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, listOf("Gemzie" to first)) }
        assertFalse(run.sparklingChecks.checked("Gemzie"))
        caught(run, "Gemzie")
        run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, List(2) { "Gemzie" to UUID.randomUUID() })
        assertTrue(run.uniqueComplete("Gemzie", party("Gemzie")))
        run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, emptyList())
        assertTrue(run.uniqueComplete("Gemzie", party("Gemzie")))
        assertEquals(1, run.count("Gemzie"))
    }

    @Test fun `party reports never supply Unique catches and reentry revalidates other modes`() {
        for ((biome, word) in listOf(SafariBiome.FOREST to "fd", SafariBiome.CAVERN to "cd", SafariBiome.ICY to "id", SafariBiome.HAUNTED to "hd")) {
            val run = SafariRun(0).knownStartingEvidence()
            SafariMessages.forRun("Party > Friend: $word", run, true)
            assertEquals(0, run.completed(SafariMode.UNIQUE, biome.critters, party()))
            assertEquals(biome.critters.count { SafariFullClear.minimum(it.name) == 0 && it.name !in setOf("Bluebird", "Parakeet", "Macaw") }, run.completed(SafariMode.FULL_CLEAR, biome.critters, party()))
            run.visitBiome(biome)
            assertEquals(if (biome == SafariBiome.FOREST) 1 else 0, run.completed(SafariMode.SPARKLING, biome.critters, party()),
                "Honeybug retains its trusted Forest report on entry")
        }
    }

    @Test fun `late catches satisfy radius checks without old UUIDs`() {
        val run = SafariRun(0).knownStartingEvidence()
        caught(run, "Gemzie", 3)
        run.visitBiome(SafariBiome.CAVERN)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false, playerX = 0.0, playerZ = 0.0)
        assertFalse(run.captureComplete("Gemzie"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false, playerX = -141.0, playerZ = 51.0)
        assertTrue(run.captureComplete("Gemzie"))
        run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, emptyList())
        assertTrue(run.uniqueComplete("Gemzie", party("Gemzie")))
    }

    @Test fun `honeybug source accounting uses catches plus live bees and known nest coverage`() {
        val run = SafariRun(0).knownStartingEvidence()
        caught(run, "Honeybug")
        run.updateNestEvidence(false, 0, 3, 0)
        run.liveHoneybugs(2)
        assertTrue(run.nestsResolved)
        assertTrue(run.uniqueComplete("Honeybug", party("Honeybug")))
        assertTrue(SafariHelperRules.showNests(SafariMode.FULL_CLEAR, run, party("Honeybug")))
        run.updateNestEvidence(false, 0, 4, 0)
        assertFalse(run.nestsResolved)
        assertTrue(run.uniqueComplete("Honeybug", party("Honeybug")))
    }

    @Test fun `empty Haunted requires minimum catches and no held coins`() {
        val run = SafariRun(0).knownStartingEvidence()
        (1..3).forEach { run.gimmiegoldCheck.floor(it, false) }
        run.inventoryEvidence(emptyList(), 10000)
        caught(run, "Gimmiegold", 2)
        run.scanGimmiegolds(emptyList(), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = 0)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        caught(run, "Gimmiegold")
        run.scanGimmiegolds(emptyList(), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = 0)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.inventoryEvidence(listOf("Shining Coin" to 1), 20000)
        run.scanGimmiegolds(emptyList(), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = 0)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
    }

    @Test fun `loot share Chat Heads variant gives one shared catch regardless of shard quantity`() {
        val catch = SafariCatch.parse("LOOT SHARE! You received 12x Gemzie Shard from [Friend head]Friend catching a Gemzie!")!!
        assertEquals("Gemzie", catch.critter.name); assertFalse(catch.personal)
        assertEquals("Friend", catch.catcherName)
        val run = SafariRun(0).knownStartingEvidence(); run.record(catch)
        assertEquals(1, run.count("Gemzie")); assertEquals(0, run.personalCount("Gemzie"))
        assertEquals("Friend", run.catches.single().catcherName)
    }

    @Test fun `one shared Macaw completes Full Clear despite extra scanned Macaws after all food is used`() {
        val run = SafariRun(0).knownStartingEvidence()
        val live = List(20) { "Macaw" to UUID.randomUUID() }
        run.observePopulation(live)
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false, macawsInRange = 20)
        caught(run, "Macaw")
        repeat(8) { run.birds.renderBird("Bluebird") }
        assertFalse(run.captureComplete("Macaw"))
        run.birds.renderBird("Macaw")
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
        repeat(8) { caught(run, "Bluebird") }
        assertTrue(run.captureComplete("Macaw"))
        assertFalse(run.captureComplete("Macaw", personalOnly = true))
        assertTrue(run.captureComplete("Bluebird"))
        run.recordBirdFood("FLOOR DROP! 10x Bag of Seeds")
        assertFalse(run.captureComplete("Macaw"))
        repeat(10) { run.birds.renderBird("Macaw") }
        run.scanRenderedBirds()
        assertTrue(run.captureComplete("Macaw"))
        assertEquals(1, run.count("Macaw"))
    }

    @Test fun `additional verified population survives an unload but escape replacement adds no quota`() {
        val run = SafariRun(0).knownStartingEvidence()
        caught(run, "Foxtrot", 6)
        run.observePopulation(listOf("Foxtrot" to UUID.randomUUID()))
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, playerX = 3.0, playerZ = 47.0)
        assertFalse(run.captureComplete("Foxtrot"))
        caught(run, "Foxtrot")
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, playerX = 3.0, playerZ = 47.0)
        assertTrue(run.captureComplete("Foxtrot"))
        val rare = SafariRun(0).knownStartingEvidence()
        repeat(10) { rare.observePopulation(listOf("Billygoat" to UUID.randomUUID())) }
        caught(rare, "Billygoat", 2)
        rare.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false, playerX = -121.0, playerZ = -55.0)
        assertTrue(rare.captureComplete("Billygoat"))
    }

    @Test fun `pickup correlation retains actual reward separately and does not rebase anchors`() {
        val predictor = FloorDropPredictor()
        predictor.scan(group(10, -110.0, 50.0) + group(13, -105.0, 50.0), now = 1000)
        predictor.scan(group(13, -105.0, 50.0), now = 1500)
        val correlated = predictor.pickup(FloorPickup(FloorDropType.ORANGE, 1), -110.0, 70.0, 50.0, 2000)!!
        assertEquals(FloorDropType.LIME, correlated.type)
        assertEquals(FloorDropType.ORANGE, correlated.actual?.type)
        assertEquals(FloorDropType.PURPLE, predictor.scan(group(13, -105.0, 50.0), now = 2500).single().type)
        assertNull(predictor.pickup(FloorPickup(FloorDropType.COIN, 1), -150.0, 70.0, 50.0, 2500))
    }

    @Test fun `entry messages grants and two-item starting inventory remain distinct from pickups`() {
        val entry = SafariEntryEvidence()
        entry.ticket = SafariTicketType.fromItem("First-Class Critter Safari Ticket")
        entry.chat("HEAD START! You started with a Shining Coin!")
        entry.chat("HOTSPOT! Your Hunting Hotspot is the Haunted Biome!")
        assertEquals(SafariTicketType.FIRST_CLASS, entry.ticket)
        assertEquals(FloorDropType.COIN, entry.headStart)
        assertEquals(SafariBiome.HAUNTED, entry.hotspot)
        val resources = SafariResources()
        repeat(2) { resources.headStart("HEAD START! You started with a Shining Coin!") }
        resources.inventory(listOf("Shining Coin" to 2), true)
        resources.pickup("FLOOR DROP! Shining Coin")
        resources.inventory(listOf("Shining Coin" to 3), true)
        assertEquals(2, resources.startingCount(FloorDropType.COIN))
        assertNull(SafariTicketType.fromItem("Unknown Critter Safari Ticket"))
        val birds = SafariBirdLedger()
        birds.startingGrant("Bag of Seeds")
        birds.startingInventory(listOf("Bag of Seeds" to 1), 1000)
        assertEquals(10, birds.feedingTarget)
    }

    @Test fun `extra coin notice requires an additional checked spawn and a Full Clear catch`() {
        val run = SafariRun(0).knownStartingEvidence()
        (1..3).forEach { run.gimmiegoldCheck.floor(it, false) }
        run.inventoryEvidence(emptyList(), 10000)
        caught(run, "Gimmiegold", 3)
        run.scanGimmiegolds(emptyList(), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = 0)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.resources.partyNotice("Party > Friend: [SM] Unused starting item: 1x Shining Coin -> Haunted")
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.resources.spawn(SafariBiome.HAUNTED)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        val id = UUID.randomUUID()
        run.scanGimmiegolds(listOf(id), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, listOf("Gimmiegold" to id), hauntedDropsRemaining = 0)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), false)
        assertFalse(run.captureComplete("Gimmiegold"))
        caught(run, "Gimmiegold")
        assertTrue(run.captureComplete("Gimmiegold"))
    }

    @Test fun `resource notices cannot reuse old population or discharge multiple items with one spawn`() {
        val r = SafariResources()
        r.scan(SafariBiome.HAUNTED, 3)
        assertTrue(r.partyNotice("Party > Friend: [SM] Unused starting item: 2x Shining Coin -> Haunted"))
        r.scan(SafariBiome.HAUNTED, 3); assertTrue(r.pending(SafariBiome.HAUNTED))
        r.spawn(SafariBiome.HAUNTED); assertTrue(r.pending(SafariBiome.HAUNTED))
        r.scan(SafariBiome.HAUNTED, 4); assertTrue(r.pending(SafariBiome.HAUNTED))
        r.spawn(SafariBiome.HAUNTED); assertFalse(r.pending(SafariBiome.HAUNTED))
        r.partyNotice("Party > Friend: [SM] Unused starting item: 2x Shining Coin -> Haunted")
        assertFalse(r.pending(SafariBiome.HAUNTED))
    }
    @Test fun `starting resource reminders respect discovery destination consumption and duplicate gates`() {
        val r = SafariResources()
        r.inventory(listOf("Shining Coin" to 1), true)
        assertTrue(r.notices("Local", SafariBiome.FOREST, party()).isEmpty())
        assertTrue(r.notices("Local", SafariBiome.HAUNTED, party("Gimmiegold")).isEmpty())
        val notice = r.notices("Local", SafariBiome.FOREST, party("Gimmiegold")).single()
        assertTrue(r.noticeStillRelevant(notice, SafariBiome.FOREST, party("Gimmiegold")))
        r.noticeSent("Local", notice)
        assertTrue(r.notices("Local", SafariBiome.FOREST, party("Gimmiegold")).isEmpty())
        r.inventory(emptyList(), false)
        assertFalse(r.noticeStillRelevant(notice, SafariBiome.FOREST, party("Gimmiegold")))
    }
}
