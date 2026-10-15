package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.history.achievements.*
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class AchievementIntegrationTest {
    private val scope = AchievementScope("integration-player", "one")
    private val ack = "[NPC] Safari Manager: Looks good to me. Have fun out there!"
    private val ackAgain = "[NPC] Safari Manager: I already saw your ticket, so you're free to go."

    @Test fun `entries without any Manager line leave unknown coverage at the next own success`() = isolated {
        ConfigManager.achievements = false
        TicketCounter.disconnect()
        try {
            Achievements.collectOwnSparkling(scope, 100, false)
            repeat(7) { n ->
                TicketCounter.confirmEntry(SafariRun(200L + n), scope, 200L + n)
                TicketCounter.leave()
            }
            Achievements.collectOwnSparkling(scope, 300, false)
            assertEquals(0, Achievements.activity.admissions(scope))
            assertFalse("instant_v2" in Achievements.state.eligibleEvents)
        } finally { TicketCounter.disconnect() }
    }

    @Test fun `unacknowledged entries and stale variants cannot persist instant eligibility through reload`() = isolated {
        ConfigManager.achievements = false
        TicketCounter.disconnect()
        try {
            Achievements.collectOwnSparkling(scope, 100, false)
            repeat(7) { n ->
                val at = 200L + n * 200000L
                val run = SafariRun(at)
                TicketCounter.confirmEntry(run, scope, at)
                TicketCounter.confirmEntry(run, scope, at + 119999)
                TicketCounter.acknowledge(ack, at + 120001)
                TicketCounter.acknowledge(ackAgain, at + 120002)
                TicketCounter.leave()
            }
            assertNull(Achievements.activity.ticketsSinceOwnSparkling(scope))
            dataField().set(null, AchievementPersistence.load().getOrThrow())
            Achievements.collectOwnSparkling(scope, 1500000, false)
            assertFalse("instant_v2" in Achievements.state.eligibleEvents)
            dataField().set(null, AchievementPersistence.load().getOrThrow())
            ConfigManager.achievements = true
            Achievements.afterImport()
            assertFalse("instant_v2" in Achievements.state.currentUnlocks)
        } finally { TicketCounter.disconnect() }
    }

    @Test fun `sixth entry before success cannot earn instant and late ack stays in its original interval`() = isolated {
        ConfigManager.achievements = false
        TicketCounter.disconnect()
        val session = RunHistory::class.java.getDeclaredField("session").apply { isAccessible = true }.get(null) as LiveHistorySession
        try {
            Achievements.collectOwnSparkling(scope, 100, false)
            repeat(5) { n ->
                val at = 200L + n * 10
                TicketCounter.acknowledge(ack, at)
                val run = SafariRun(at)
                TicketCounter.confirmEntry(run, scope, at)
                session.sync(run) {}
                TicketCounter.leave()
            }
            assertEquals(5, Achievements.activity.ticketsSinceOwnSparkling(scope))
            val sixth = SafariRun(300)
            TicketCounter.confirmEntry(sixth, scope, 300)
            session.sync(sixth) {}
            Achievements.collectOwnSparkling(scope, 310, false)
            assertFalse("instant_v2" in Achievements.state.eligibleEvents)
            dataField().set(null, AchievementPersistence.load().getOrThrow())
            TicketCounter.acknowledge(ack, 320)
            TicketCounter.acknowledge(ackAgain, 321)
            assertEquals(6, Achievements.activity.admissions(scope))
            assertEquals(0, Achievements.activity.ticketsSinceOwnSparkling(scope), "Late ack is for the entry before the baseline")
            dataField().set(null, AchievementPersistence.load().getOrThrow())
            TicketCounter.leave()
            TicketCounter.acknowledge(ack, 330)
            TicketCounter.confirmEntry(sixth, scope, 331)
            assertEquals(6, Achievements.activity.admissions(scope))
            assertEquals(0, Achievements.activity.ticketsSinceOwnSparkling(scope))
            ConfigManager.achievements = true
            Achievements.afterImport()
            assertFalse("instant_v2" in Achievements.state.currentUnlocks)
        } finally { TicketCounter.disconnect(); session.sync(null) {} }
    }

    @Test fun `ack after entry but before success restores known short interval and fans out once`() = isolated {
        ConfigManager.achievements = false
        TicketCounter.disconnect()
        val session = RunHistory::class.java.getDeclaredField("session").apply { isAccessible = true }.get(null) as LiveHistorySession
        session.sync(null) {}
        try {
            Achievements.collectOwnSparkling(scope, 100, false)
            val run = SafariRun(200)
            val before = SparklingLog.ticketsSinceSparkling
            TicketCounter.confirmEntry(run, scope, 200)
            session.sync(run) {}
            assertNull(Achievements.activity.ticketsSinceOwnSparkling(scope))
            TicketCounter.acknowledge(ack, 210)
            TicketCounter.acknowledge(ackAgain, 211)
            assertEquals(1, session.tickets)
            assertEquals(before + 1, SparklingLog.ticketsSinceSparkling)
            assertEquals(1, Achievements.activity.ticketsSinceOwnSparkling(scope))
            Achievements.collectOwnSparkling(scope, 220, false)
            assertEquals(220L, Achievements.state.eligibleEvents["instant_v2"])
        } finally { TicketCounter.disconnect(); session.sync(null) {} }
    }

    @Test fun `disconnect freezes own discovery before actual session finalization and lagged dex reload`() = isolated {
        ConfigManager.achievements = false
        val oldHistory = ConfigManager.runHistory
        ConfigManager.runHistory = false
        val ledger = SafariTracking.ledger
        val session = RunHistory::class.java.getDeclaredField("session").apply { isAccessible = true }.get(null) as LiveHistorySession
        session.sync(null) {}
        try {
            val snapshot = DexSnapshot(scope, emptySet(), 100)
            Achievements.refreshDex(snapshot)
            ledger.confirmEntry(200)
            val run = ledger.current!!
            run.attachAchievementSnapshot(snapshot)
            val line = "CAPTURE! You caught a SPARKLING Wumpa!"
            run.record(SafariCatch.parse(line)!!, 210, "Me")
            session.chat(run, line, "Me") {}
            OwnStats.reset()
            ledger.leave(220)
            session.sync(ledger.current) { RunHistory.completeRun(it, 1, 220) }
            dataField().set(null, AchievementPersistence.load().getOrThrow())
            Achievements.refreshDex(DexSnapshot(scope, emptySet(), 300))
            fun dry(at: Long, vararg species: String) {
                val next = SafariRun(at)
                next.attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 300))
                species.forEach { next.record(SafariCatch.parse("CAPTURE! You caught a $it!")!!, at + 1, "Me") }
                val record = RunRecord(start = at, achievementEvidence = next.achievementEvidence(next.runId, true))
                Achievements.onRunEnd(record)
                Achievements.onRunEnd(record)
            }
            val evidence = Achievements.state.droughtByScope.getValue(scope.key)
            assertEquals(0.0, evidence.bestOrdinaryHazard, "Interrupted run earns no exposure")
            dry(400, "Wumpa")
            assertEquals(0.0, evidence.currentUniqueHazard, "Known own discovery survives stale API")
            dry(500, "Wumpa", "Gazer")
            assertEquals(.000488400498, evidence.currentUniqueHazard, 1e-12)
            assertEquals(.000488400498, evidence.bestUniqueHazard, 1e-12)
        } finally { ledger.leave(600); session.sync(null) {}; ConfigManager.runHistory = oldHistory }
    }

    @Test fun `scope switch freezes only prior own successes and rejects later foreign captures`() = isolated {
        ConfigManager.achievements = false
        val ledger = SafariTracking.ledger
        try {
            val snapshot = DexSnapshot(scope, emptySet(), 100)
            Achievements.refreshDex(snapshot)
            ledger.confirmEntry(200)
            val run = ledger.current!!
            run.attachAchievementSnapshot(snapshot)
            run.record(SafariCatch.parse("CAPTURE! You caught a SPARKLING Wumpa!")!!, 210, "Me")
            Achievements.refreshDex(DexSnapshot(scope.copy(profileId = "other"), emptySet(), 220))
            run.record(SafariCatch.parse("CAPTURE! You caught a SPARKLING Gazer!")!!, 230, "Me")
            val record = RunRecord(start = 200, achievementEvidence = run.achievementEvidence(run.runId, true))
            Achievements.onRunEnd(record)
            ledger.leave(240)
            Achievements.refreshDex(DexSnapshot(scope, emptySet(), 300))
            Achievements.onRunEnd(record) // delayed replay after the old scope becomes active again
            val dry = SafariRun(400)
            dry.attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 300))
            for (name in listOf("Wumpa", "Gazer")) dry.record(SafariCatch.parse("CAPTURE! You caught a $name!")!!, 410, "Me")
            Achievements.onRunEnd(RunRecord(start = 400, achievementEvidence = dry.achievementEvidence(dry.runId, true)))
            assertEquals(.000488400498, Achievements.state.droughtByScope.getValue(scope.key).currentUniqueHazard, 1e-12,
                "Only Gazer remains missing in the old profile")
        } finally { ledger.leave(500) }
    }
    @Test fun `completed personal evidence persists with notifications disabled and replay cannot inflate`() = isolated {
        val snapshot = DexSnapshot(scope, emptySet(), 100)
        val run = SafariRun(200)
        run.attachAchievementSnapshot(snapshot)
        run.record(SafariCatch.parse("CAPTURE! You caught a Wumpa!")!!, 210, "Me")
        val record = RunRecord(start = 200, runId = run.runId, achievementEvidence = run.achievementEvidence(run.runId, true))
        ConfigManager.achievements = false
        Achievements.onRunEnd(record)
        val evidence = Achievements.state.droughtByScope[scope.key]
        assertNotNull(evidence, "Run-end must consume contemporaneous personal evidence")
        assertEquals(.000488400498, evidence!!.currentOrdinaryHazard, 1e-12)
        Achievements.onRunEnd(record)
        assertEquals(.000488400498, evidence.currentOrdinaryHazard, 1e-12)
        val loaded = AchievementPersistence.load().getOrThrow().state!!
        assertEquals(setOf(run.runId), loaded.droughtByScope.getValue(scope.key).processedRunIds)
        Achievements.onRunEnd(RunRecord(imported = true, caught = 999))
        assertEquals(.000488400498, evidence.currentOrdinaryHazard, 1e-12)
    }
    @Test fun `disabled instant eligibility survives reload and quietly catches up`() = isolated {
        ConfigManager.achievements = false
        Achievements.collectOwnSparkling(scope, 100, false)
        Achievements.collectAdmission("one", scope, 110)
        Achievements.collectOwnSparkling(scope, 120, false)
        val award = Achievements.all().single { it.id == "instant_v2" }
        assertFalse(Achievements.isUnlocked(award))
        dataField().set(null, AchievementPersistence.load().getOrThrow())
        ConfigManager.achievements = true
        Achievements.afterImport()
        assertTrue(Achievements.isUnlocked(award))
        val at = Achievements.unlockedAt(award)
        Achievements.afterImport()
        assertEquals(at, Achievements.unlockedAt(award))
    }
    @Test fun `entry snapshot refresh discovery scope switch and disconnect are prospective`() = isolated {
        ConfigManager.achievements = false
        val snapshotField = OwnStats::class.java.getDeclaredField("dexSnapshot").apply { isAccessible = true }
        val previous = snapshotField.get(null)
        val ledger = SafariTracking.ledger
        val attach = SafariTracking::class.java.getDeclaredMethod("entryEvidence", SafariRun::class.java, java.lang.Long.TYPE).apply { isAccessible = true }
        try {
            val first = DexSnapshot(scope, emptySet(), 100)
            snapshotField.set(null, first)
            Achievements.refreshDex(first)
            ledger.confirmEntry(200)
            val run = ledger.current!!
            attach.invoke(SafariTracking, run, 200L)
            run.record(SafariCatch.parse("CAPTURE! You caught a Wumpa!")!!, 210, "Me")
            assertEquals(setOf("Wumpa"), run.achievementEvidence(run.runId, true)!!.personalCounts.keys)
            assertTrue(run.achievementEvidence(run.runId, true)!!.missingBefore!!.contains("Wumpa"))
            Achievements.refreshDex(DexSnapshot(scope, setOf("Gazer"), 220))
            assertNull(run.achievementEvidence(run.runId, true)!!.missingBefore)
            assertTrue(run.achievementEvidence(run.runId, true)!!.provenanceComplete)
            Achievements.onRunEnd(RunRecord(achievementEvidence = run.achievementEvidence(run.runId, true)))
            assertEquals(.000488400498, Achievements.state.droughtByScope.getValue(scope.key).currentOrdinaryHazard, 1e-12)
            Achievements.refreshDex(DexSnapshot(scope.copy(profileId = "two"), emptySet(), 230))
            assertFalse(run.achievementEvidence(run.runId, true)!!.provenanceComplete)
            assertEquals(0.0, Achievements.state.droughtByScope.getValue(scope.key).currentOrdinaryHazard)
            OwnStats.reset()
            assertNull(OwnStats.dexSnapshot)
            assertEquals(0.0, Achievements.state.droughtByScope.getValue(scope.key).currentUniqueHazard)
            assertEquals(.000488400498, Achievements.state.droughtByScope.getValue(scope.key).bestOrdinaryHazard, 1e-12)
            val late = SafariRun(50)
            snapshotField.set(null, first)
            attach.invoke(SafariTracking, late, 240L)
            assertNull(late.achievementEvidence(late.runId, true))
        } finally { ledger.leave(300); snapshotField.set(null, previous) }
    }
    @Test fun `real run session records modeled evidence even when raw run history is disabled`() = isolated {
        val session = RunHistory::class.java.getDeclaredField("session").apply { isAccessible = true }.get(null) as LiveHistorySession
        val old = ConfigManager.runHistory
        val run = SafariRun(100)
        run.attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 90))
        val line = "CAPTURE! You caught a Wumpa!"
        run.record(SafariCatch.parse(line)!!, 110, "Me")
        try {
            session.chat(run, line, "Me") {}
            ConfigManager.runHistory = false
            ConfigManager.achievements = false
            RunHistory.completeRun(run, 4, 200)
            assertEquals(.000488400498, Achievements.state.droughtByScope[scope.key]?.currentOrdinaryHazard ?: 0.0, 1e-12)
            assertFalse(RunHistory.runs.any { it.runId == run.runId })
        } finally { session.sync(null) {}; ConfigManager.runHistory = old }
    }
    @Test fun `actual Miria sidebar tier upgrades feed collector exactly once`() = isolated {
        val tracker = net.johnceo.sparklingmutuals.contest.ContestTracker
        tracker.selectScope(scope.key)
        tracker.javaClass.getDeclaredField("windowKey").apply { isAccessible = true }.setLong(null, tracker.absoluteWindow(java.time.LocalDateTime.now()))
        tracker.javaClass.getDeclaredField("secondsIntoWindow").apply { isAccessible = true }.setInt(null, 30)
        ConfigManager.achievements = false
        try {
            for (tier in listOf("Uncommon", "Rare", "Epic", "Legendary", "Mythic", "Divine")) {
                tracker.observeSidebar(listOf("Miria's Contest", "$tier with 1234"))
                tracker.completedWindow()?.let { Achievements.collectContest(scope.playerUuid, it, scope, 100) }
                assertEquals(1, Achievements.activity.contests(scope), tier)
            }
            assertEquals(1, AchievementPersistence.load().getOrThrow().state!!.activity.contests(scope))
        } finally { tracker.selectScope(null) }
    }
    @Test fun `legacy migration live personal sequence historical imports and reload keep separate evidence`() = isolated {
        val raw = """{"initialised":true,"unlocked":{"run_10":123,"dry_runs_10":456,"unknown-old":789},"future":{"keep":true}}"""
        Files.writeString(HistoryStore.directory!!.resolve("sparkling-mutuals-achievements.json"), raw)
        dataField().set(null, AchievementPersistence.load().getOrThrow())
        ConfigManager.achievements = false
        Achievements.refreshDex(DexSnapshot(scope, emptySet(), 1000))
        assertEquals(123L, Achievements.state.currentUnlocks["run_10"])
        assertEquals(789L, Achievements.state.legacyAwards["unknown-old"]!!.unlockedAt)
        assertTrue(Achievements.state.originalPayload.getAsJsonObject("future")["keep"].asBoolean)
        val dry = SafariRun(1100)
        dry.attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 1000))
        dry.record(SafariCatch.parse("CAPTURE! You caught a Wumpa!")!!, 1101, "Me")
        dry.record(SafariCatch.parse("LOOT SHARE! You received a Gazer from Friend catching it!")!!, 1102, "Me")
        assertTrue(Achievements.collectAdmission(dry.runId, scope, 1100))
        assertFalse(Achievements.collectAdmission(dry.runId, scope, 1101))
        Achievements.onRunEnd(RunRecord(start = 1100, achievementEvidence = dry.achievementEvidence(dry.runId, true)))
        val evidence = Achievements.state.droughtByScope.getValue(scope.key)
        assertEquals(.000488400498, evidence.currentUniqueHazard, 1e-12)
        assertEquals(.000488400498, evidence.currentOrdinaryHazard, 1e-12)
        val own = SafariRun(1200)
        own.attachAchievementSnapshot(DexSnapshot(scope, emptySet(), 1000))
        own.record(SafariCatch.parse("CAPTURE! You caught a SPARKLING Wumpa!")!!, 1201, "Me")
        Achievements.collectOwnSparkling(scope, 1201, false)
        Achievements.onRunEnd(RunRecord(start = 1200, ownSparklings = 1, achievementEvidence = own.achievementEvidence(own.runId, true)))
        assertEquals(0.0, evidence.currentUniqueHazard)
        assertEquals(0.0, evidence.currentOrdinaryHazard)
        val historyField = RunHistory::class.java.getDeclaredField("data").apply { isAccessible = true }
        val history = historyField.get(null)
        try {
            historyField.set(null, RunHistory.Data())
            assertEquals(1, RunHistory.importRuns(listOf(RunRecord(start = 500, imported = true, caught = 9999, tickets = 999))))
            assertEquals(0, RunHistory.importRuns(listOf(RunRecord(start = 500, imported = true, caught = 9999, tickets = 999))))
            Achievements.afterImport()
            assertEquals(2, evidence.processedRunIds.size)
            assertEquals(1, Achievements.activity.admissions(scope))
        } finally { historyField.set(null, history) }
        Achievements.refreshDex(DexSnapshot(scope, setOf("Wumpa", "Gazer"), 1300))
        assertEquals(0.0, evidence.currentUniqueHazard)
        assertTrue(Achievements.collectContest(scope.playerUuid, 900, scope, 1400))
        assertFalse(Achievements.collectContest(scope.playerUuid, 900, scope, 1401))
        Achievements.save().getOrThrow()
        dataField().set(null, AchievementPersistence.load().getOrThrow())
        assertEquals(1, Achievements.activity.contests(scope))
        assertEquals(1, Achievements.activity.admissions(scope))
        assertEquals(123L, Achievements.state.currentUnlocks["run_10"])
        assertEquals(456L, Achievements.state.legacyAwards["dry_runs_10"]!!.unlockedAt)
        assertEquals(raw, Files.readString(HistoryStore.directory!!.resolve("sparkling-mutuals-achievements.json.pre-v2.bak")))
        assertEquals(.000488400498, Achievements.state.droughtByScope.getValue(scope.key).bestUniqueHazard, 1e-12)
        ConfigManager.achievements = true
        Achievements.afterImport()
        val before = Achievements.state.currentUnlocks.toMap()
        Achievements.afterImport()
        assertEquals(before, Achievements.state.currentUnlocks)
    }
    @Test fun `profile refresh and disconnect clear the current sidebar completion before collection`() = isolated {
        val tracker = net.johnceo.sparklingmutuals.contest.ContestTracker
        fun qualify() {
            tracker.javaClass.getDeclaredField("windowKey").apply { isAccessible = true }.setLong(null, tracker.absoluteWindow(java.time.LocalDateTime.now()))
            tracker.javaClass.getDeclaredField("secondsIntoWindow").apply { isAccessible = true }.setInt(null, 30)
            tracker.observeSidebar(listOf("Miria's Contest", "Uncommon with 400"))
            assertNotNull(tracker.completedWindow())
        }
        try {
            Achievements.refreshDex(DexSnapshot(scope, emptySet(), 100))
            qualify()
            Achievements.refreshDex(DexSnapshot(scope.copy(profileId = "other"), emptySet(), 110))
            assertNull(tracker.completedWindow(), "The old profile's sidebar completion must not cross the scope boundary")
            qualify()
            OwnStats.reset()
            assertNull(tracker.completedWindow())
        } finally { tracker.selectScope(null) }
    }
    private fun dataField() = Achievements::class.java.getDeclaredField("data").apply { isAccessible = true }
    private fun isolated(body: () -> Unit) {
        val field = dataField(); val previous = field.get(null)
        val directory = HistoryStore.directory; val enabled = ConfigManager.achievements
        try {
            HistoryStore.directory = Files.createTempDirectory("achievement-integration")
            field.set(null, Achievements.Data(initialised = true))
            body()
        } finally { field.set(null, previous); HistoryStore.directory = directory; ConfigManager.achievements = enabled }
    }
}

