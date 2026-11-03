package net.johnceo.sparklingmutuals.skyblock.market

internal object BazaarUnlistPresentation {
    fun message(products: Set<String>): String? {
        val names=products.mapNotNull {SaleProducts.names[it]}.distinct().sorted()
        if(names.isEmpty())return null
        return "Consider unlisting: "+names.joinToString(", ")+
            ". Guidance changed to HOLD; check your last observed offers are still active."
    }
}
