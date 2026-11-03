package net.johnceo.sparklingmutuals.skyblock.market

internal data class MarketScope(val accountId: String, val profileId: String)
internal data class MarketQuote(val productId: String, val ask: Double, val bid: Double, val serverAt: Long) {
    fun freshAt(now: Long): Boolean = serverAt > 0 && now >= serverAt - 60_000L && now - serverAt <= 180_000L
}

internal data class QuoteSnapshot(
    val serverAt: Long,
    val quotes: Map<String, MarketQuote>,
    /** Legacy weighted values, in the original instant-sell / sell-offer order. */
    val quickPrices: Map<String, DoubleArray>,
)

internal enum class PriceBasis { VERIFIED_BEST_ASK, UNVERIFIED_REFERENCE }
/** at is market bin close; actual delivery availability is a separate fact and may be unknown. */
internal data class PriceSample(val at: Long, val ask: Double, val resolutionMillis: Long, val observedAt: Long = at,
    val basis: PriceBasis = PriceBasis.UNVERIFIED_REFERENCE, val availableAt: Long? = null)
internal data class HistorySnapshot(val fine: List<PriceSample>, val week: List<PriceSample>)
internal enum class AdviceState { SELL, HOLD, UNKNOWN }
internal enum class UnknownReason { HISTORY_COVERAGE, STALE_HISTORY, STALE_QUOTE, UNVALIDATED_MODEL }
internal data class TargetResult(val price: Double?, val falling: Boolean, val unknown: UnknownReason?)
internal data class AdviceLatch(
    val state: AdviceState,
    val target: Double?,
    val unknown: UnknownReason?,
    val serverAt: Long = 0L,
    val belowSince: Long? = null,
)
internal data class Recommendation(
    val productId: String,
    val state: AdviceState,
    val target: Double?,
    val quoteAt: Long,
    val reason: String,
    val ask: Double? = null,
)
