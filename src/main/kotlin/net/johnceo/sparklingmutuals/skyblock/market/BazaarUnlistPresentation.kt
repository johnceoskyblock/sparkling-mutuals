package net.johnceo.sparklingmutuals.skyblock.market

internal object BazaarUnlistPresentation {
    const val COLOR=0xFF00DFFF.toInt()
    fun message(products: Set<String>): String? {
        val names=products.mapNotNull {SaleProducts.names[it]}.distinct().sorted()
        if(names.isEmpty())return null
        return "Consider unlisting: "+names.joinToString(", ")+
            ". Guidance changed to HOLD; check your last observed offers are still active."
    }
}

internal data class OrderMarkerSnapshot(val menu: Any,val revision: Int,val items: Map<Int,Any>,val slots: Map<Int,Recommendation>) {
    fun needsUnlist(menu: Any,revision: Int,index: Int,item: Any,now: Long): Boolean {
        if(menu!==this.menu || revision!=this.revision || items[index]!==item)return false
        val advice=slots[index] ?: return false
        return advice.state==AdviceState.HOLD && advice.quoteAt>0 && now-advice.quoteAt<=180000 &&
            now>=advice.quoteAt-60000 && advice.target?.let {it.isFinite() && it>0}==true &&
            advice.ask?.let {it.isFinite() && it>0}==true
    }
}
