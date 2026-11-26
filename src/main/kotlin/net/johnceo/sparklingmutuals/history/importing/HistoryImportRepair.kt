package net.johnceo.sparklingmutuals.history.importing

import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.SparklingEntry

/** Repairs only records positively identified by scanned logs. Absence from a log is never deletion evidence. */
object HistoryImportRepair {
    private fun same(a: SparklingEntry, b: SparklingEntry) = (a.ownerUuid == null || b.ownerUuid == null || a.ownerUuid == b.ownerUuid) &&
        a.player.equals(b.player, true) &&
        a.critter.equals(b.critter, true) && a.at > 0 && b.at > 0 && abs(a.at - b.at) <= 10_000

    fun sparklings(saved: MutableList<SparklingEntry>, found: List<SparklingEntry>, alpha: List<SparklingEntry>,
                   foreign: List<SparklingEntry> = emptyList()): Int {
        val before = saved.size
        // A verified main-server copy wins over conflicting metadata in an overlapping export.
        saved.removeAll { old ->
            (alpha.any { same(old, it) } || foreign.any { same(old, it) && old.logFile == it.logFile }) &&
                found.none { same(old, it) }
        }
        val retained = Collections.newSetFromMap(IdentityHashMap<SparklingEntry, Boolean>())
        // One-to-one matching preserves two genuine same-species catches when both are present in logs.
        found.sortedBy { it.at }.forEach { event ->
            saved.filter { it !in retained && same(it, event) }.minByOrNull { abs(it.at - event.at) }?.let { retained += it }
        }
        saved.removeAll { old -> old !in retained && found.any { same(old, it) } }
        return before - saved.size
    }

    private fun sameRun(a: RunRecord, b: RunRecord): Boolean =
        (a.ownerUuid == null || b.ownerUuid == null || a.ownerUuid == b.ownerUuid) &&
        listOf(a.start, a.ticketAt).filter { it > 0 }.any { at ->
            listOf(b.start, b.ticketAt).any { it > 0 && abs(at - it) <= 1500 }
        }

    private fun runIndex(runs: List<RunRecord>) = runs.flatMap { run ->
        listOf(run.start, run.ticketAt).filter { it > 0 }.distinct().map { it / 1_000 to run }
    }.groupBy({ it.first }, { it.second })

    private fun nearby(run: RunRecord, index: Map<Long, List<RunRecord>>) = sequenceOf(run.start, run.ticketAt)
        .filter { it > 0 }.flatMap { anchor ->
            ((anchor - 1_500) / 1_000..(anchor + 1_500) / 1_000).asSequence().flatMap { index[it].orEmpty().asSequence() }
        }.distinct().filter { sameRun(run, it) }

    fun runs(saved: MutableList<RunRecord>, found: List<RunRecord>, alpha: List<RunRecord>,
             foreign: List<RunRecord> = emptyList()): Int {
        val before = saved.size
        val valid = runIndex(found); val invalidRealm = runIndex(alpha); val invalidOwner = runIndex(foreign)
        saved.removeAll { old -> old.imported && (nearby(old, invalidRealm).any() ||
            old.importSource != null && nearby(old, invalidOwner).any { it.importSource == old.importSource }) &&
            nearby(old, valid).none() }
        val index = runIndex(saved)
        val retained = Collections.newSetFromMap(IdentityHashMap<RunRecord, Boolean>())
        found.sortedBy { it.start }.forEach { event ->
            nearby(event, index).filter { it !in retained }
                .minWithOrNull(compareBy<RunRecord> { it.imported }.thenBy { abs(it.start - event.start) })
                ?.let { retained += it }
        }
        saved.removeAll { old -> old.imported && old !in retained && nearby(old, valid).any() }
        return before - saved.size
    }
}
