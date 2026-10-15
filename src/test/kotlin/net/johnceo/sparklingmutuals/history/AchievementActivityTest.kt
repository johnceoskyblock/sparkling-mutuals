package net.johnceo.sparklingmutuals.history

import com.google.gson.Gson
import net.johnceo.sparklingmutuals.history.achievements.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AchievementActivityTest {
    private val scope = AchievementScope("Player-UUID", "profile")
    @Test fun `contest progression reload and profile switching count one personal reward per absolute window`() {
        val activity = ActivityEvidence()
        for (tier in listOf("Uncommon", "Rare", "Epic", "Legendary", "Mythic", "Divine"))
            activity.recordContest("Player-UUID", 100, scope, 1000)
        assertEquals(1, activity.contests(scope))
        val saved = Gson().fromJson(Gson().toJson(activity), ActivityEvidence::class.java)
        assertFalse(saved.recordContest("playeruuid", 100, scope.copy(profileId = "other"), 2000))
        assertTrue(saved.recordContest("playeruuid", 101, scope, 3000))
        assertFalse(saved.recordContest("playeruuid", 99, scope, 3001))
        assertFalse(saved.recordContest("playeruuid", 99, scope, 3002))
        assertTrue(saved.recordContest("playeruuid", 102, scope, 4000))
        assertTrue(saved.recordContest("playeruuid", 103, scope, 5000))
        assertEquals(4, saved.contests(scope))
        assertEquals(5.0, saved.expectedRewardTickets(scope))
        assertEquals(0, saved.admissions(scope))
    }
    @Test fun `unscoped activity never becomes scoped retroactively`() {
        val activity = ActivityEvidence()
        assertTrue(activity.recordUnscopedAdmission("raw", 10))
        assertFalse(activity.recordAdmission(scope, "raw", 11))
        assertTrue(activity.recordContest("playeruuid", 100, null, 12))
        assertFalse(activity.recordContest("playeruuid", 100, scope, 13))
        assertEquals(0, activity.admissions(scope))
        assertEquals(0, activity.contests(scope))
        assertEquals(1, activity.unscopedAdmissions)
        assertFalse(activity.historicalCoverageComplete)
    }
    @Test fun `known own sparkling interval gates instant and survives reload with admission dedup`() {
        var activity = ActivityEvidence()
        assertTrue(activity.recordAdmission(scope, "first", 10))
        assertNull(activity.ticketsSinceOwnSparkling(scope))
        assertFalse(activity.recordOwnSparkling(scope, 20))
        for (n in 1..5) activity.recordAdmission(scope, "run$n", 20L + n)
        activity = Gson().fromJson(Gson().toJson(activity), ActivityEvidence::class.java)
        assertFalse(activity.recordAdmission(scope, "run5", 30))
        assertEquals(5, activity.ticketsSinceOwnSparkling(scope))
        assertTrue(activity.recordOwnSparkling(scope, 40))
        activity.invalidateIntervals()
        assertNull(activity.ticketsSinceOwnSparkling(scope))
        assertFalse(activity.recordOwnSparkling(scope, 50))
        for (n in 6..11) activity.recordAdmission(scope, "run$n", 50L + n)
        assertFalse(activity.recordOwnSparkling(scope, 70))
    }
    @Test fun `legacy data and totals remain untouched with unknown new coverage`() {
        val data = Gson().fromJson("{\"ticketsSinceOwnSparkling\":42,\"contestsCompleted\":90}", Achievements.Data::class.java)
        assertEquals(42, data.ticketsSinceOwnSparkling)
        assertEquals(90, data.contestsCompleted)
        assertEquals(0, requireNotNull(data.activity).admissions(scope))
        assertNull(requireNotNull(data.activity).ticketsSinceOwnSparkling(scope))
        assertFalse(requireNotNull(data.activity).historicalCoverageComplete)
    }
    @Test fun `runtime contest collection persists with achievement notifications disabled`() {
        val oldDirectory = HistoryStore.directory
        val oldSetting = net.johnceo.sparklingmutuals.config.ConfigManager.achievements
        val directory = java.nio.file.Files.createTempDirectory("activity-evidence")
        try {
            HistoryStore.directory = directory
            net.johnceo.sparklingmutuals.config.ConfigManager.achievements = false
            assertTrue(Achievements.collectContest("activity-test-player", 901, AchievementScope("activity-test-player", "profile"), 1000))
            assertFalse(Achievements.collectContest("activity-test-player", 901, AchievementScope("activity-test-player", "other"), 1001))
            val saved = HistoryStore.load("achievements", Achievements.Data::class.java) { Achievements.Data() }
            assertEquals(1, requireNotNull(saved.activity).contests(AchievementScope("activity-test-player", "profile")))
            assertFalse(requireNotNull(saved.activity).recordContest("activity-test-player", 901, null, 1002))
        } finally {
            HistoryStore.directory = oldDirectory
            net.johnceo.sparklingmutuals.config.ConfigManager.achievements = oldSetting
        }
    }}
