package net.johnceo.sparklingmutuals.history

import java.util.Locale

/** A discovery belongs to this client's participating account, irrespective of who caught it. */
object SparklingUniqueness {
    fun isNew(entry: SparklingEntry, history: List<SparklingEntry>, localWasKnown: Boolean?) =
        localWasKnown == false && history.none {
            it !== entry && it.at <= entry.at && it.critter.equals(entry.critter, true)
        }

    /** Repair old catcher-based repeat flags and recompute first-seen estimates after older imports. */
    fun reconcile(history: List<SparklingEntry>): Boolean {
        val seen = mutableSetOf<String>()
        var changed = false
        history.sortedBy { it.at }.filter { it.at > 0 && it.critter.isNotBlank() }.forEach { entry ->
            val first = seen.add(entry.critter.trim().lowercase(Locale.ROOT))
            val unique = if (entry.uniqueEstimated) first else entry.unique && first
            if (entry.unique != unique) { entry.unique = unique; changed = true }
        }
        return changed
    }
}
