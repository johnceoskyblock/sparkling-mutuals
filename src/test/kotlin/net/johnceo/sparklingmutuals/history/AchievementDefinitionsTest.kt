package net.johnceo.sparklingmutuals.history

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path
import net.johnceo.sparklingmutuals.history.achievements.AchievementPersistence

class AchievementDefinitionsTest {
    @Test fun `shipped awards cover the approved mapping without archived criteria`() {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }.first { it.resolve("docs/issues/assets/achievement-rarity/achievement-mapping-v2.csv").toFile().exists() }
        val rows = root.resolve("docs/issues/assets/achievement-rarity/achievement-mapping-v2.csv").toFile().readLines().drop(1).map { it.split(',').take(6) }
        val current = rows.filter { !it[3].startsWith("legacy:") }
        assertEquals(current.map { it[3] }.toSet(), Achievements.all().map { it.id }.toSet())
        assertEquals(64, Achievements.all().size)
        for (row in current) {
            val award = Achievements.all().single { it.id == row[3] }
            assertEquals(row[5], award.tier.name, award.id)
            if (row[4].all(Char::isDigit)) assertEquals(row[4].toLong(), award.goal, award.id)
        }
    }

    @Test fun `modeled boundaries and unknown coverage cannot award or draw a zero bar`() {
        for ((suffix, threshold) in listOf("250" to 5.050676829380, "500" to 10.101353658760, "1000" to 20.202707317519)) {
            for (prefix in listOf("dry_model_v2_", "unique_dry_model_v2_")) {
                val a = Achievements.all().single { it.id == prefix + suffix }
                assertEquals(threshold, a.modeledHazardGoal)
                assertFalse(AchievementProgress(Math.nextDown(threshold), threshold, "modeled hazard", "recorded", true).reached)
                assertTrue(AchievementProgress(threshold, threshold, "modeled hazard", "recorded", true).reached)
                val unknown = AchievementProgress(null, threshold, "modeled hazard", "Unknown coverage", true)
                assertFalse(unknown.reached)
                val row = AchievementPresentation.current(a, unknown, null)
                assertNull(row.fraction)
                assertTrue(row.detail.contains("Unknown"))
                val modeled = AchievementPresentation.current(a, AchievementProgress(threshold / 2, threshold, "modeled hazard", "Recorded since 100", true), null)
                assertEquals(.5, modeled.fraction)
                assertTrue(modeled.detail.contains("modeled hazard"))
                assertTrue(modeled.progressLabel.contains("modeled full runs"))
            }
        }
    }
    @Test fun `current and legacy presentation preserve dates secret policy and distinct totals`() {
        val a = Achievements.all().single { it.id == "instant_v2" }
        val p = AchievementProgress(null, 1.0, "event", "Unknown", false)
        assertEquals("???", AchievementPresentation.current(a, p, null).name)
        val earned = AchievementPresentation.current(a, p, 123L)
        assertEquals("Instant Gratification", earned.name)
        assertEquals(123L, earned.unlockedAt)
        assertNull(earned.fraction)
        val legacy = net.johnceo.sparklingmutuals.history.achievements.LegacyAward("instant", 456, "Original criterion", "EPIC", "Replaced")
        val row = AchievementPresentation.legacy(legacy)
        assertTrue(row.name.contains("Legacy"))
        assertEquals(456L, row.unlockedAt)
        assertTrue(row.description.contains("Original criterion"))
        assertTrue(row.detail.contains("EPIC"))
        assertEquals(0, AchievementPresentation.totals(Achievements.all(), mapOf("instant" to 456L), listOf(legacy)).currentEarned)
        assertEquals(1, AchievementPresentation.totals(Achievements.all(), emptyMap(), listOf(legacy)).legacyEarned)
    }
    @Test fun `novelty has no prestige fanfare and workload families increase explicitly`() {
        assertFalse(AchievementTier.SPECIAL.fanfare)
        assertFalse(AchievementTier.SPECIAL.title)
        assertTrue(AchievementTier.SPECIAL.displayRank <= AchievementTier.COMMON.displayRank)
        assertTrue(AchievementTier.MYTHIC.fanfare)
        assertTrue(AchievementTier.LEGENDARY.title)
        for (prefix in listOf("tickets_v2_", "contests_v2_", "sparkle_", "unique_", "party_caught_")) {
            val ranks = Achievements.all().filter { it.id.startsWith(prefix) && it.modeledHazardGoal == null }.sortedBy { it.goal }.map { it.tier.displayRank }
            assertEquals(ranks.sorted(), ranks)
        }
    }

    @Test fun `facade records admissions with notifications off and preserves unknown profile coverage`() {
        isolated {
            val scope = net.johnceo.sparklingmutuals.history.achievements.AchievementScope("test-player", "one")
            val snapshot = OwnStats::class.java.getDeclaredField("dexSnapshot").apply { isAccessible = true }
            val old = snapshot.get(null)
            try {
                snapshot.set(null, net.johnceo.sparklingmutuals.history.achievements.DexSnapshot(scope, emptySet(), 100))
                val a = Achievements.all().single { it.id == "tickets_v2_10" }
                assertNull(Achievements.progressState(a).value)
                net.johnceo.sparklingmutuals.config.ConfigManager.achievements = false
                assertTrue(Achievements.collectAdmission("r1", scope, 100))
                assertFalse(Achievements.collectAdmission("r1", scope, 101))
                assertEquals(1.0, Achievements.progressState(a).value)
                assertEquals("personal tickets", Achievements.progressState(a).unit)
                assertTrue(Achievements.progressState(a).coverage.contains("lower bound"))
                val saved = AchievementPersistence.load().getOrThrow()
                assertEquals(1, saved.state!!.activity.admissions(scope))
                snapshot.set(null, net.johnceo.sparklingmutuals.history.achievements.DexSnapshot(scope.copy(profileId = "other"), emptySet(), 102))
                assertNull(Achievements.progressState(a).value)
            } finally { snapshot.set(null, old) }
        }
    }
    @Test fun `instant award requires a known verified interval at the facade`() {
        isolated {
            val scope = net.johnceo.sparklingmutuals.history.achievements.AchievementScope("test-player", "one")
            val a = Achievements.all().single { it.id == "instant_v2" }
            Achievements.collectOwnSparkling(scope, 100, false)
            assertFalse(Achievements.isUnlocked(a))
            for (n in 1..6) Achievements.collectAdmission("long$n", scope, 100L + n)
            Achievements.collectOwnSparkling(scope, 200, false)
            assertFalse(Achievements.isUnlocked(a))
            for (n in 1..5) Achievements.collectAdmission("short$n", scope, 200L + n)
            Achievements.collectOwnSparkling(scope, 300, false)
            assertTrue(Achievements.isUnlocked(a))
        }
    }
    @Test fun `overlapping historical capture totals use maximum not addition`() {
        isolated {
            val field = RunHistory::class.java.getDeclaredField("data").apply { isAccessible = true }
            val caught = OwnStats::class.java.getDeclaredField("caught").apply { isAccessible = true }
            val previous = field.get(RunHistory); val previousCaught = caught.getLong(null)
            try {
                field.set(RunHistory, RunHistory.Data(runs = mutableListOf(RunRecord(caught = 10, lootShares = 20))))
                caught.setLong(null, 25)
                val a = Achievements.all().single { it.id == "party_caught_1000" }
                assertEquals(30.0, Achievements.progressState(a).value)
                caught.setLong(null, 40)
                assertEquals(40.0, Achievements.progressState(a).value)
            } finally { field.set(RunHistory, previous); caught.setLong(null, previousCaught) }
        }
    }
    @Test fun `legacy filter excludes current criteria and novelty sorts without ordinal prestige`() {
        assertFalse(AchievementPresentation.visible(AchievementFilter.LEGACY, true))
        assertFalse(AchievementPresentation.visible(AchievementFilter.LEGACY, false))
        assertFalse(AchievementPresentation.visible(AchievementFilter.LOCKED, true))
        assertFalse(AchievementPresentation.visible(AchievementFilter.UNLOCKED, false))
        assertTrue(AchievementPresentation.visible(AchievementFilter.ALL, false))
        val novelty = Achievements.all().single { it.id == "own_triple" }
        val divine = Achievements.all().single { it.id == "max_sparklings" }
        assertEquals(listOf(novelty, divine), listOf(divine, novelty).sortedWith(AchievementPresentation.rarityOrder))
    }
    private fun isolated(body: () -> Unit) {
        val field = Achievements::class.java.getDeclaredField("data").apply { isAccessible = true }
        val previous = field.get(Achievements)
        val directory = HistoryStore.directory
        val setting = net.johnceo.sparklingmutuals.config.ConfigManager.achievements
        try {
            field.set(Achievements, Achievements.Data(initialised = true))
            HistoryStore.directory = java.nio.file.Files.createTempDirectory("definition-test")
            net.johnceo.sparklingmutuals.config.ConfigManager.achievements = true
            body()
        } finally {
            field.set(Achievements, previous); HistoryStore.directory = directory
            net.johnceo.sparklingmutuals.config.ConfigManager.achievements = setting
        }
    }
}
