package net.johnceo.sparklingmutuals.skyblock.market

/** Descriptive references only. Approval is not evidence that any forecast passed replay. */
internal object ExperimentalRange {
    const val KEY="experimental-reference-range-v1"
    fun target(history: HistorySnapshot,quote: MarketQuote?,now: Long): TargetResult {
        if(quote==null || !quote.freshAt(now) || !quote.ask.isFinite() || !quote.bid.isFinite() ||
            quote.bid<=0 || quote.ask<quote.bid) return TargetResult(null,false,UnknownReason.STALE_QUOTE)
        val recent=history.fine.lastOrNull() ?: return TargetResult(null,false,UnknownReason.HISTORY_COVERAGE)
        if(now-recent.at>900_000)return TargetResult(null,false,UnknownReason.STALE_HISTORY)
        if(kotlin.math.abs(quote.ask/recent.ask-1)>.10)return TargetResult(null,false,UnknownReason.REFERENCE_MISMATCH)
        return ShardTargetModel.target(history,now)
    }
}
