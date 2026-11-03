package net.johnceo.sparklingmutuals.skyblock.market

internal data class WholeBoxValue(val known: Double?,val missing: Int,val priced: Int,val quoteAt: Long)
internal object WholeBoxTotals {
    fun calculate(rows: List<WholeBoxOwned>,quotes: QuoteSnapshot?,now: Long): WholeBoxValue {
        var sum=0.0;var missing=0;var priced=0;var at=Long.MAX_VALUE
        for(row in rows) {
            val quote=quotes?.quotes?.get(row.productId)
            val amount=quote?.takeIf {it.freshAt(now) && it.ask.isFinite() && it.ask>0 && it.bid.isFinite() &&
                it.bid>0 && it.bid<=it.ask && row.amount>0}?.ask?.times(row.amount)
            if(amount==null || !amount.isFinite() || sum+amount>=Long.MAX_VALUE.toDouble()) {missing++;continue}
            sum+=amount;priced++;at=minOf(at,quote.serverAt)
        }
        return WholeBoxValue(if(priced>0 || rows.isEmpty())sum else null,missing,priced,if(priced>0)at else 0)
    }
}
