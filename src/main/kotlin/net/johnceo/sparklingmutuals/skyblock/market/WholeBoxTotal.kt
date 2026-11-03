package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.mixin.ShardSaleScreenBounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.math.ceil
import kotlin.math.roundToLong

internal data class WholeBoxSummary(val value: WholeBoxValue,val coverage: WholeBoxCoverage,val verified: Boolean)
internal data class WholeBoxHeaderBounds(val x: Int,val y: Int,val width: Int,val height: Int,val scale: Float)
internal class WholeBoxTotal(private val refresh: ()->Unit={}) {
    private val ledger=WholeBoxLedger()
    private var active=false
    private var selected: MarketScope?=null
    private var connection: Any?=null
    private var lastBoxAt: Long?=null
    private var summary: WholeBoxSummary?=null
    fun tick(enabled: Boolean,inSkyblock: Boolean,scope: MarketScope?,connection: Any?,title: String,
        slots: List<MenuSlot>,quotes: QuoteSnapshot?,now: Long) {
        val on=enabled && inSkyblock
        val valid=if(on)scope?.let {SaleProfileEvidence.scope(it.accountId,it.profileId)}else null
        if(on!=active || valid!=selected || connection!==this.connection) {
            ledger.select(valid,if(on)connection else null);ledger.beginOpening();lastBoxAt=null
            active=on;selected=valid;this.connection=connection
        }
        summary=null
        if(!on || !HuntingBoxShardParsing.isBox(title)) {lastBoxAt=null;ledger.beginOpening();return}
        val previous=lastBoxAt
        if(previous==null || now<previous || now-previous>2000)ledger.beginOpening()
        lastBoxAt=now
        refresh() // Shared throttled public snapshot only; no per-product/history requests.
        val catalog=quotes?.let {it.quotes.keys+it.quickPrices.keys}.orEmpty()
        val page=WholeBoxParsing.observe(title,slots,catalog)
        if(page!=null)ledger.observe(page,now)
        val coverage=ledger.coverage()
        val rows=ledger.rows()
        val value=if(rows.isEmpty() && coverage.visited==0)WholeBoxValue(null,0,0,0)
            else WholeBoxTotals.calculate(rows,quotes,now)
        summary=WholeBoxSummary(value,coverage,valid!=null)
    }
    fun snapshot(now: Long): WholeBoxSummary? = summary?.let {current ->
        val value=current.value
        if(value.priced>0 && (value.quoteAt<=0 || now-value.quoteAt>180000 || now<value.quoteAt-60000))
            current.copy(value=WholeBoxValue(null,value.missing+value.priced,0,0))else current
    }
    fun reset() {active=false;selected=null;connection=null;lastBoxAt=null;summary=null;ledger.select(null,null);ledger.beginOpening()}
    fun export()=ledger.export()
    fun restore(raw: String,now: Long) {ledger.restore(raw,now)}
    fun ownedSafariProducts(scope: MarketScope?,connection: Any?): Set<String> {
        val valid=scope?.let {SaleProfileEvidence.scope(it.accountId,it.profileId)} ?: return emptySet()
        if(!active || valid!=selected || connection!==this.connection)return emptySet()
        return ledger.rows().map {it.productId}.filter {it in SaleProducts.names}.toSet()
    }
}
internal object WholeBoxHeader {
    fun lines(summary: WholeBoxSummary): List<String> = listOf("Hunting Box value: "+
        (summary.value.known?.let {BazaarPrices.format(it.roundToLong())+" coins"} ?: "unavailable"))
    fun bounds(screenWidth: Int,left: Int,top: Int,width: Int,textWidth: Int,lineCount: Int): WholeBoxHeaderBounds? {
        if(left<0 || width<=12 || left+width>screenWidth || lineCount<=0)return null
        val scale=minOf(1f,(width-12).toFloat()/textWidth.coerceAtLeast(1))
        if(scale<.75f)return null
        val height=ceil((8+lineCount*10)*scale).toInt()
        val y=top-height-4
        return if(y<4)null else WholeBoxHeaderBounds(left,y,width,height,scale)
    }
}

/** Cached total only, composed before vanilla tooltips; no item-value tooltips or input handlers. */
internal object WholeBoxTotalOverlay {
    fun render(screen: AbstractContainerScreen<*>,g: GuiGraphicsExtractor) {
        val summary=ShardSaleGuidance.totalSummary(screen,System.currentTimeMillis()) ?: return
        val container=screen as? ShardSaleScreenBounds ?: return
        val lines=WholeBoxHeader.lines(summary)
        val client=Minecraft.getInstance()
        val area=WholeBoxHeader.bounds(screen.width,container.sparklingMutualsShardPanelLeft(),
            container.sparklingMutualsShardPanelTop(),container.sparklingMutualsShardPanelWidth(),
            lines.maxOfOrNull {client.font.width(it)} ?: 0,lines.size) ?: return
        g.pose().pushMatrix();g.pose().translate(area.x.toFloat(),area.y.toFloat());g.pose().scale(area.scale,area.scale)
        val width=(area.width/area.scale).toInt()
        AppearanceConfig.panels.getValue("progress").draw(g,width,8+lines.size*10)
        lines.forEachIndexed {index,text->g.text(client.font,text,6,4+index*10,HudRow.GOLD,true)}
        g.pose().popMatrix()
    }
}
