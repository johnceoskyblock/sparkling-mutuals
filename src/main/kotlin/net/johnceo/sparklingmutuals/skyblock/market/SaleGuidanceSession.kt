package net.johnceo.sparklingmutuals.skyblock.market

/** Pure client lifecycle adapter. Personal orders need a freshly confirmed scope; box ranges do not. */
internal class SaleGuidanceSession(
    private val watches: SellOfferWatch,
    private val demand: (Boolean,Boolean,Set<String>,Long)->Unit,
    private val notify: (String,String)->Unit,
    private val boxes: SavedHuntingBoxLedger=SavedHuntingBoxLedger(SaleProducts.names.keys),
    private val profitProducts: ()->Set<String> = {emptySet()},
    private val ownedProducts: ()->Set<String> = {emptySet()},
) {
    private var scope: MarketScope?=null
    private var active=false
    private var generation=0L
    private var connection: Any?=null
    private var lastBoxAt: Long?=null
    private var insideBox=false
    private var latest=emptyMap<String,Recommendation>()
    private val display=ShardDisplayAdvice()
    private var eligible=emptySet<String>()
    private var markers=emptyMap<Int,Recommendation>()
    private var offerAdvice=emptyMap<Int,Recommendation>()
    var sidebarOpening=0L
        private set
    fun tick(enabled: Boolean,inSkyblock: Boolean,scope: MarketScope?,localName: String,title: String,
        slots: List<MenuSlot>,advice: Map<String,Recommendation>,now: Long,connection: Any?=null): Map<Int,Recommendation> {
        val on=enabled && inSkyblock
        val selected=if(on)scope?.let {SaleProfileEvidence.scope(it.accountId,it.profileId)}else null
        if(selected!=this.scope || on!=active || connection!==this.connection) {
            generation++;this.scope=selected;active=on;watches.selectScope(null);watches.selectScope(selected)
            this.connection=connection;lastBoxAt=null
            display.clear()
        }
        boxes.select(selected,connection)
        insideBox=on && HuntingBoxShardParsing.isBox(title)
        val orders=on && SellOfferParsing.isOrders(title)
        val ranked=if(insideBox || orders || on && watches.products(now).isNotEmpty())
            profitProducts().intersect(SaleProducts.names.keys).take(10).toSet()else emptySet()
        eligible=if(insideBox)ranked else emptySet()
        markers=emptyMap()
        offerAdvice=emptyMap()
        if(!on) {latest=emptyMap();demand(false,false,emptySet(),generation);return emptyMap()}
        val qualifying=ranked+if(selected!=null)ownedProducts().intersect(SaleProducts.names.keys)else emptySet()
        val fresh=advice.mapValues {(_,rec) ->fresh(rec,now)}
        latest=display.project(fresh)
        val offers=if(selected!=null)SellOfferParsing.parseSlots(title,slots,localName)
            .filter {!it.evidence.completed && it.evidence.productId in qualifying}else emptyList()
        watches.observe(offers.map {it.evidence},now)
        val observed=HuntingBoxShardParsing.observe(title,slots)
        if(insideBox) {
            val previous=lastBoxAt
            if(previous==null || now<previous || now-previous>2000) {boxes.beginOpening();sidebarOpening++}
            lastBoxAt=now;boxes.observe(observed,now)
        }
        val box=observed.filter {it.amount>0 && it.productId in eligible}
        val products=if(insideBox)boxes.rows().map {it.first.productId}.filter {it in eligible}.toSet()else emptySet()
        demand(true,true,products+watches.products(now).intersect(qualifying),generation)
        offerAdvice=offers.associate {offer->offer.slot to (fresh[offer.evidence.productId]?.takeIf {it.productId==offer.evidence.productId}
            ?: Recommendation(offer.evidence.productId,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE"))}
        markers=offerAdvice.filterValues {it.state==AdviceState.HOLD}
        val alerts=watches.update(fresh.filterKeys {it in qualifying},now)
        BazaarUnlistPresentation.message(alerts.map {it.productId}.toSet())?.let {notify(alerts.first().productId,it)}
        return box.associate {entry ->entry.slot to (latest[entry.productId] ?: Recommendation(entry.productId,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE"))}
    }
    private fun fresh(rec: Recommendation,now: Long)=
        if(now-rec.quoteAt>180000 || now<rec.quoteAt-60000)rec.copy(state=AdviceState.UNKNOWN,target=null,reason="STALE_QUOTE")else rec
    fun reset() {
        generation++;scope=null;active=false;connection=null;lastBoxAt=null;insideBox=false;latest=emptyMap();eligible=emptySet()
        display.clear()
        markers=emptyMap()
        offerAdvice=emptyMap()
        boxes.select(null,null);watches.selectScope(null);demand(false,false,emptySet(),generation)
    }
    fun sidebar(now: Long): List<SavedBoxRow> = if(!insideBox)emptyList()else boxes.rows().filter {it.first.productId in eligible}.map {(shard,seen)->
        SavedBoxRow(shard,seen,latest[shard.productId]?.let {fresh(it,now)} ?:
            Recommendation(shard.productId,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE"))
    }
    fun restoreBoxes(raw: String,now: Long) {boxes.restore(raw,now)}
    fun exportBoxes(): String = boxes.export()
    fun profitRankingReady()=eligible.isNotEmpty()
    fun orderMarkers(): Map<Int,Recommendation> = markers
    fun orderAdvice(): Map<Int,Recommendation> = offerAdvice
}
