package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.history.BazaarPrices

/** Uses the command's cached ranking; this consumer alone may bootstrap its existing quote refresh. */
internal class ProfitGuidanceSelection(private val species: ()->Set<String>,private val refreshQuotes: ()->Unit) {
    fun products(): Set<String> {
        val ranked=species().asSequence().map(BazaarPrices::shardId).filter {it in SaleProducts.names}.take(10).toSet()
        if(ranked.isEmpty())refreshQuotes()
        return ranked
    }
}
