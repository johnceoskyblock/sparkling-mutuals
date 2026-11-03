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
    fun color(state: AdviceState)=when(state) {AdviceState.SELL->0xCC55FF55.toInt();AdviceState.HOLD->0xCCFF5555.toInt();AdviceState.UNKNOWN->0xAA999999.toInt()}
    private fun coins(value: Double?)=value?.let {String.format(Locale.ROOT,"%,.1f coins",it)} ?: "unavailable"
    fun tooltip(advice: Recommendation,now: Long): List<String> = buildList {
        add("Experimental shard range: ${advice.state}")
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
