package net.johnceo.sparklingmutuals.history

import java.util.Locale
import kotlin.math.abs

/** Nearest-nine median estimate used by the supplied run backfiller. */
object HistoryEstimates {
    fun coins(run: RunRecord, reference: List<RunRecord>): Long {
        val similar = reference.filter { it.coins > 0 && it.caught >= 0 }.sortedBy {
            abs(it.caught - run.caught) * 1.5 + abs(it.lootShares - run.lootShares) * .65 +
                abs(it.party - run.party) * .5 + abs(it.sparklings.size - run.sparklings.size) * 10 + abs(it.feathers - run.feathers) * 3
        }.take(9).map { it.coins }.sorted()
        return similar.getOrNull(similar.size / 2) ?: 0
    }
}

/** Preserves existing entries and the caller's live counter; matches existing catches within ten seconds. */
object SparklingHistoryMerge {
    private fun key(entry: SparklingEntry) = entry.player.trim().lowercase(Locale.ROOT) + ":" +
        entry.critter.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    fun merge(entries: MutableList<SparklingEntry>, found: List<SparklingEntry>, ticketTimes: List<Long>): Int {
        data class Candidate(val found: Int, val saved: SparklingEntry, val distance: Long)
        val groups = entries.groupBy(::key).mapValues { (_, values) -> values.sortedBy { it.at } }
        val candidates = mutableListOf<Candidate>()
        found.forEachIndexed { index, entry ->
            val existing = groups[key(entry)].orEmpty()
            var lo = 0; var hi = existing.size
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (existing[mid].at < entry.at - 10000) lo = mid + 1 else hi = mid }
            while (lo < existing.size && existing[lo].at <= entry.at + 10000) {
                val saved = existing[lo++]
                candidates += Candidate(index, saved, abs(saved.at - entry.at))
            }
        }
        val matchedFound = mutableSetOf<Int>()
        val matchedSaved = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<SparklingEntry, Boolean>())
        candidates.sortedBy { it.distance }.forEach {
            if (it.found !in matchedFound && it.saved !in matchedSaved) { matchedFound += it.found; matchedSaved += it.saved }
        }
        val added = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<SparklingEntry, Boolean>())
        val latestAdded = mutableMapOf<String, Long>()
        found.withIndex().sortedBy { it.value.at }.forEach { (index, entry) ->
            if (index in matchedFound || entry.at <= 0 || entry.player.isBlank() || entry.critter.isBlank()) return@forEach
            if (latestAdded[key(entry)]?.let { entry.at - it <= 1500 } == true) return@forEach
            entries += entry; added += entry; latestAdded[key(entry)] = entry.at
        }
        val tickets = ticketTimes.sorted(); var ti = 0; var previous = 0
        val seen = mutableSetOf<String>()
        entries.sortBy { it.at }
        entries.forEach { entry ->
            while (ti < tickets.size && tickets[ti] <= entry.at) ti++
            val species = entry.critter.lowercase(Locale.ROOT)
            if (entry in added) {
                if (ticketTimes.isNotEmpty()) entry.tickets = ti - previous
                entry.unique = species !in seen
                entry.uniqueEstimated = true
            }
            previous = ti; seen += species
        }
        entries.sortByDescending { it.at }
        return added.size
    }
}
