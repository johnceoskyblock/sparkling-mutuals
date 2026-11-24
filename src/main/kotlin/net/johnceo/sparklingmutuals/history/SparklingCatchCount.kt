package net.johnceo.sparklingmutuals.history

/** Per-species totals from saved party catch history. */
object SparklingCatchCount {
    fun number(entry: SparklingEntry, history: List<SparklingEntry>) = history.count {
        it.at <= entry.at && it.critter.equals(entry.critter,true)
    }
    fun text(entry: SparklingEntry, history: List<SparklingEntry>): String {
        val total = number(entry, history).coerceAtLeast(1)
        val newTag = if (entry.unique) " (NEW!)" else ""
        return "Sparkling ${entry.critter} caught! #$total$newTag"
    }
}
