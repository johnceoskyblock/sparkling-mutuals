package net.johnceo.sparklingmutuals.skyblock.market

/** Essence is market advice only, never shard ownership, rank or an order-watch identity. */
internal object SaleMarketProducts {
    const val ESSENCE="ESSENCE_SAFARI"
    val ids=SaleProducts.names.keys+ESSENCE
    fun bounded(ids: Set<String>)=ids.size<=38 && ids.count {it!=ESSENCE}<=37
    fun historyId(id: String)=id==ESSENCE || Regex("SHARD_[A-Z_]+").matches(id)
}
