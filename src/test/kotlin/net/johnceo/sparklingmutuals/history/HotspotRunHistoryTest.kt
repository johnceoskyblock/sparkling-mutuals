package net.johnceo.sparklingmutuals.history

import com.google.gson.Gson
import net.johnceo.sparklingmutuals.safari.SAFARI_REWARD_SUMMARY
import net.johnceo.sparklingmutuals.safari.SafariRunParty
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotspotRunHistoryTest {
    private val party = mapOf("local" to "Local", "teammate" to "Friend")
    private val otherParty = mapOf("local" to "Local", "other" to "Other")
    private fun completed(start: Long, roster: Map<String, String> = party) =
        RunRecord(start = start, partyMembers = roster, hotspotRun = true, fullPartyPresent = true, rewardSummarySeen = true)

    @Test fun `enabling hotspot during a run sticks after it is turned off`() {
        val run = SafariRunParty()
        run.observeHotspot(false)
        assertFalse(run.hotspotUsed)
        run.observeHotspot(true)
        run.observeHotspot(false)
        assertTrue(run.hotspotUsed)
    }

    @Test fun `the observed party survives roster refreshes and a teammate leaving before the summary`() {
        val run = SafariRunParty()
        run.observeRoster(party.keys, false) { party }
        assertFalse(run.presentTogether)
        run.observeRoster(party.keys, true) { mapOf("local" to "Local") }
        assertFalse(run.presentTogether)
        run.observeRoster(party.keys, true) { party }
        assertTrue(run.presentTogether)
        run.observeRoster(null, true) { emptyMap() }
        run.observeRoster(setOf("local"), true) { mapOf("local" to "Local") }
        assertEquals(party, run.members)
        assertTrue(run.presentTogether)
    }

    @Test fun `an expanded party replaces the saved roster once the new member is present`() {
        val run = SafariRunParty()
        val expanded = party + ("late" to "Late")
        run.observeRoster(party.keys, true) { party }
        run.observeRoster(expanded.keys, true) { party }
        assertEquals(party, run.members)
        run.observeRoster(expanded.keys, true) { expanded }
        assertEquals(expanded, run.members)
    }

    @Test fun `the persisted count matches UUIDs across modes and ignores legacy or unconfirmed runs`() {
        val gson = Gson()
        val legacy = gson.fromJson("""{"hotspot":"Forest","partySize":2}""", RunRecord::class.java)
        assertNull(legacy.hotspotRun)
        assertNull(legacy.partyMembers)
        val data = RunHistory.Data(runs = mutableListOf(
            RunRecord(mode = "UNIQUE", partyMembers = party, hotspotRun = true, fullPartyPresent = true, rewardSummarySeen = true),
            RunRecord(mode = "FULL_CLEAR", partyMembers = mapOf("teammate" to "Renamed", "local" to "Local"), hotspotRun = true,
                fullPartyPresent = true, rewardSummarySeen = true),
            RunRecord(partyMembers = party, hotspotRun = false, fullPartyPresent = true, rewardSummarySeen = true),
            RunRecord(partyMembers = party, hotspotRun = true, fullPartyPresent = false, rewardSummarySeen = true),
            RunRecord(partyMembers = party, hotspotRun = true, fullPartyPresent = true, rewardSummarySeen = false),
            RunRecord(partyMembers = mapOf("local" to "Local"), hotspotRun = true, fullPartyPresent = true, rewardSummarySeen = true),
            legacy,
        ))
        val restored = gson.fromJson(gson.toJson(data), RunHistory.Data::class.java)
        assertEquals(2, HistoryQueries.consecutiveHotspotRuns(restored.runs, party.keys))
    }

    @Test fun `another party breaks the historical streak even when its run had hotspot off`() {
        val runs = listOf(
            completed(50),
            completed(40).apply { hotspotRun = false },
            completed(30),
            completed(20, otherParty).apply { hotspotRun = false },
            completed(10),
        )
        assertEquals(2, HistoryQueries.consecutiveHotspotRuns(runs, party.keys))
        assertEquals(1, HistoryQueries.consecutiveHotspotRuns(runs, otherParty.keys, liveStart = 60))
    }

    @Test fun `a roster change between runs does not reset the count for the reformed party`() {
        val fullParty = party + mapOf("third" to "Third", "fourth" to "Fourth")
        val runs = listOf(completed(20, fullParty), completed(10, fullParty))
        assertEquals(2, HistoryQueries.consecutiveHotspotRuns(runs, fullParty.keys))
        assertEquals(0, HistoryQueries.consecutiveHotspotRuns(runs, party.keys))
        assertEquals(3, HistoryQueries.consecutiveHotspotRuns(runs, fullParty.keys, liveStart = 80))
    }

    @Test fun `old roster reset metadata does not cut off the restored history`() {
        val gson = Gson()
        val json = gson.toJsonTree(RunHistory.Data(runs = mutableListOf(completed(50), completed(10)))).asJsonObject
        json.add("hotspotPartyRoster", gson.toJsonTree(party.keys))
        json.addProperty("hotspotPartySince", 40)
        val restored = gson.fromJson(json, RunHistory.Data::class.java)
        assertEquals(2, HistoryQueries.consecutiveHotspotRuns(restored.runs, party.keys))
    }

    @Test fun `the live run is included once before and after it is saved`() {
        val oldRun = completed(10)
        val current = completed(20)
        assertEquals(1, HistoryQueries.consecutiveHotspotRuns(listOf(oldRun), party.keys))
        assertEquals(2, HistoryQueries.consecutiveHotspotRuns(listOf(oldRun), party.keys, liveStart = current.start))
        assertEquals(2, HistoryQueries.consecutiveHotspotRuns(listOf(current, oldRun), party.keys, liveStart = current.start))
        assertEquals(2, HistoryQueries.consecutiveHotspotRuns(listOf(current, oldRun), party.keys))
        assertEquals(0, HistoryQueries.consecutiveHotspotRuns(emptyList(), setOf("local"), liveStart = current.start))
    }

    @Test fun `a late reward summary makes the saved party run count only once`() {
        val run = RunRecord(partyMembers = party, hotspotRun = true, fullPartyPresent = true)
        val tally = RunTally()
        assertEquals(0, HistoryQueries.consecutiveHotspotRuns(listOf(run), party.keys))
        assertTrue(tally.summaryLine(SAFARI_REWARD_SUMMARY))
        assertTrue(RunHistory.applySummary(run, tally))
        assertFalse(RunHistory.applySummary(run, tally))
        assertEquals(1, HistoryQueries.consecutiveHotspotRuns(listOf(run), party.keys))
    }
}
