package net.johnceo.sparklingmutuals.skyblock.market

internal object BazaarUnlistPresentation {
    fun action(state: AdviceState)=when(state) {AdviceState.HOLD->"TAKE DOWN";AdviceState.SELL->"KEEP LISTED";AdviceState.UNKNOWN->"UNAVAILABLE"}
    fun actionColor(state: AdviceState)=if(state==AdviceState.HOLD)COLOR else ShardSalePresentation.color(state) or 0xFF000000.toInt()
    const val COLOR=0xFF00DFFF.toInt()
    fun message(products: Set<String>): String? {
        val names=products.mapNotNull {SaleProducts.names[it]}.distinct().sorted()
        if(names.isEmpty())return null
        return "Consider unlisting: "+names.joinToString(", ")+
            ". Guidance changed to HOLD; check your last observed offers are still active."
    }
}

internal data class OrderMarkerSnapshot(val menu: Any,val revision: Int,val items: Map<Int,Any>,val slots: Map<Int,Recommendation>) {
    fun rows(menu: Any,revision: Int,itemAt: (Int)->Any?,now: Long): List<Recommendation> = slots.keys.mapNotNull {index->
        advice(menu,revision,index,itemAt(index),now)
    }.distinctBy {it.productId}.sortedBy {SaleProducts.names.getValue(it.productId)}
    fun needsUnlist(menu: Any,revision: Int,index: Int,item: Any,now: Long): Boolean {
        return advice(menu,revision,index,item,now)?.state==AdviceState.HOLD
    }
    private fun advice(menu: Any,revision: Int,index: Int,item: Any?,now: Long): Recommendation? {
        if(menu!==this.menu || revision!=this.revision || item==null || items[index]!==item)return null
        val advice=slots[index]?.takeIf {it.productId in SaleProducts.names} ?: return null
        val valid=advice.quoteAt>0 && now-advice.quoteAt<=180000 && now>=advice.quoteAt-60000 &&
            advice.target?.let {it.isFinite() && it>0}==true && advice.ask?.let {it.isFinite() && it>0}==true
        return if(valid)advice else advice.copy(state=AdviceState.UNKNOWN,target=null,reason="UNAVAILABLE")
    }
}
