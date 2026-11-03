package net.johnceo.sparklingmutuals.skyblock.market

/** Pure client lifecycle adapter. Personal orders need a freshly confirmed scope; box ranges do not. */
internal class SaleGuidanceSession(
    private val watches: SellOfferWatch,
    private val demand: (Boolean,Boolean,Set<String>,Long)->Unit,
    private val notify: (String,String)->Unit,
    private val boxes: SavedHuntingBoxLedger=SavedHuntingBoxLedger(SaleProducts.names.keys),
) {
    private var scope: MarketScope?=null
    private var active=false
    private var generation=0L
    private var connection: Any?=null
    private var lastBoxAt: Long?=null
    private var insideBox=false
    private var latest=emptyMap<String,Recommendation>()
    var sidebarOpening=0L
        private set
    fun tick(enabled: Boolean,inSkyblock: Boolean,scope: MarketScope?,localName: String,title: String,
        slots: List<MenuSlot>,advice: Map<String,Recommendation>,now: Long,connection: Any?=null): Map<Int,Recommendation> {
        val on=enabled && inSkyblock
        val selected=if(on)scope else null
        if(selected!=this.scope || on!=active || connection!==this.connection) {
            generation++;this.scope=selected;active=on;watches.selectScope(null);watches.selectScope(selected)
            this.connection=connection;lastBoxAt=null
        }
        boxes.select(selected,connection)
        insideBox=on && HuntingBoxShardParsing.isBox(title)
        if(!on) {latest=emptyMap();demand(false,false,emptySet(),generation);return emptyMap()}
        val fresh=advice.mapValues {(_,rec) ->fresh(rec,now)}
        latest=fresh
        watches.observe(SellOfferParsing.parse(title,slots,localName),now)
        val observed=HuntingBoxShardParsing.observe(title,slots)
        if(insideBox) {
            val previous=lastBoxAt
            if(previous==null || now<previous || now-previous>2000) {boxes.beginOpening();sidebarOpening++}
            lastBoxAt=now;boxes.observe(observed,now)
        }
        val box=observed.filter {it.amount>0}
        val products=if(insideBox)boxes.rows().map {it.first.productId}.toSet()else emptySet()
        demand(true,true,products+watches.products(now),generation)
        watches.update(fresh,now).forEach {notify(it.productId,it.message)}
        return box.associate {entry ->entry.slot to (fresh[entry.productId] ?: Recommendation(entry.productId,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE"))}
    }
    private fun fresh(rec: Recommendation,now: Long)=
        if(now-rec.quoteAt>180000 || now<rec.quoteAt-60000)rec.copy(state=AdviceState.UNKNOWN,target=null,reason="STALE_QUOTE")else rec
    fun reset() {
        generation++;scope=null;active=false;connection=null;lastBoxAt=null;insideBox=false;latest=emptyMap()
        boxes.select(null,null);watches.selectScope(null);demand(false,false,emptySet(),generation)
    }
    fun sidebar(now: Long): List<SavedBoxRow> = if(!insideBox)emptyList()else boxes.rows().map {(shard,seen)->
        SavedBoxRow(shard,seen,latest[shard.productId]?.let {fresh(it,now)} ?:
            Recommendation(shard.productId,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE"))
    }
    fun restoreBoxes(raw: String,now: Long) {boxes.restore(raw,now)}
    fun exportBoxes(): String = boxes.export()
}
