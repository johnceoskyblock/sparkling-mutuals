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
