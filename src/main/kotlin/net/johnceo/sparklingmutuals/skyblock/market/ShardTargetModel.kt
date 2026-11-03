package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.DAY
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.HOUR
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.WEEK
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.percentile
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.median
import kotlin.math.ceil

/** A versioned heuristic, not a fill-time or financial-return forecast. */
internal data class ModelParameters(val quantile: Double=.6, val hoursWeight: Double=.6, val dayWeight: Double=.3, val weekWeight: Double=.1) {
    init {
        require(quantile.isFinite() && quantile in 0.0..1.0)
        require(listOf(hoursWeight,dayWeight,weekWeight).all {it.isFinite() && it>=0})
        require(kotlin.math.abs(hoursWeight+dayWeight+weekWeight-1)<.000001)
    }
}

internal object ShardTargetModel {
    const val VERSION=1
    fun target(history: HistorySnapshot, now: Long, parameters: ModelParameters=ModelParameters()): TargetResult {
        val fine=history.fine.filter { it.at<=now }
        fun window(millis: Long)=fine.filter { it.at>now-millis }
        val hours=window(3*HOUR)
        val day=window(DAY)
        val six=window(6*HOUR)
        val week=history.week.filter { it.at>now-WEEK && it.at<=now }
        if (fine.isNotEmpty() && now-fine.last().at>900_000)
            return TargetResult(null,false,UnknownReason.STALE_HISTORY)
        if (hours.size<ceil(36*.8) || day.size<ceil(288*.8) || six.size<ceil(72*.8) || week.size<ceil(84*.8))
            return TargetResult(null,false,UnknownReason.HISTORY_COVERAGE)
        val raw=parameters.hoursWeight*percentile(hours.map {it.ask},parameters.quantile)+
            parameters.dayWeight*percentile(day.map {it.ask},parameters.quantile)+parameters.weekWeight*percentile(week.map {it.ask},parameters.quantile)
        val recent=six.map { it.ask }
        val bounded=raw.coerceIn(percentile(recent,.1),percentile(recent,.9))
        val medians=(2 downTo 0).map { offset ->
            val end=now/900_000*900_000-offset*900_000
            val points=fine.filter { it.at>end-900_000 && it.at<=end }
            if(points.size<3) return@map null else median(points.map { it.ask })
        }
        val falling=medians.all { it!=null } && medians[0]!!>medians[1]!! && medians[1]!!>medians[2]!! && medians[2]!!<=medians[0]!!*.99
        val latest=window(1_800_000).map { it.ask }
        return TargetResult(if(falling)minOf(bounded,median(latest))else bounded,falling,null)
    }

    fun observe(previous: AdviceLatch?, quote: MarketQuote?, target: TargetResult, now: Long, eligible: Boolean): AdviceLatch {
        val unknown=when {
            !eligible -> UnknownReason.UNVALIDATED_MODEL
            quote==null || !quote.freshAt(now) || !quote.ask.isFinite() || quote.ask<=0 || quote.bid<=0 || quote.bid>quote.ask -> UnknownReason.STALE_QUOTE
            target.price==null -> target.unknown ?: UnknownReason.HISTORY_COVERAGE
            else -> null
        }
        if(unknown!=null) return AdviceLatch(AdviceState.UNKNOWN,null,unknown)
        quote!!
        val price=if(target.falling)quote.ask else target.price!!
        if(target.falling || quote.ask>=price*.995)
            return AdviceLatch(AdviceState.SELL,price,null,quote.serverAt)
        if(previous?.state!=AdviceState.SELL)
            return AdviceLatch(AdviceState.HOLD,price,null,quote.serverAt)
        if(quote.serverAt<previous.serverAt) return previous
        if(quote.ask>=price*.985) return AdviceLatch(AdviceState.SELL,price,null,quote.serverAt)
        val since=previous.belowSince ?: quote.serverAt
        val confirmed=quote.serverAt>previous.serverAt && quote.serverAt-since>=60_000
        return AdviceLatch(if(confirmed)AdviceState.HOLD else AdviceState.SELL,price,null,quote.serverAt,if(confirmed)null else since)
    }
}
