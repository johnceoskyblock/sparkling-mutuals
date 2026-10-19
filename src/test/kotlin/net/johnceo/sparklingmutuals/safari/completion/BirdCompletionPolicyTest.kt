package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BirdCompletionPolicyTest {
    private val names = listOf("Bluebird", "Parakeet", "Macaw")
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - names.toSet()))
    }
    private fun run(biome: SafariBiome = SafariBiome.FOREST) = SafariRun(0).apply {
        visitBiome(biome); inventoryEvidence(emptyList(), 10000); localPlayer("me")
        updateCaptureEvidence(biome, emptySet(), false)
    }
    private fun catch(run: SafariRun, name: String, n: Int = 1) {
        repeat(n) { run.record(SafariCatch(SafariRoster.named(name)!!, personal = false), 10000) }
    }
    private fun live(blue: Int = 0, para: Int = 0, macaws: Int = 0) =
        List(blue) { "Bluebird" } + List(para) { "Parakeet" } + List(macaws) { "Macaw" }

    @Test fun `chat messages and out of radius birds cannot establish spawns`() {
        val run = run()
        repeat(20) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        run.scanBirdSources(0, live(blue = 9), qualified = false)
        assertFalse(run.sparklingComplete("Bluebird", party()))
        run.scanBirdSources(0, live(blue = 9), qualified = true)
        assertTrue(run.sparklingComplete("Bluebird", party()))
        assertEquals(0, run.count("Bluebird"))
    }
    @Test fun `modes share spawn proof but require different capture evidence`() {
        val run = run(); val state = party()
        run.scanBirdSources(0, live(5, 2, 4))
        names.forEach { assertTrue(run.sparklingComplete(it, state)); assertFalse(run.uniqueComplete(it, state)) }
        names.forEach { catch(run, it) }
        run.scanBirdSources(0, live(4, 1, 3))
        names.forEach { assertTrue(run.uniqueComplete(it, state)); assertFalse(run.fullClearComplete(it)) }
        catch(run, "Bluebird", 4); catch(run, "Parakeet")
        run.scanBirdSources(0, live(macaws = 3))
        names.forEach { assertTrue(run.fullClearComplete(it), "one Macaw remains sufficient after all normal birds are caught") }
    }
    @Test fun `no Macaws needs a qualified complete population in Forest`() {
        val run = run(); val state = party()
        run.scanBirdSources(0, live(5, 3))
        assertFalse(run.uniqueComplete("Macaw", state))
        run.scanBirdSources(0, live(5, 4))
        assertTrue(run.uniqueComplete("Macaw", state)); assertEquals(0, run.count("Macaw"))
        assertFalse(run.uniqueComplete("Bluebird", state)); assertFalse(run.fullClearComplete("Macaw"))
        catch(run, "Bluebird", 5); catch(run, "Parakeet", 4)
        run.scanBirdSources(0)
        names.forEach { assertTrue(run.uniqueComplete(it, state)); assertTrue(run.fullClearComplete(it)) }
    }
    @Test fun `outside fd supplies spawn trust and only the missing Macaw Unique catch exception`() {
        val run = run(SafariBiome.CAVERN); val state = party()
        run.partyFullClear("Party > Friend: fd")
        names.forEach { assertTrue(run.sparklingComplete(it, state)); assertFalse(run.uniqueComplete(it, state)) }
        catch(run, "Bluebird"); catch(run, "Parakeet")
        names.forEach { assertTrue(run.uniqueComplete(it, state)); assertFalse(run.fullClearComplete(it)) }
        catch(run, "Bluebird", 7)
        names.forEach { assertTrue(run.fullClearComplete(it), "fd alone never supplies missing Full Clear captures") }
        assertFalse(run.uniqueComplete("Foxtrot", state), "fd cannot waive another Forest species catch")
        run.visitBiome(SafariBiome.FOREST)
        assertFalse(run.sparklingComplete("Bluebird", state), "entering Forest needs qualified local evidence")
    }
    @Test fun `a floor marker anywhere or food in inventory overrides fd and captures`() {
        val run = run(SafariBiome.ICY); val state = party()
        catch(run, "Bluebird", 8); catch(run, "Parakeet")
        run.partyFullClear("Party > Friend: fd")
        run.scanBirdSources(1, qualified = false)
        names.forEach { assertFalse(run.sparklingComplete(it, state)); assertFalse(run.uniqueComplete(it, state)); assertFalse(run.fullClearComplete(it)) }
        run.visitBiome(SafariBiome.HAUNTED)
        assertFalse(run.sparklingComplete("Macaw", state))
        run.scanBirdSources(0, qualified = false)
        assertTrue(run.sparklingComplete("Macaw", state))
        run.inventoryEvidence(listOf("Yogi Berry" to 1), 20000)
        assertFalse(run.fullClearComplete("Bluebird")); assertFalse(run.sparklingComplete("Macaw", state))
    }
    @Test fun `unused starting food notices add deduplicated expected spawns and invalidate older fd`() {
        val run = run(SafariBiome.CAVERN); val state = party()
        run.partyFullClear("Party > Friend: fd")
        repeat(3) { run.resources.partyNotice("Party > Other: [SM] Unused starting item: 2x Yogi Berry -> Forest (Other)") }
        assertFalse(run.sparklingComplete("Bluebird", state)); assertEquals(11, run.birds.feedingTarget)
        run.visitBiome(SafariBiome.FOREST)
        run.scanBirdSources(0, live(blue = 10)); assertFalse(run.sparklingComplete("Bluebird", state))
        run.scanBirdSources(0, live(blue = 11)); assertTrue(run.sparklingComplete("Bluebird", state))
    }
    @Test fun `fd needs six personally collected foods initial Forest clearing and no held food`() {
        val run = run(); val state = party()
        repeat(5) { run.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.FOREST, 20000) }
        run.scanBirdSources(0, live(blue = 9))
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.SPARKLING, state))
        run.recordBirdFood("FLOOR DROP! Wriggleworm", SafariBiome.FOREST, 20000)
        run.inventoryEvidence(listOf("Wriggleworm" to 1), 20000)
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.SPARKLING, state))
        run.inventoryEvidence(emptyList(), 20000)
        run.scanBirdSources(0, live(blue = 9))
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.SPARKLING, state))
        val late = run(SafariBiome.CAVERN)
        catch(late, "Bluebird")
        late.visitBiome(SafariBiome.FOREST); late.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        repeat(6) { late.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.FOREST, 20000) }
        late.scanBirdSources(0, live(blue = 9))
        assertTrue(late.sparklingComplete("Bluebird", state))
        assertFalse(late.canReportBiome(SafariBiome.FOREST))
        assertNull(late.completion.announcement(late, SafariBiome.FOREST, SafariMode.SPARKLING, state))
    }
    @Test fun `current captures and renders never accumulate escaped replacement UUIDs`() {
        val run = run()
        catch(run, "Bluebird", 5)
        repeat(20) { run.scanBirdSources(0, live(blue = 3)) }
        assertFalse(run.birdSpawnsComplete())
        run.scanBirdSources(0, live(blue = 4)); assertTrue(run.birdSpawnsComplete())
    }
    @Test fun `outside captures can account for extra food after an older fd without treating fd as captures`() {
        val run = run(SafariBiome.CAVERN)
        run.partyFullClear("Party > Friend: fd")
        run.resources.partyNotice("Party > Other: [SM] Use: 1x Yogi Berry")
        catch(run, "Bluebird", 9)
        assertFalse(run.sparklingComplete("Bluebird", party()))
        catch(run, "Bluebird")
        assertTrue(run.sparklingComplete("Bluebird", party()))
        assertTrue(run.fullClearComplete("Bluebird"))
    }
    @Test fun `a first Forest visit after bird catches is a late entrant even with six pickups`() {
        val run = SafariRun(0)
        catch(run, "Bluebird")
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(emptyList(), 10000)
        repeat(6) { run.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.FOREST, 20000) }
        run.scanBirdSources(0, live(blue = 8))
        assertTrue(run.birdSpawnsComplete())
        assertFalse(run.canReportBiome(SafariBiome.FOREST))
    }
    @Test fun `Forest clearer can report after an Icy detour once all mode requirements pass`() {
        val forest = SafariBiome.FOREST
        val state = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to emptySet()))
        }
        for (mode in SafariMode.entries) {
            val run = run(SafariBiome.ICY)
            run.visitBiome(forest)
            repeat(3) { for (food in listOf("Wriggleworm", "Yogi Berry", "Bag of Seeds")) {
                run.recordBirdFood("FLOOR DROP! You found $food on the ground!", forest, 20000)
            } }
            catch(run, "Bluebird", 5); catch(run, "Parakeet", 3); catch(run, "Macaw", 2)
            run.inventoryEvidence(emptyList(), 30000)
            run.scanBirdSources(0)
            run.updateCaptureEvidence(forest, emptySet(), false, playerX = 6.0, playerZ = 51.0)
            assertNull(run.completion.announcement(run, forest, mode, state), "remaining non-bird requirements must block $mode")
            forest.critters.filter { it.name !in names }.forEach { catch(run, it.name, SafariFullClear.minimum(it.name)) }
            run.updateNestEvidence(true, 3, 3, 0)
            run.updateCaptureEvidence(forest, emptySet(), false, allNestsChecked = true, playerX = 6.0, playerZ = 51.0)
            run.sparklingChecks.scan(forest, 6.0, 51.0, emptyList(), nestsChecked = true, birdsSpawned = true)
            assertEquals(forest.critters.size, run.completed(mode, forest.critters, state, localOnly = true), mode.name)
            assertTrue(run.canReportBiome(forest), mode.name)
            assertEquals("fd", run.completion.announcement(run, forest, mode, state), mode.name)
            run.completion.sent(forest)
            for (nextMode in SafariMode.entries) assertNull(run.completion.announcement(run, forest, nextMode, state), "fd is sent once across mode switches")
        }
    }
    @Test fun `earned bird completion survives travel and reentry without fd including absent Macaws`() {
        val run = run(); val state = party()
        catch(run, "Bluebird", 6); catch(run, "Parakeet", 3)
        run.scanBirdSources(0)
        for (destination in listOf(SafariBiome.FOREST, SafariBiome.CAVERN, SafariBiome.ICY, SafariBiome.FOREST)) {
            run.visitBiome(destination)
            run.scanBirdSources(0, qualified = false)
            for (mode in SafariMode.entries) {
                assertEquals(3, run.completed(mode, names.map { SafariRoster.named(it)!! }, state), "$mode in $destination")
            }
        }
        run.scanBirdSources(1, qualified = false)
        assertFalse(run.birdSpawnsComplete(), "new food evidence still blocks retained proof")
        run.scanBirdSources(0, qualified = false)
        assertTrue(run.birdSpawnsComplete())
        run.resources.partyNotice("Party > Other: [SM] Use: 1x Yogi Berry")
        assertFalse(run.birdSpawnsComplete(), "new demand cannot be waived by the old nine-bird proof")
        catch(run, "Bluebird")
        run.scanBirdSources(0)
        assertTrue(run.birdSpawnsComplete())
    }
    @Test fun `completed render proof survives leaving even before birds are captured`() {
        val run = run(); val state = party()
        run.scanBirdSources(0, live(blue = 9))
        run.visitBiome(SafariBiome.CAVERN)
        assertTrue(run.sparklingComplete("Bluebird", state))
        assertFalse(run.uniqueComplete("Bluebird", state))
        assertFalse(run.fullClearComplete("Bluebird"))
        run.visitBiome(SafariBiome.FOREST)
        run.scanBirdSources(0)
        assertTrue(run.sparklingComplete("Bluebird", state))
    }
}
