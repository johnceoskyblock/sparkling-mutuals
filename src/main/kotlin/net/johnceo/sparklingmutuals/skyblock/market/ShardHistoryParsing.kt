package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonParser
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.ceil

internal object ShardHistoryParsing {
    const val STEP = 300_000L
    const val HOUR = 3_600_000L
    const val DAY = 24 * HOUR
    const val WEEK = 7 * DAY
    const val COARSE = 2 * HOUR

    /** Provider UTC is independently verified; arbitrary local system timezones are never consulted. */
    fun parse(raw: String, now: Long, resolutionMillis: Long = STEP): List<PriceSample> {
        if (raw.length > 2 * 1024 * 1024 || resolutionMillis !in setOf(STEP,COARSE)) return emptyList()
        val values = runCatching { JsonParser.parseString(raw).asJsonArray }.getOrNull() ?: return emptyList()
        val parsed = values.mapNotNull { value -> runCatching {
            val record=value.asJsonObject
            val text=record.get("timestamp").asString
            val source=runCatching { Instant.parse(text) }.getOrElse {
                runCatching { java.time.OffsetDateTime.parse(text).toInstant() }.getOrElse {
                    LocalDateTime.parse(text).toInstant(ZoneOffset.UTC)
                }
            }.toEpochMilli()
            val ask=record.get("buy").asDouble
            val bid=record.get("sell").asDouble
            val close=(source/resolutionMillis+1)*resolutionMillis
            if (source<=0 || source>now+60_000 || close>now || !ask.isFinite() || !bid.isFinite() || bid<=0 || ask<bid)
                null else PriceSample(close,ask,resolutionMillis,source)
        }.getOrNull() }
        // Conflicting observations at the exact same source time are not resolved by JSON response order.
        val consistent=parsed.groupBy { it.observedAt }.values.mapNotNull { group -> group.first().takeIf { group.map { it.ask }.distinct().size==1 } }
        return consistent.groupBy { it.at }.values.map { it.maxBy { point -> point.observedAt } }.sortedBy { it.at }
    }

    fun normalize(samples: List<PriceSample>, now: Long, requireAvailability: Boolean=false): HistorySnapshot {
        val valid=samples.filter { it.at>now-8*DAY && it.at<=now && it.ask.isFinite() && it.ask>0 &&
            it.resolutionMillis in setOf(STEP,COARSE) && it.at%it.resolutionMillis==0L &&
            (!requireAvailability || it.availableAt?.let {available -> available>=it.observedAt && available<=now}==true) }
        fun unique(points: List<PriceSample>)=points.groupBy { it.at }.values.mapNotNull { group ->
            val newest=group.maxOf { it.observedAt }
            val current=group.filter { it.observedAt==newest }
            current.first().takeIf { current.map { it.ask }.distinct().size==1 }
        }.sortedBy { it.at }
        val fine=unique(valid.filter { it.resolutionMillis==STEP })
        val week=unique(valid.filter { it.resolutionMillis==COARSE }).associateBy { it.at }.toMutableMap()
        fine.groupBy { ((it.at-1)/COARSE+1)*COARSE }.forEach { (end, points) ->
            if(end<=now && points.size>=ceil(COARSE.toDouble()/STEP*.8).toInt()) {
                val verified=points.all {it.basis==PriceBasis.VERIFIED_BEST_ASK}
                val available=if(points.all {it.availableAt!=null})maxOf(end,points.maxOf {it.availableAt!!})else null
                week[end]=PriceSample(end,median(points.map {it.ask}),COARSE,end,
                    if(verified)PriceBasis.VERIFIED_BEST_ASK else PriceBasis.UNVERIFIED_REFERENCE,available)
            }
        }
        return HistorySnapshot(fine,week.values.sortedBy { it.at })
    }

    internal fun percentile(values: List<Double>, quantile: Double): Double {
        require(values.isNotEmpty())
        val sorted=values.sorted()
        return sorted[(ceil(sorted.size*quantile).toInt()-1).coerceIn(0,sorted.lastIndex)]
    }

    internal fun median(values: List<Double>): Double {
        require(values.isNotEmpty())
        val sorted=values.sorted()
        val middle=sorted.size/2
        return if(sorted.size%2==1)sorted[middle] else sorted[middle-1]/2+sorted[middle]/2
    }
}
