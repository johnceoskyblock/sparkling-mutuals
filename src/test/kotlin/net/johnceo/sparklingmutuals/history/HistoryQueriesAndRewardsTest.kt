package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.history.importing.HistoryEstimates
import net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HistoryQueriesAndRewardsTest : ProfitableEspPriceFixture() {
    @Test fun `Bests luck counts party sparklings and verified Macaw misses without bridging imports or unknown spawns`() {
        val runs = listOf(
            RunRecord(start = 1, macawChecked = true), RunRecord(start = 2, imported = true),
            RunRecord(start = 3, macawChecked = true), RunRecord(start = 4, ownSparklings = 1, macaws = 1),
            RunRecord(start = 5, sparklings = mutableListOf("Friend:Fluffling"), macawChecked = true),
            RunRecord(start = 6, imported = true), RunRecord(start = 7, macawChecked = true))
        val luck = HistoryQueries.liveLuck(runs)
        assertEquals(5, luck.runs)
        assertEquals(1, luck.sparklingDryStreak)
        assertEquals(1, luck.macawDrought)
        assertEquals(5, luck.macawRuns)
        assertEquals(0, HistoryQueries.liveLuck(runs.filter { it.imported }).runs)
        val partial = listOf(RunRecord(start = 1, macawChecked = true), RunRecord(start = 2),
            RunRecord(start = 3, macawChecked = false), RunRecord(start = 4, macawChecked = true))
        assertEquals(1, HistoryQueries.liveLuck(partial).macawDrought)
        assertEquals(0, HistoryQueries.liveLuck(listOf(RunRecord())).macawRuns)
        assertEquals(0, HistoryQueries.liveLuck(listOf(RunRecord(catches=linkedMapOf("Macaw" to 1)))).macawDrought)
    }
    @Test fun `37th type time survives later captures long runs and reimport enrichment`() {
        val tally = RunTally()
        val names = net.johnceo.sparklingmutuals.safari.SafariRoster.all.map { it.name }
        names.forEachIndexed { i, name -> tally.line("CAPTURE! You caught a $name and gained 1x $name Shard!", "Me", 1000000L + i * 1000) }
        assertEquals(1036000L, tally.allTypesAt)
        tally.line("CAPTURE! You caught a Foxtrot and gained 1x Foxtrot Shard!", "Me", 1900000)
        assertEquals(1036000L, tally.allTypesAt)
        val saved = RunRecord(start=1000000,party=37,length=900000)
        val found = RunRecord(start=1000000,party=37,length=900000,allTypesMillis=36000,imported=true)
        assertEquals(0,HistoryMerge.merge(mutableListOf(saved),listOf(found)))
        assertEquals(36000L,HistoryQueries.allTypesTime(saved))
        assertNull(HistoryQueries.allTypesTime(RunRecord(party=36,allTypesMillis=50000)))
        assertNull(HistoryQueries.allTypesTime(RunRecord(party=37)))
        assertEquals(120000L,HistoryQueries.allTypesTime(RunRecord(party=37,length=120000)))
    }
    @Test fun `party catches and party discoveries reset achievement streaks while imports break them`() {
        val runs = listOf(RunRecord(start=1000,length=100),RunRecord(start=2000,length=100),
            RunRecord(start=3000,length=100,sparklings=mutableListOf("Friend:Foxtrot")),
            RunRecord(start=4000,length=100),RunRecord(start=5000,length=100,imported=true),RunRecord(start=6000,length=100))
        val found = listOf(SparklingEntry(at=3050,player="Friend",critter="Foxtrot",unique=true))
        val result = HistoryQueries.partyStreaks(runs,found)
        assertEquals(HistoryQueries.PartyStreaks(1,2,1,2),result)
        assertEquals(2,HistoryQueries.liveLuck(runs).sparklingDryStreak)
    }
    @Test fun `reimport upgrades an old imported row and subsequent identical rescans report no changes`() {
        val old = RunRecord(start = 1000000, imported = true, modeEstimated = true, coins = 0, coinsEstimated = true)
        val saved = mutableListOf(old)
        val richer = RunRecord(start = 1000000, imported = true, mode = "UNIQUE", modeEstimated = false,
            caught = 2, catches = linkedMapOf("Foxtrot" to 2), shards = linkedMapOf("Foxtrot" to 3),
            safariEssence = 350, summaryShards = 3, huntingXp = 1000, hotspot = "Forest",
            splits = linkedMapOf("Forest" to 60000), coins = 300, coinsEstimated = false)
        var updated = 0
        assertEquals(0, HistoryMerge.merge(saved, listOf(richer)) { updated++ })
        assertEquals(1, updated); assertSame(old, saved.single())
        assertEquals(350, old.safariEssence); assertEquals(2, old.caught)
        assertEquals("UNIQUE", old.mode); assertEquals(300L, old.coins)
        updated = 0
        assertEquals(0, HistoryMerge.merge(saved, listOf(richer)) { updated++ })
        assertEquals(0, updated)
        richer.splits["Cavern"] = 70000
        assertEquals(0, HistoryMerge.merge(saved, listOf(richer)) { updated++ })
        assertEquals(1, updated); assertEquals(70000L, old.splits["Cavern"])
    }
    @Test fun `average excludes AFK and unknown durations without dropping runs from other stats`() {
        val runs = listOf(0L, 60000L, 120000L, 600000L, 600001L).map { RunRecord(length = it) }
        assertEquals(90000L, HistoryQueries.averageLength(runs))
        assertNull(HistoryQueries.averageLength(listOf(RunRecord(length = 900000))))
        assertEquals(5, runs.size)
    }
    @Test fun `duration cleanup removes old overlong rows and rejects them on every reimport`() {
        val kept = RunRecord(start = 1000000, length = 1800000)
        val overlong = RunRecord(start = 4000000, length = 1800001, imported = true)
        val unknown = RunRecord(start = 7000000, length = 0, imported = true)
        val runs = mutableListOf(kept, overlong, unknown)
        assertEquals(1, HistoryMerge.removeOverlongRuns(runs))
        assertEquals(listOf(unknown, kept), runs)
        assertEquals(listOf(2, 1), runs.map { it.number })
        assertEquals(0, HistoryMerge.merge(runs, listOf(overlong)))
        assertEquals(0, HistoryMerge.merge(runs, listOf(overlong)))
        assertEquals(2, runs.size)
        assertTrue(HistoryQueries.keepRunLength(1800000))
        assertFalse(HistoryQueries.keepRunLength(1800001))
    }
    @Test fun `run navigation finds the index without filtering and species search remains case insensitive`() {
        val run = RunRecord(number = 123)
        val runs = listOf(RunRecord(number = 124), run, RunRecord(number = 122))
        assertEquals(1, HistoryQueries.runIndex(runs, " #123 "))
        assertEquals(-1, HistoryQueries.runIndex(runs, ""))
        assertEquals(-1, HistoryQueries.runIndex(runs, "12"))
        assertEquals(-1, HistoryQueries.runIndex(runs, "not a number"))
        assertEquals(3, runs.size)
        val catch = SparklingEntry(critter = "Fluffling")
        assertTrue(HistoryQueries.matchesCritter(catch, " fluf "))
        assertFalse(HistoryQueries.matchesCritter(catch, "Macaw"))
    }
    @Test fun `scrollbar drags map track extremes and preserve the pointer grab position`() {
        val track = net.johnceo.sparklingmutuals.history.ui.HistoryScrollTrack(50, 250, 20, 1000)
        assertTrue(track.scrollable)
        assertEquals(0, track.offsetAt(-100.0, 5.0))
        assertEquals(980, track.offsetAt(500.0, 5.0))
        assertEquals(490, track.offsetAt(150.0, 5.0))
        assertEquals(50, track.thumbTop(0))
        assertEquals(250 - track.thumbHeight, track.thumbTop(980))
        val short = net.johnceo.sparklingmutuals.history.ui.HistoryScrollTrack(50, 250, 20, 5)
        assertFalse(short.scrollable)
        assertEquals(0, short.offsetAt(150.0, 5.0))
    }
    @Test fun `summary essence enriches a saved run once and repeat imports preserve native profit`() {
        prices(mapOf(BazaarPrices.SAFARI_ESSENCE to doubleArrayOf(10.0, 20.0)))
        val tally = RunTally()
        tally.line("+999 Safari Essence", "Me")
        assertEquals(-1, tally.safariEssence)
        listOf("SAFARI REWARD SUMMARY", "+350 Safari Essence", "+88 Shards", "+239,781 Hunting Exp").forEach { tally.line(it, "Me") }
        assertEquals(350, tally.safariEssence)
        val run = RunRecord(start = 1000000, coins = 100)
        val saved = mutableListOf(run)
        val found = RunRecord(start = 1000000, imported = true, safariEssence = 350, summaryShards = 88, huntingXp = 239781)
        assertEquals(0, HistoryMerge.merge(saved, listOf(found)))
        assertTrue(RunHistory.addEssenceValue(run))
        assertEquals(7100L, run.coins)
        assertEquals(7000L, run.essenceCoins)
        assertEquals(0, HistoryMerge.merge(saved, listOf(found)))
        assertFalse(RunHistory.addEssenceValue(run))
        assertEquals(7100L, run.coins)
        tally.line("+400 Safari Essence", "Me")
        assertTrue(RunHistory.applySummary(run, tally))
        assertEquals(8100L, run.coins)
        assertFalse(RunHistory.applySummary(run, tally))
    }
    @Test fun `complete logged item prices replace estimates while incomplete items keep a marked estimate`() {
        val quote = mapOf(BazaarPrices.shardId("Foxtrot") to doubleArrayOf(50.0, 100.0))
        val complete = RunRecord(shards = linkedMapOf("Foxtrot" to 2), summaryShards = 2, coinsEstimated = true)
        HistoryEstimates.assignCoins(complete, emptyList(), quote, false)
        assertEquals(200L, complete.coins); assertFalse(complete.coinsEstimated)
        val incomplete = RunRecord(shards = linkedMapOf("Foxtrot" to 2), summaryShards = 4)
        HistoryEstimates.assignCoins(incomplete, listOf(RunRecord(coins = 600, essenceCoins = 100)), quote, false)
        assertEquals(500L, incomplete.coins); assertTrue(incomplete.coinsEstimated)
    }
}
