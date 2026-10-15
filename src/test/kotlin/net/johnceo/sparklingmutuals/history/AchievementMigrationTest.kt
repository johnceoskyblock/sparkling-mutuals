package net.johnceo.sparklingmutuals.history

import com.google.gson.Gson
import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.history.achievements.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class AchievementMigrationTest {
    @TempDir lateinit var directory: Path
    private val gson = Gson()
    // Catches dropping unknown fields/timestamps or treating old low thresholds as new evidence.
    @Test fun `legacy awards and raw payload survive without earning replacements`() {
        val raw = JsonParser.parseString("""{"initialised":true,"unlocked":{"dry_runs_10":123,"run_10":456,"alien":789},"emblems":{"old_emblem":777},"ticketsSinceOwnSparkling":42,"longestRunsWithoutUnique":999,"custom":{"history":[1,"two",null]}}""").asJsonObject
        val data = gson.fromJson(raw, Achievements.Data::class.java)
        val state = AchievementMigration.migrate(data, raw)
        assertEquals(mapOf("run_10" to 456L), state.currentUnlocks)
        assertEquals(123L, state.legacyAwards["dry_runs_10"]?.unlockedAt)
        assertEquals("UNCOMMON", state.legacyAwards["dry_runs_10"]?.originalTier)
        assertEquals("10 runs in a row without catching a sparkling.", state.legacyAwards["dry_runs_10"]?.originalGoal)
        assertEquals(789L, state.legacyAwards["alien"]?.unlockedAt)
        assertEquals(777L, state.legacyAwards["old_emblem"]?.unlockedAt)
        assertEquals(raw, state.originalPayload)
        assertTrue(state.droughtByScope.isEmpty())
        assertNull(state.activity.ticketsSinceOwnSparkling(AchievementScope("p", "s")))
        assertEquals(gson.toJson(state), gson.toJson(AchievementMigration.migrate(data, raw)))
    }
    // Catches replacing the durable event object with derived totals, losing dedup/unknown scope.
    @Test fun `migration retains complete recorded activity across reload`() {
        val data = Achievements.Data()
        val scope = AchievementScope("p", "s")
        data.activity!!.recordUnscopedAdmission("unknown", 1)
        data.activity!!.recordContest("p", 10, null, 2)
        data.activity!!.recordOwnSparkling(scope, 3)
        data.activity!!.recordAdmission(scope, "known", 4)
        val migrated = AchievementMigration.migrate(data)
        val restored = gson.fromJson(gson.toJson(migrated), AchievementStateV2::class.java)
        assertFalse(restored.activity.recordAdmission(scope, "unknown", 5))
        assertFalse(restored.activity.recordContest("p", 9, scope, 6))
        assertEquals(1, restored.activity.ticketsSinceOwnSparkling(scope))
        assertEquals(1, restored.activity.unscopedAdmissions)
    }
    // Catches writes before backup, overwriting first backup, and raw fields lost on actual disk loading.
    @Test fun `disk migration backs up original bytes once and is idempotent`() {
        withStore {
            val source = """{ "unlocked": {"run_10":123}, "custom": [3,2,1], "contestsCompleted":90 }"""
            Files.writeString(file(), source)
            val data = AchievementPersistence.load().getOrThrow()
            AchievementPersistence.save(data).getOrThrow()
            val first = Files.readString(file())
            assertEquals(source, Files.readString(file().resolveSibling(file().fileName.toString() + ".pre-v2.bak")))
            val loaded = AchievementPersistence.load().getOrThrow()
            AchievementPersistence.save(loaded).getOrThrow()
            assertEquals(first, Files.readString(file()))
            assertEquals(90, loaded.contestsCompleted)
            assertEquals("[3,2,1]", loaded.state!!.originalPayload["custom"].toString())
        }
    }
    // Catches default-data save after a bad or future source destroys the recoverable source.
    @Test fun `malformed and unsupported source blocks all achievement writes`() {
        for (source in listOf("{broken", "null", "[]", "{\"state\":{\"schemaVersion\":99}}", "{\"unlocked\":null}")) {
            withStore {
                Files.writeString(file(), source)
                assertTrue(AchievementPersistence.load().isFailure)
                assertTrue(AchievementPersistence.save(Achievements.Data()).isFailure)
                assertTrue(HistoryStore.save("achievements", Achievements.Data()).isFailure)
                assertEquals(source, Files.readString(file()))
            }
        }
    }
    // Catches accepting an unusable backup target then overwriting the only original file.
    @Test fun `backup failure leaves source protected`() {
        withStore {
            val source = "{\"unlocked\":{\"dry_runs_10\":10}}"
            Files.writeString(file(), source)
            Files.createDirectory(file().resolveSibling(file().fileName.toString() + ".pre-v2.bak"))
            assertTrue(AchievementPersistence.load().isFailure)
            assertTrue(AchievementPersistence.save(Achievements.Data()).isFailure)
            assertEquals(source, Files.readString(file()))
        }
    }
    @Test fun `retiered macaw keeps timestamp while contest and instant become legacy`() {
        val old = Achievements.Data(unlocked = linkedMapOf("no_macaw_3" to 11, "contests_10" to 22, "instant" to 33))
        val migrated = AchievementMigration.migrate(old)
        assertEquals(mapOf("no_macaw_3" to 11L), migrated.currentUnlocks)
        assertEquals(22L, migrated.legacyAwards["contests_10"]?.unlockedAt)
        assertEquals(33L, migrated.legacyAwards["instant"]?.unlockedAt)
    }
    // Calls the real quiet importer twice. Any announcement would touch the absent game client and fail.
    @Test fun `quiet import cannot recreate replaced awards and preserves original snapshot`() {
        withStore {
            val raw = JsonParser.parseString("""{"initialised":true,"unlocked":{"dry_runs_10":123},"longestRunsWithoutSparkling":10000,"contestsCompleted":10000,"custom":"kept"}""").asJsonObject
            val old = gson.fromJson(raw, Achievements.Data::class.java)
            AchievementMigration.migrate(old, raw)
            val field = Achievements::class.java.getDeclaredField("data").also { it.isAccessible = true }
            val prior = field.get(Achievements)
            field.set(Achievements, old)
            try {
                Achievements.afterImport()
                val once = gson.toJson(Achievements.state)
                assertEquals(0, Achievements.afterImport())
                assertEquals(once, gson.toJson(Achievements.state))
                assertEquals(raw, Achievements.state.originalPayload)
                assertFalse(Achievements.state.currentUnlocks.keys.any { it in LegacyAchievementMetadata.replacedIds })
                assertTrue(Achievements.state.droughtByScope.isEmpty())
            } finally { field.set(Achievements, prior) }
        }
    }
    @Test fun `all tier names remain lossless in preserved award records`() {
        for (tier in AchievementTier.entries) {
            val award = LegacyAward("x", 123, "old goal", tier.name, "old")
            assertEquals(tier.name, gson.fromJson(gson.toJson(award), LegacyAward::class.java).originalTier)
        }
    }
    private fun file() = HistoryStore.directory!!.resolve("sparkling-mutuals-achievements.json")
    private fun withStore(block: () -> Unit) {
        val prior = HistoryStore.directory
        HistoryStore.directory = Files.createTempDirectory(directory, "case")
        try { block() } finally { HistoryStore.directory = prior }
    }
}
