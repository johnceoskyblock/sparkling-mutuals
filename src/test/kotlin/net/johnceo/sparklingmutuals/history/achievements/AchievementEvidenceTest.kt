package net.johnceo.sparklingmutuals.history.achievements

import com.google.gson.Gson
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AchievementEvidenceTest {
    private val scope = AchievementScope("AB-CD", "profile")
    private fun event(id: String = "run", missing: Set<String>? = setOf("Wumpa"),
        counts: Map<String, Int> = mapOf("Wumpa" to 1), own: Set<String> = emptySet(), complete: Boolean = true) =
        AchievementRunEvidence(id, scope, missing, counts, own, complete)

    @Test fun `same length streaks respect missing identities and personal exposure`() {
        val wumpa = DroughtEvidence(scope)
        val gazer = DroughtEvidence(scope)
        repeat(250) {
            wumpa.recordRun(event("$it"))
            gazer.recordRun(event("$it", setOf("Gazer"), mapOf("Gazer" to 4)))
        }
        assertEquals(.122100124527, wumpa.currentUniqueHazard, 1e-10)
        assertEquals(.488400498109, gazer.currentUniqueHazard, 1e-10)
        assertTrue(wumpa.currentUniqueHazard < 5.050676829)
        assertEquals(wumpa.currentUniqueHazard, wumpa.bestUniqueHazard)
    }
    @Test fun `exposure caps personal counts and excludes skipped negative and unknown species`() {
        val state = DroughtEvidence(scope)
        state.recordRun(event(counts = mapOf("Gazer" to 99, "Wumpa" to -2, "Unknown" to 999)))
        assertEquals(.0019536020, state.currentOrdinaryHazard, 1e-10)
        assertEquals(0.0, state.currentUniqueHazard)
    }
    @Test fun `unknown missing set and incomplete observations do not invent unique severity`() {
        val state = DroughtEvidence(scope)
        state.recordRun(event("known"))
        state.recordRun(event("gap", complete = false))
        assertEquals(0.0, state.currentUniqueHazard)
        assertEquals(0.0, state.currentOrdinaryHazard)
        state.recordRun(event("unknown", missing = null))
        assertEquals(0.0, state.currentUniqueHazard)
        assertEquals(.000488400498, state.currentOrdinaryHazard, 1e-12)
        assertEquals(.000488400498, state.bestUniqueHazard, 1e-12)
    }
    @Test fun `full dex stops unique exposure and success drops ambiguous partial run`() {
        val state = DroughtEvidence(scope)
        state.recordRun(event("dry"))
        state.recordRun(event("own", own = setOf("Wumpa")))
        assertEquals(0.0, state.currentOrdinaryHazard)
        assertEquals(0.0, state.currentUniqueHazard)
        state.recordRun(event("full", missing = emptySet()))
        assertEquals(0.0, state.currentUniqueHazard)
        assertEquals(.000488400498, state.bestUniqueHazard, 1e-12)
    }
    @Test fun `owned repeat resets ordinary but does not reset unique and replay is ignored`() {
        val state = DroughtEvidence(scope)
        assertTrue(state.recordRun(event("one"))) // no own success: party successes are not supplied here
        assertFalse(state.recordRun(event("one", own = setOf("Wumpa"))))
        state.recordRun(event("repeat", own = setOf("Gazer")))
        assertEquals(0.0, state.currentOrdinaryHazard)
        assertEquals(.000976800996, state.currentUniqueHazard, 1e-12)
        assertEquals(setOf("one", "repeat"), state.processedRunIds)
    }
    @Test fun `api additions invalidate continuation without rebuilding from current dex`() {
        val state = DroughtEvidence(scope)
        state.refreshDex(DexSnapshot(scope, emptySet(), 10))
        state.recordRun(event("one"))
        state.refreshDex(DexSnapshot(scope, setOf("Wumpa"), 20))
        assertEquals(0.0, state.currentUniqueHazard)
        assertEquals(.000488400498, state.bestUniqueHazard, 1e-12)
        assertEquals(10L, state.coverageStart)
        state.refreshDex(DexSnapshot(AchievementScope("other", "profile"), emptySet(), 30))
        assertEquals(scope.key, state.scope.key)
        state.invalidateScope()
        state.recordRun(event("uncertain"))
        assertEquals(0.0, state.currentUniqueHazard)
    }
    @Test fun `different scopes never mix and uuid formatting does not split a scope`() {
        val state = DroughtEvidence(scope)
        assertEquals("abcd:profile", scope.key)
        assertTrue(state.recordRun(event().copy(scope = AchievementScope("abcd", "profile"))))
        assertFalse(state.recordRun(event("other").copy(scope = AchievementScope("abcd", "second"))))
        assertEquals(.000488400498, state.currentUniqueHazard, 1e-12)
    }
    @Test fun `run snapshot is prospective and a midrun dex change invalidates unique input`() {
        val run = SafariRun(100)
        run.attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 90))
        assertTrue("Wumpa" in run.achievementEvidence("run", true)!!.missingBefore!!)
        run.refreshAchievementDex(DexSnapshot(scope, setOf("Wumpa"), 110))
        assertNull(run.achievementEvidence("run", true)!!.missingBefore)
        run.attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 120))
        assertNull(run.achievementEvidence("run", true)!!.missingBefore)
        assertNull(SafariRun(100).apply { attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 101)) }
            .achievementEvidence("late", true))
    }
    @Test fun `legacy run stays unknown through json and evidence survives round trip`() {
        val gson = Gson()
        assertNull(gson.fromJson("{\"caught\":999}", RunRecord::class.java).achievementEvidence)
        val record = RunRecord(achievementEvidence = event())
        val loaded = gson.fromJson(gson.toJson(record), RunRecord::class.java)
        assertEquals(event(), loaded.achievementEvidence)
    }
    @Test fun `confirmed own discovery cannot reenter missing set while api lags`() {
        val state = DroughtEvidence(scope)
        state.refreshDex(DexSnapshot(scope, emptySet(), 1))
        state.recordRun(event("own", own = setOf("Wumpa")))
        state.recordRun(event("stale", counts = mapOf("Wumpa" to 1)))
        assertEquals(0.0, state.currentUniqueHazard)
        state.recordRun(event("other", missing = setOf("Wumpa", "Gazer"), counts = mapOf("Gazer" to 4)))
        assertEquals(.001953601992, state.currentUniqueHazard, 1e-12)
        state.refreshDex(DexSnapshot(scope, setOf("Wumpa"), 2))
        assertEquals(.001953601992, state.currentUniqueHazard, 1e-12)
    }
    @Test fun `response owner provider and generation guard rejects stale callbacks`() {
        val guard = ProfileResponseGuard()
        val token = guard.begin("ab-cd", "cloud")
        assertTrue(guard.accepts(token, "abcd", "cloud"))
        assertFalse(guard.accepts(token, "other", "cloud"))
        assertFalse(guard.accepts(token, "abcd", "key"))
        guard.invalidate()
        assertFalse(guard.accepts(token, "abcd", "cloud"))
        val newer = guard.begin("abcd", "cloud")
        assertFalse(guard.accepts(token, "abcd", "cloud"))
        assertTrue(guard.accepts(newer, "abcd", "cloud"))
    }
}
