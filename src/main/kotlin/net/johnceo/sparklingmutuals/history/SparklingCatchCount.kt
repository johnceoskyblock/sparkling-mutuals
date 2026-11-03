package net.johnceo.sparklingmutuals.history

/** Party catch numbers. Existing history is the only source; never fetch on catch. */
object SparklingCatchCount {
    fun number(entry: SparklingEntry, history: List<SparklingEntry>) = history.count {
        it.at <= entry.at && it.critter.equals(entry.critter,true)
    }
    fun text(entry: SparklingEntry, history: List<SparklingEntry>): String {
        val suffix=if(entry.unique) "NEW!" else "#${number(entry,history).coerceAtLeast(1)}"
        return "Sparkling ${entry.critter} caught! $suffix"
    }
}
