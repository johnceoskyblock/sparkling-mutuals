package net.johnceo.sparklingmutuals.skyblock.market

/** Display-only hysteresis over validated advice. Never use this state to drive personal alerts. */
internal class ShardDisplayAdvice {
    private var previous=emptyMap<String,Recommendation>()

    fun project(confirmed: Map<String,Recommendation>): Map<String,Recommendation> {
        val result=confirmed.mapValues { (id,current) ->
            val ask=current.ask
            val target=current.target
            if(current.state==AdviceState.UNKNOWN || ask==null || !ask.isFinite() || ask<=0 ||
                target==null || !target.isFinite() || target<=0)current
            else {
                val before=previous[id]?.takeIf {it.modelKey==current.modelKey && it.state!=AdviceState.UNKNOWN}
                if(before!=null && current.quoteAt<before.quoteAt)before
                else current.copy(state=when {
                    ask>=target*.995 -> AdviceState.SELL
                    ask<target*.985 -> AdviceState.HOLD
                    else -> before?.state ?: current.state
                })
            }
        }
        previous=result.filterKeys {it in SaleMarketProducts.ids}
        return result
    }

    fun clear() {previous=emptyMap()}
}
