package net.johnceo.sparklingmutuals.history.importing

import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.SparklingEntry

/** Repairs only records positively identified by scanned logs. Absence from a log is never deletion evidence. */
object HistoryImportRepair {
    private fun same(a: SparklingEntry, b: SparklingEntry) = a.player.equals(b.player, true) &&
        a.critter.equals(b.critter, true) && a.at > 0 && b.at > 0 && abs(a.at - b.at) <= 10_000

    fun sparklings(saved: MutableList<SparklingEntry>, found: List<SparklingEntry>, alpha: List<SparklingEntry>): Int {
        val before = saved.size
        // A verified main-server copy wins over conflicting metadata in an overlapping export.
        saved.removeAll { old -> alpha.any { same(old, it) } && found.none { same(old, it) } }
        val retained = Collections.newSetFromMap(IdentityHashMap<SparklingEntry, Boolean>())
        // One-to-one matching preserves two genuine same-species catches when both are present in logs.
        found.sortedBy { it.at }.forEach { event ->
            saved.filter { it !in retained && same(it, event) }.minByOrNull { abs(it.at - event.at) }?.let { retained += it }
        }
        saved.removeAll { old -> old !in retained && found.any { same(old, it) } }
        return before - saved.size
    }

    private fun sameRun(a: RunRecord, b: RunRecord): Boolean =
        listOf(a.start, a.ticketAt).filter { it > 0 }.any { at ->
            listOf(b.start, b.ticketAt).any { it > 0 && abs(at - it) <= 1500 }
        }

    fun runs(saved: MutableList<RunRecord>, found: List<RunRecord>, alpha: List<RunRecord>): Int {
        val before = saved.size
        saved.removeAll { old -> alpha.any { sameRun(old, it) } && found.none { sameRun(old, it) } }
        val retained = Collections.newSetFromMap(IdentityHashMap<RunRecord, Boolean>())
        found.sortedBy { it.start }.forEach { event ->
            saved.filter { it !in retained && sameRun(it, event) }
                .minWithOrNull(compareBy<RunRecord> { it.imported }.thenBy { abs(it.start - event.start) })
                ?.let { retained += it }
        }
        saved.removeAll { old -> old !in retained && found.any { sameRun(old, it) } }
        return before - saved.size
    }
}
