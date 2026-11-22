package net.johnceo.sparklingmutuals.history

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal enum class ProfitTimeframe(val header: String) {
    DAY("Day"), SESSION("Session #"), WEEK("Week")
}

enum class SafariProfitPeriod(val label: String) {
    SESSION("Session"), LIFETIME("Lifetime"), WEEK("Week"), DAY("Day")
}

internal data class ProfitTotals(val runs: Int, val coins: Long, val millis: Long, val feathers: Int, val best: RunRecord?) {
    val perHour get() = if (millis > 0) coins * 3_600_000L / millis else 0L
    val perRun get() = if (runs > 0) coins / runs else 0L

    companion object {
        fun of(runs: List<RunRecord>) = ProfitTotals(runs.size, runs.sumOf { it.coins.coerceAtLeast(0) },
            runs.sumOf { it.length }, runs.sumOf { it.feathers }, runs.maxByOrNull { it.coins })
    }
}

internal data class SafariSessionStats(val runs: Int, val averageRunMillis: Long?, val sparklings: Long) {
    val runsPerSparkling get() = if (sparklings > 0) runs.toDouble() / sparklings else null
    val formattedRunsPerSparkling get() = runsPerSparkling?.let { "%.1f".format(Locale.ROOT, it) }

    companion object {
        fun of(runs: List<RunRecord>): SafariSessionStats {
            var timed = 0
            var duration = 0L
            var sparklings = 0L
            for (run in runs) {
                if (run.length > 0) {
                    timed++
                    duration += run.length
                }
                sparklings += run.sparklings.size
            }
            return SafariSessionStats(runs.size, if (timed > 0) duration / timed else null, sparklings)
        }
    }
}

internal object ProfitPeriods {
    data class Row(val totals: ProfitTotals, val date: LocalDate? = null, val session: Int? = null)

    fun rows(runs: List<RunRecord>, timeframe: ProfitTimeframe, sessionStarts: List<Long>,
             zone: ZoneId = ZoneId.systemDefault()): List<Row> {
        if (timeframe == ProfitTimeframe.SESSION) {
            val groups = runs.groupBy { run ->
                val index = sessionStarts.binarySearch(run.start)
                if (index < 0) -index - 1 else index + 1
            }
            return (sessionStarts.size downTo 0).map { Row(ProfitTotals.of(groups[it].orEmpty()), session = it) }
        }
        return runs.groupBy { run -> periodDate(run.start, timeframe == ProfitTimeframe.WEEK, zone) }
            .entries.sortedByDescending { it.key }.map { (date, group) -> Row(ProfitTotals.of(group), date = date) }
    }

    fun currentRuns(runs: List<RunRecord>, period: SafariProfitPeriod, sessionStart: Long?, now: Long,
                    zone: ZoneId = ZoneId.systemDefault()): List<RunRecord> {
        val start = when (period) {
            SafariProfitPeriod.LIFETIME -> return runs
            SafariProfitPeriod.SESSION -> sessionStart ?: return runs
            SafariProfitPeriod.DAY, SafariProfitPeriod.WEEK ->
                periodDate(now, period == SafariProfitPeriod.WEEK, zone).atStartOfDay(zone).toInstant().toEpochMilli()
        }
        return runs.filter { it.start in start..now }
    }

    private fun periodDate(at: Long, week: Boolean, zone: ZoneId): LocalDate {
        val date = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        return if (week) date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else date
    }
}
