package net.johnceo.sparklingmutuals.skyblock.market

/** Pure client lifecycle adapter. Personal orders need a freshly confirmed scope; box ranges do not. */
internal class SaleGuidanceSession(
    private val watches: SellOfferWatch,
    private val demand: (Boolean,Boolean,Set<String>,Long)->Unit,
    private val notify: (String,String)->Unit,
) {
    private var scope: MarketScope?=null
    private var active=false
    private var generation=0L
    fun tick(enabled: Boolean,inSkyblock: Boolean,scope: MarketScope?,localName: String,title: String,
        slots: List<MenuSlot>,advice: Map<String,Recommendation>,now: Long): Map<Int,Recommendation> {
        val on=enabled && inSkyblock
        val selected=if(on)scope else null
        if(selected!=this.scope || on!=active) {
            generation++;this.scope=selected;active=on;watches.selectScope(null);watches.selectScope(selected)
        }
        if(!on) {demand(false,false,emptySet(),generation);return emptyMap()}
        val fresh=advice.mapValues {(_,rec) ->
            if(now-rec.quoteAt>180000 || now<rec.quoteAt-60000)rec.copy(state=AdviceState.UNKNOWN,target=null,reason="STALE_QUOTE")else rec
        }
        watches.observe(SellOfferParsing.parse(title,slots,localName),now)
        val box=HuntingBoxShardParsing.parse(title,slots)
        demand(true,true,box.map {it.productId}.toSet()+watches.products(now),generation)
        watches.update(fresh,now).forEach {notify(it.productId,it.message)}
        return box.associate {entry ->entry.slot to (fresh[entry.productId] ?: Recommendation(entry.productId,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE"))}
    }
    fun reset() {generation++;scope=null;active=false;watches.selectScope(null);demand(false,false,emptySet(),generation)}
}
