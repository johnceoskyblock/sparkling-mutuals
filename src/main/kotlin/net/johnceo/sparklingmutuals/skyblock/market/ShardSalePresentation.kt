package net.johnceo.sparklingmutuals.skyblock.market

import java.util.Locale

/** Item references and menu revision come from client ticks; render only checks identity and freshness. */
internal data class SaleMenuSnapshot(val menu: Any,val revision: Int,val items: Map<Int,Any>,val slots: Map<Int,Recommendation>) {
    fun advice(menu: Any,revision: Int,index: Int,item: Any,now: Long): Recommendation? {
        if(menu!==this.menu || revision!=this.revision || items[index]!==item)return null
        val advice=slots[index] ?: return null
        return if(advice.state!=AdviceState.UNKNOWN && (now-advice.quoteAt>180000 || now<advice.quoteAt-60000))
            advice.copy(state=AdviceState.UNKNOWN,target=null,reason="STALE_QUOTE")else advice
    }
}

internal object ShardSalePresentation {
    fun color(advice: Recommendation)=advice.sale?.action?.color ?: color(advice.state)
    fun color(state: AdviceState)=when(state) {AdviceState.SELL->0xCC55FF55.toInt();AdviceState.HOLD->0xCCFF5555.toInt();AdviceState.UNKNOWN->0xAA999999.toInt()}
    fun compact(value: Double)=net.johnceo.sparklingmutuals.history.BazaarPrices.format(value.toLong())
    fun demand(sale: ShardSaleComparison): String {
        val week=sale.weekly ?: return "7d activity unavailable"
        val avg=sale.average ?: return "7d activity: "+compact(week)
        return "7d volume: "+compact(week)+" shards · Safari avg "+compact(avg)+" · "+((sale.ratio ?: 0.0)*100).toInt()+"%"
    }
    fun volumeLabel(ratio: Double?)=when {ratio==null->"Unknown";ratio<.5->"Low";ratio<1.5->"Typical";else->"High"}
    fun volumeColor(ratio: Double?)=when {ratio==null->0xFFAAAAAA.toInt();ratio<.5->0xFFFFAA77.toInt();ratio<1.5->0xFFCCCCCC.toInt();else->0xFF66DDBB.toInt()}
    fun rank(rows: List<SavedBoxRow>): List<SavedBoxRow> = rows.sortedWith(
        compareByDescending<SavedBoxRow> {it.shard.amount>0}
            .thenByDescending {it.advice.sale?.recommended ?: -1.0}
            .thenBy {SaleProducts.names[it.shard.productId] ?: it.shard.productId})
    private fun coins(value: Double?)=value?.let {String.format(Locale.ROOT,"%,.1f coins",it)} ?: "unavailable"
    fun tooltip(advice: Recommendation,now: Long): List<String> = buildList {
        advice.sale?.let {sale ->
            val actionColor=when(sale.action) {ShardSaleAction.OFFER->"§a";ShardSaleAction.INSTANT->"§e";ShardSaleAction.SALVAGE->"§b"}
            add(actionColor+"§l"+sale.action.label+"§r")
            add("§7"+sale.reason)
            add("")
            add("§aSell offer: §f"+compact(sale.offer)+" §7coins")
            add("§eInstant sale: §f"+compact(sale.instant)+" §7coins")
            add("§bSalvage: §f"+compact(sale.salvage)+" §7coins")
            add("")
            add("§d7d sell volume: §f"+(sale.weekly?.let(::compact) ?: "Unknown")+" §7shards")
            add("§dSafari average: §f"+(sale.average?.let(::compact) ?: "Unknown")+" §7shards / 7d")
            val volumeTint=when {sale.ratio==null->"§7";sale.ratio!!<.5->"§6";sale.ratio!!<1.5->"§f";else->"§a"}
            add("§7Volume bar: "+volumeTint+volumeLabel(sale.ratio)+
                (sale.ratio?.let {" ("+(it*100).toInt()+"% of average)"} ?: ""))
            add("§7Compares this shard's sell volume with")
            add("§7the average Safari shard sell volume.")
            add("§7Tick = average §8· §7Full bar = 150% or more")
            add("§8Before tax · estimated volume, not guaranteed sales")
            return@buildList
        }
        add("Recent price: "+advice.state)
        if(advice.state==AdviceState.UNKNOWN) {
            add("Range guidance unavailable: "+when(advice.reason) {
                "STALE_QUOTE"->"live price is stale"
                "STALE_HISTORY"->"history is stale"
                "REFERENCE_MISMATCH"->"live and reference prices disagree"
                else->"insufficient recent history"
            })
        } else {
            add(if(advice.state==AdviceState.SELL)"SELL: near/above recent reference target" else "HOLD: below recent reference target")
            add("Current competing sell offer: ${coins(advice.ask)}")
            add("Recent reference target: ${coins(advice.target)}")
            if(advice.reason=="EXPERIMENTAL_FALLING_RANGE")add("Recent sustained prices are falling")
        }
        add(if(advice.quoteAt<=0)"Live quote unavailable" else "Live quote age: ${((now-advice.quoteAt).coerceAtLeast(0)/1000)}s")
        add("Skyblock.finance history + Hypixel live book")
        add("Descriptive range; not a forecast or fill guarantee")
    }
}
