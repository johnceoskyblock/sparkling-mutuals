package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StartingResourceDeliveryTest {
    private fun party(vararg needed: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needed.toSet())))
    }
    @Test fun `mixed starting items produce one short message and one combined title`() {
        val run = SafariRun(0)
        val needs = party("Gimmiegold", "Macaw")
        run.inventoryEvidence(listOf("Shining Coin" to 1, "Yogi Berry" to 1), 0)
        run.resourceReminders.afterBiomeClear(0)
        val sent = mutableListOf<String>()
        assertTrue(run.resourceReminders.dispatch(4000, "Local", SafariBiome.CAVERN, needs) { text, notice ->
            sent.add(text)
            assertEquals(2, notice.items.size)
            assertEquals("USE YOUR STARTING ITEMS!", notice.title)
            assertTrue(notice.detail.contains("Haunted")); assertTrue(notice.detail.contains("Forest"))
        })
        assertEquals(1, sent.size)
        val notice = sent.single()
        assertTrue(notice.startsWith("[SM] Unused floor drops: "))
        assertTrue(notice.contains("1x Shining Coin")); assertTrue(notice.contains("1x Yogi Berry"))
        assertEquals("[SM] Unused floor drops: 1x Yogi Berry (missing Sparkling Macaw); 1x Shining Coin (missing Sparkling Gimmiegold)", notice)
        assertEquals(2, StartingResourceNotice.parse(notice, needs)!!.items.size)
        assertTrue(SafariResources().partyNotice("Party > Friend: $notice"))
        assertTrue(notice.length < 256)
        assertFalse(run.resourceReminders.dispatch(8000, "Local", SafariBiome.CAVERN, needs) { _, _ -> fail("duplicate message") })
    }
    @Test fun `all foods share missing bird explanation and fit one party message`() {
        val needs = party("Gimmiegold", "Bluebird", "Parakeet", "Macaw")
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Shining Coin" to 2, "Wriggleworm" to 2, "Yogi Berry" to 2, "Bag of Seeds" to 2), 0)
        run.resourceReminders.afterBiomeClear(0)
        assertTrue(run.resourceReminders.dispatch(4000, "Local", SafariBiome.CAVERN, needs) { text, _ ->
            assertTrue(text.contains("missing Sparkling Bluebird, Parakeet, Macaw"))
            assertTrue(text.length < 256)
            assertEquals(4, StartingResourceNotice.parse(text, needs)!!.items.size)
            val receiver = SafariResources()
            assertTrue(receiver.partyNotice("Party > Friend: $text"))
            assertEquals(2, receiver.coinContributions()["friend"])
            assertTrue(receiver.pending(SafariBiome.FOREST))
        })
    }
    @Test fun `malformed combined notices cannot partially reopen a biome`() {
        for (text in listOf("[SM] Unused floor drops: 1x Shining Coin (missing Sparkling Macaw)",
            "[SM] Unused floor drops: 1x Shining Coin (missing Sparkling Gimmiegold); 0x Yogi Berry (missing Sparkling Macaw)",
            "[SM] Use: 1x Shining Coin + 0x Yogi Berry",
            "[SM] Use: 1x Shining Coin + 1x Icebreaker",
            "[SM] Use: 1x Shining Coin + 1x Shining Coin")) {
            val receiver = SafariResources()
            assertFalse(receiver.partyNotice("Party > Friend: $text"))
            assertFalse(receiver.pending(SafariBiome.FOREST))
            assertFalse(receiver.pending(SafariBiome.HAUNTED))
        }
    }
    @Test fun `failed batch dispatch leaves every item available for retry`() {
        val run = SafariRun(0)
        val needs = party("Gimmiegold", "Macaw")
        run.inventoryEvidence(listOf("Shining Coin" to 1, "Bag of Seeds" to 1), 0)
        run.resourceReminders.afterBiomeClear(0)
        assertThrows(IllegalStateException::class.java) {
            run.resourceReminders.dispatch(4000, "Local", SafariBiome.CAVERN, needs) { _, _ -> error("not sent") }
        }
        assertFalse(run.resources.pending(SafariBiome.HAUNTED)); assertFalse(run.resources.pending(SafariBiome.FOREST))
        assertTrue(run.resourceReminders.dispatch(4001, "Local", SafariBiome.CAVERN, needs) { _, notice -> assertEquals(2, notice.items.size) })
    }
    @Test fun `preparing reminders does not consume unsent candidates or create source obligations`() {
        val resources = SafariResources()
        resources.inventory(listOf("Shining Coin" to 1, "Bag of Seeds" to 1), true)
        val needs = party("Gimmiegold", "Macaw")
        assertEquals(2, resources.notices("Local", SafariBiome.CAVERN, needs).size)
        assertFalse(resources.pending(SafariBiome.HAUNTED), "no coin notice has been delivered")
        assertFalse(resources.pending(SafariBiome.FOREST), "no food notice has been delivered")
        assertEquals(1, resources.notices("Local", SafariBiome.HAUNTED, needs).size)
        assertEquals(2, resources.notices("Local", SafariBiome.CAVERN, needs).size,
            "temporarily suppressed coin must still be available")
    }
    @Test fun `first actual inventory after delayed world arrival still records two starting items`() {
        val ledger = SafariLedger()
        ledger.worldChanged(0)
        ledger.confirmEntry(10000)
        val run = ledger.current!!
        run.resources.headStart("HEAD START! You started with a Shining Coin!")
        run.inventoryEvidence(listOf("Shining Coin" to 2), 10000)
        assertEquals(2, run.resources.startingCount(FloorDropType.COIN))
        assertEquals(2, StartingResourceReminder.parse(run.resources.notices("Local", SafariBiome.CAVERN,
            party("Gimmiegold")).single(), party("Gimmiegold"))!!.quantity)
    }
    @Test fun `late first inventory keeps floor pickups separate from silent starting food`() {
        val run = SafariRun(0)
        run.resources.pickup("FLOOR DROP! Shining Coin")
        run.inventoryEvidence(listOf("Shining Coin" to 3, "Yogi Berry" to 2), 10000)
        assertEquals(2, run.resources.startingCount(FloorDropType.COIN))
        assertEquals(2, run.resources.startingCount(FloorDropType.BERRY))
        assertEquals(11, run.birds.feedingTarget)
        run.inventoryEvidence(listOf("Yogi Berry" to 20), 13001)
        assertEquals(2, run.resources.startingCount(FloorDropType.BERRY), "later holdings are not new grants")
    }
    @Test fun `one clear trigger batches all eligible items with independent discovery gates`() {
        for ((needs, expected) in listOf(
            party("Gimmiegold", "Macaw") to listOf(FloorDropType.COIN, FloorDropType.SEEDS),
            party("Macaw") to listOf(FloorDropType.SEEDS),
            party("Gimmiegold") to listOf(FloorDropType.COIN),
            party() to emptyList())) {
            val run = SafariRun(0)
            run.inventoryEvidence(listOf("Shining Coin" to 1, "Bag of Seeds" to 1), 0)
            val output = mutableListOf<StartingResourceReminder>()
            val delivery = run.resourceReminders
            assertFalse(delivery.dispatch(0, "Local", SafariBiome.CAVERN, needs) { _, r -> output.addAll(r.items) })
            delivery.afterBiomeClear(0)
            assertFalse(delivery.dispatch(3999, "Local", SafariBiome.CAVERN, needs) { _, r -> output.addAll(r.items) })
            for (now in listOf(4000L, 4001L, 7999L, 8000L, 12000L))
                delivery.dispatch(now, "Local", SafariBiome.CAVERN, needs) { _, r -> output.addAll(r.items) }
            assertEquals(expected.toSet(), output.map { it.type }.toSet())
            assertEquals(expected.size, output.size)
            assertTrue(output.all { it.quantity == 1 && it.missing.size == 1 })
            assertEquals(FloorDropType.COIN in expected, run.resources.pending(SafariBiome.HAUNTED))
            assertEquals(FloorDropType.SEEDS in expected, run.resources.pending(SafariBiome.FOREST))
        }
    }
    @Test fun `temporary destination or discovery suppression does not lose an unsent reminder`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Shining Coin" to 1, "Yogi Berry" to 1), 0)
        val output = mutableListOf<StartingResourceReminder>()
        val delivery = run.resourceReminders
        delivery.afterBiomeClear(0)
        assertTrue(delivery.dispatch(4000, "Local", SafariBiome.HAUNTED, party("Gimmiegold", "Macaw")) { _, r -> output.addAll(r.items) })
        assertEquals(listOf(FloorDropType.BERRY), output.map { it.type })
        assertFalse(run.resources.pending(SafariBiome.HAUNTED))
        assertFalse(delivery.dispatch(8000, "Local", SafariBiome.CAVERN, party("Macaw")) { _, r -> output.addAll(r.items) })
        assertTrue(delivery.dispatch(9000, "Local", SafariBiome.CAVERN, party("Gimmiegold", "Macaw")) { _, r -> output.addAll(r.items) })
        assertEquals(listOf(FloorDropType.BERRY, FloorDropType.COIN), output.map { it.type })
        assertFalse(SafariRun(1).resourceReminders.dispatch(12000, "Local", SafariBiome.CAVERN, party("Macaw")) { _, r -> output.addAll(r.items) })
    }
    @Test fun `dispatch rechecks quantities and only delivered items create independent spawn work`() {
        val run = SafariRun(0)
        val needs = party("Gimmiegold", "Bluebird", "Parakeet")
        run.inventoryEvidence(listOf("Shining Coin" to 2, "Bag of Seeds" to 2, "Yogi Berry" to 1), 0)
        val outgoing = mutableListOf<String>()
        val receiver = SafariResources()
        receiver.scan(SafariBiome.FOREST, 9)
        receiver.scan(SafariBiome.HAUNTED, 3)
        run.resourceReminders.afterBiomeClear(0)
        // One coin is consumed and its quantity must change before dispatch.
        run.inventoryEvidence(listOf("Shining Coin" to 1, "Bag of Seeds" to 2, "Yogi Berry" to 1), 5000)
        for (now in listOf(5000L, 9000L, 13000L, 17000L)) {
            run.resourceReminders.dispatch(now, "Local", SafariBiome.CAVERN, needs) { text, _ ->
                outgoing.add(text); assertTrue(receiver.partyNotice("Party > Local: $text"))
            }
        }
        assertEquals(1, outgoing.size, "all eligible starting items share one party message")
        val reminders = outgoing.flatMap { StartingResourceNotice.parse(it, needs)!!.items }
        assertEquals(mapOf(FloorDropType.COIN to 1, FloorDropType.SEEDS to 2, FloorDropType.BERRY to 1), reminders.associate { it.type to it.quantity })
        assertEquals(4, receiver.requiredPopulation(SafariBiome.HAUNTED))
        assertEquals(12, receiver.requiredPopulation(SafariBiome.FOREST))
        receiver.scan(SafariBiome.HAUNTED, 3); receiver.scan(SafariBiome.FOREST, 9)
        assertTrue(receiver.pending(SafariBiome.HAUNTED)); assertTrue(receiver.pending(SafariBiome.FOREST))
        receiver.spawn(SafariBiome.HAUNTED)
        assertFalse(receiver.pending(SafariBiome.HAUNTED)); assertTrue(receiver.pending(SafariBiome.FOREST))
        repeat(2) { receiver.spawn(SafariBiome.FOREST) }
        assertTrue(receiver.pending(SafariBiome.FOREST), "two food types need all three uses")
        receiver.spawn(SafariBiome.FOREST)
        assertFalse(receiver.pending(SafariBiome.FOREST))
        outgoing.forEach { receiver.partyNotice("Party > Local: $it") }
        assertFalse(receiver.pending(SafariBiome.FOREST), "repeat notices do not create more work")
    }
    @Test fun `using one held item before dispatch suppresses only that reminder`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Shining Coin" to 1, "Wriggleworm" to 1), 0)
        run.resourceReminders.afterBiomeClear(0)
        run.inventoryEvidence(listOf("Wriggleworm" to 1), 5000)
        val output = mutableListOf<StartingResourceReminder>()
        assertTrue(run.resourceReminders.dispatch(5000, "Local", SafariBiome.CAVERN, party("Gimmiegold", "Macaw")) { _, r -> output.addAll(r.items) })
        assertEquals(listOf(FloorDropType.WORM), output.map { it.type })
        assertFalse(run.resources.pending(SafariBiome.HAUNTED))
        assertFalse(run.resourceReminders.dispatch(9000, "Local", SafariBiome.CAVERN, party("Gimmiegold", "Macaw")) { _, r -> output.addAll(r.items) })
    }

    @Test fun `late sampled food does not count a reported floor pickup as another starting grant`() {
        val run = SafariRun(0)
        run.resources.pickup("FLOOR DROP! Yogi Berry")
        run.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.CAVERN, 10000)
        run.inventoryEvidence(listOf("Yogi Berry" to 3), 10000)
        assertEquals(2, run.resources.startingCount(FloorDropType.BERRY))
        assertEquals(12, run.birds.feedingTarget, "two starting foods plus one floor pickup")
        run.inventoryEvidence(listOf("Yogi Berry" to 3), 11000)
        assertEquals(12, run.birds.feedingTarget)
    }

    @Test fun `paced food notices preserve partial spawn credit between deliveries`() {
        val resources = SafariResources()
        resources.scan(SafariBiome.FOREST, 9)
        resources.partyNotice("Party > Local: [SM] Unused starting item: 2x Bag of Seeds -> Forest")
        resources.spawn(SafariBiome.FOREST)
        resources.partyNotice("Party > Local: [SM] Unused starting item: 1x Yogi Berry -> Forest")
        resources.scan(SafariBiome.FOREST, 9)
        assertTrue(resources.pending(SafariBiome.FOREST))
        resources.spawn(SafariBiome.FOREST)
        assertTrue(resources.pending(SafariBiome.FOREST))
        resources.spawn(SafariBiome.FOREST)
        assertFalse(resources.pending(SafariBiome.FOREST), "all three reported foods have spawned birds")
        assertEquals(12, resources.requiredPopulation(SafariBiome.FOREST))
    }
    @Test fun `first destination scan of the baseline population cannot discharge an extra resource`() {
        for ((type, biome, baseline) in listOf(Triple("Yogi Berry", SafariBiome.FOREST, 9),
            Triple("Shining Coin", SafariBiome.HAUNTED, 3))) {
            val resources = SafariResources()
            resources.partyNotice("Party > Local: [SM] Unused starting item: 1x $type -> ${biome.label}")
            resources.scan(biome, baseline)
            assertTrue(resources.pending(biome), "$type needs an extra spawn beyond ordinary baseline")
            resources.scan(biome, baseline + 1)
            assertFalse(resources.pending(biome))
        }
    }

    @Test fun `local clearing still delivers supplies when a prior party report suppresses our done message`() {
        val run = SafariRun(0)
        val needs = party("Gimmiegold")
        run.visitBiome(SafariBiome.CAVERN)
        run.inventoryEvidence(listOf("Shining Coin" to 1), 0)
        run.partyFullClear("Party > Friend: cd")
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        assertFalse(run.completion.locallyComplete(run, SafariBiome.CAVERN, SafariMode.UNIQUE, needs))
        SafariBiome.CAVERN.critters.forEach { run.record(SafariCatch(it, false)) }
        run.moundSurvey.scan(emptySet()) { true }
        run.observeCavernSources(false, false, true)
        assertNull(run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.UNIQUE, needs))
        assertTrue(run.completion.locallyComplete(run, SafariBiome.CAVERN, SafariMode.UNIQUE, needs))
        // Runtime polls the local clear every half-second: this must not postpone delivery forever.
        for (now in 0L..4000L step 500)
            if (run.completion.locallyComplete(run, SafariBiome.CAVERN, SafariMode.UNIQUE, needs))
                run.resourceReminders.afterBiomeClear(now)
        assertTrue(run.resourceReminders.dispatch(4000, "Local", SafariBiome.CAVERN, needs) { text, _ ->
            assertEquals("[SM] Unused floor drops: 1x Shining Coin (missing Sparkling Gimmiegold)", text)
        })
        assertFalse(run.resourceReminders.dispatch(8000, "Local", SafariBiome.CAVERN, needs) { _, _ -> fail("duplicate") })
    }

}
