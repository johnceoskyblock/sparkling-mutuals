package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.commands.ProfitableCritters

internal enum class ShardSaleAction(val label: String,val color: Int) {
    OFFER("SELL OFFER",0xFF55FF88.toInt()), SALVAGE("SALVAGE",0xFF55DDFF.toInt()),
    INSTANT("INSTA-SELL",0xFFFFCC66.toInt())
}
internal data class ShardSaleComparison(val action: ShardSaleAction,val offer: Double,val instant: Double,
    val salvage: Double,val weekly: Double?,val average: Double?,val reason: String) {
    val ratio get()=weekly?.let {week->average?.takeIf {it>0}?.let {week/it}}
    val recommended get()=when(action) {ShardSaleAction.OFFER->offer;ShardSaleAction.SALVAGE->salvage;ShardSaleAction.INSTANT->instant}
}

/** Gross estimates, not fill predictions. All comparison inputs come from one fresh public snapshot. */
internal object ShardSaleDecisions {
    fun apply(advice: Recommendation,amount: Long,snapshot: QuoteSnapshot?,now: Long): Recommendation {
        val name=SaleProducts.names[advice.productId] ?: return advice
        val shard=snapshot?.quotes?.get(advice.productId)?.takeIf {it.freshAt(now)} ?: return advice.copy(sale=null)
        val essence=snapshot.quotes[SaleMarketProducts.ESSENCE]?.takeIf {it.freshAt(now)} ?: return advice.copy(sale=null)
        if(amount<=0)return advice.copy(sale=null)
        val activity=SaleProducts.names.keys.mapNotNull {snapshot.quotes[it]?.takeIf {q->q.freshAt(now)}?.weeklyBuys}
        // Do not silently compare a shard with a changing subset of the Safari catalogue.
        val average=activity.takeIf {it.size==SaleProducts.names.size}?.average()
        val result=compare(shard.ask,shard.bid,essence.ask,ProfitableCritters.baseEssence(name)/2,
            amount,shard.weeklyBuys,average)
        return advice.copy(quoteAt=minOf(shard.serverAt,essence.serverAt),sale=result)
    }
    fun compare(ask: Double,bid: Double,essence: Double,yield: Int,amount: Long,weekly: Double?,average: Double?): ShardSaleComparison {
        val offer=ask*amount;val instant=bid*amount;val salvage=essence*yield*amount
        val weak=weekly!=null && average!=null && average>0 && weekly<average*.5
        val large=weekly!=null && amount>weekly/7
        val action: ShardSaleAction
        val reason: String
        when {
            salvage>=offer && salvage>=instant -> {action=ShardSaleAction.SALVAGE;reason="Essence is worth more"}
            (weak && offer<=salvage*1.25 || large && offer<=salvage*1.5) && salvage>=instant -> {
                action=ShardSaleAction.SALVAGE;reason=if(large)"Over a day's activity; small premium" else "Small premium; weak demand"
            }
            (weak || large) && instant>=salvage && offer<=instant*1.1 -> {action=ShardSaleAction.INSTANT;reason="Small offer premium; quicker sale"}
            else -> {action=ShardSaleAction.OFFER;reason="Worth the sell-offer premium"}
        }
        return ShardSaleComparison(action,offer,instant,salvage,weekly,average,reason)
    }
}
