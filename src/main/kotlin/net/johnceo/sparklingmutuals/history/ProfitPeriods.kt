package net.johnceo.sparklingmutuals.history

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal enum class ProfitTimeframe(val header: String) {
    DAY("Day"), SESSION("Session #"), WEEK("Week")
}

internal data class ProfitTotals(val runs: Int, val coins: Long, val millis: Long, val feathers: Int, val best: RunRecord?) {
    val perHour get() = if (millis > 0) coins * 3_600_000L / millis else 0L
    val perRun get() = if (runs > 0) coins / runs else 0L

    companion object {
        fun of(runs: List<RunRecord>) = ProfitTotals(runs.size, runs.sumOf { it.coins.coerceAtLeast(0) },
            runs.sumOf { it.length }, runs.sumOf { it.feathers }, runs.maxByOrNull { it.coins })
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
        return runs.groupBy { run ->
            val date = Instant.ofEpochMilli(run.start).atZone(zone).toLocalDate()
            if (timeframe == ProfitTimeframe.WEEK) date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else date
        }.entries.sortedByDescending { it.key }.map { (date, group) -> Row(ProfitTotals.of(group), date = date) }
    }
}
