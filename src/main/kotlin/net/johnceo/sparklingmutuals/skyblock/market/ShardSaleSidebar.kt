package net.johnceo.sparklingmutuals.skyblock.market

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.PanelStyle
import net.johnceo.sparklingmutuals.hud.*
import net.johnceo.sparklingmutuals.mixin.ShardSaleScreenBounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import kotlin.math.ceil

/** One scrollable Safari panel, with details confined to hover text. */
internal object ShardSaleSidebar {
    private const val WIDTH=236
    private const val HEADER=46
    private const val ROW=40
    private const val FOOTER=15
    private var registered=false
    private var scroll=0
    private var opening=Long.MIN_VALUE
    fun register() {
        if(registered)return
        registered=true
        ScreenEvents.AFTER_INIT.register {_,screen,_,_->
            if(screen is AbstractContainerScreen<*> && HuntingBoxShardParsing.isBox(screen.title.string)) {
                ScreenMouseEvents.allowMouseScroll(screen).register {current,mx,my,_,vertical->
                    val box=current as? AbstractContainerScreen<*>
                    if(box==null || !visible(box) || vertical==0.0)true
                    else {
                        syncOpening()
                        val rows=ShardSaleGuidance.sidebar(box,System.currentTimeMillis())
                        val area=bounds(box,rows.size)
                        if(area==null || !area.contains(mx,my))true
                        else {scroll=(scroll+if(vertical>0)-1 else 1).coerceIn(0,(rows.size-area.rows*columns(area)).coerceAtLeast(0));false}
                    }
                }
            }
        }
    }
    private fun visible(screen: AbstractContainerScreen<*>)=ConfigManager.shardSaleGuidance &&
        Minecraft.getInstance().smScreen===screen && SkyblockSidebar.inSkyblock(Minecraft.getInstance()) &&
        HuntingBoxShardParsing.isBox(screen.title.string)
    private fun syncOpening() {
        val current=ShardSaleGuidance.sidebarOpening()
        if(current!=opening) {opening=current;scroll=0}
    }
    private fun columns(area: ShardSidebarBounds)=(area.width/area.scale/WIDTH).toInt().coerceIn(1,2)
    private fun bounds(screen: AbstractContainerScreen<*>,count: Int): ShardSidebarBounds? {
        val container=screen as? ShardSaleScreenBounds ?: return null
        val right=container.sparklingMutualsShardPanelLeft()+container.sparklingMutualsShardPanelWidth()
        val left=container.sparklingMutualsShardPanelLeft()
        val space=maxOf(screen.width-right-10,left-10)
        val cols=if(space>=WIDTH*2 && count>12)2 else 1
        val width=WIDTH*cols
        val layout=SafariHud.SHARD_GUIDANCE.layout
        if(!layout.hasSavedPosition) {
            val fitted=minOf(1f,space/width.toFloat())
            // For narrow GUI resolutions use a compact overlay; it can be moved in /sm gui.
            layout.scale=fitted.coerceIn(.5f,1f)
            layout.x=(if(screen.width-right>=left)right+6 else (left-6-width*layout.scale).toInt()).coerceAtLeast(0)/screen.width.toFloat()
        }
        val scale=layout.effectiveScale(screen.width,screen.height,width,1).coerceAtMost(screen.width/width.toFloat())
        val capacity=((screen.height-12)/scale-HEADER-FOOTER).toInt().div(ROW).coerceIn(1,37)
        val rows=((count+cols-1)/cols).coerceIn(1,capacity)
        val height=HEADER+rows*ROW+FOOTER
        if(!layout.hasSavedPosition)layout.y=((screen.height-height*scale)/2).coerceAtLeast(0f)/screen.height
        val area=layout.bounds(screen.width,screen.height,width,height)
        return ShardSidebarBounds(area.x,area.y,ceil(width*scale).toInt(),ceil(height*scale).toInt(),scale,rows,HEADER)
    }
    fun render(screen: AbstractContainerScreen<*>,g: GuiGraphicsExtractor,mx: Int,my: Int) {
        if(!visible(screen))return
        syncOpening()
        draw(screen,g,mx,my,ShardSaleGuidance.sidebar(screen,System.currentTimeMillis()),System.currentTimeMillis())
    }
    fun draw(screen: AbstractContainerScreen<*>,g: GuiGraphicsExtractor,mx: Int,my: Int,rows: List<SavedBoxRow>,now: Long) {
        val area=bounds(screen,rows.size) ?: return
        val cols=columns(area)
        val capacity=area.rows*cols
        scroll=scroll.coerceIn(0,(rows.size-capacity).coerceAtLeast(0))
        val visible=rows.drop(scroll).take(capacity)
        val height=HEADER+area.rows*ROW+FOOTER
        val panel=object: HudPanel("Safari shard guidance",rows=emptyList(),titleIcon=null,widthOverride=WIDTH*cols,heightOverride=height) {
            override fun draw(graphics: GuiGraphicsExtractor,x: Int,y: Int,scale: Float,style: PanelStyle,borderOverride: Int?) {
                paint(graphics,x,y,scale,visible,rows.size,capacity,height,cols,area.rows)
            }
        }
        HuntingBoxHuds.shown(SafariHud.SHARD_GUIDANCE,panel)
        panel.draw(g,area.x,area.y,area.scale)
        val local=(my-area.y)/area.scale-HEADER
        if(area.contains(mx.toDouble(),my.toDouble()) && local>=0 && local<area.rows*ROW) {
            val row=visible.getOrNull(((mx-area.x)/area.scale/WIDTH).toInt()*area.rows+(local/ROW).toInt()) ?: return
            val lines=listOf("§f§l"+SaleProducts.names.getValue(row.shard.productId)+"§r §8· §6${row.shard.amount} §7shards")+
                ShardSalePresentation.tooltip(row.advice,now)+
                if(row.shard.amount==0L)listOf("§8Per-shard estimate · no observed holdings") else if(row.refreshedThisOpening)emptyList() else listOf("§8Quantity from a previously visited page")
            g.setComponentTooltipForNextFrame(Minecraft.getInstance().font,lines.map(Component::literal),mx,my)
        }
    }
    private fun paint(g: GuiGraphicsExtractor,x: Int,y: Int,scale: Float,rows: List<SavedBoxRow>,count: Int,capacity: Int,height: Int,columns: Int,perColumn: Int) {
        val client=Minecraft.getInstance();val font=client.font
        g.pose().pushMatrix();g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale)
        AppearanceConfig.panels.getValue("shard_guidance").draw(g,WIDTH*columns,height)
        fun text(s: String,x: Int,y: Int,color: Int=HudRow.WHITE)=g.text(font,s,x,y,color,true)
        g.fill(3,3,WIDTH*columns-3,HEADER-5,0x332A2417)
        g.fill(3,3,5,HEADER-5,HudRow.GOLD)
        text("§lSafari Shard Guidance",9,6,HudRow.GOLD)
        text("Highest holding value first · Before tax",6,17,HudRow.GRAY)
        val average=rows.firstNotNullOfOrNull {it.advice.sale?.average}
        text("7d volume avg: "+(average?.let(ShardSalePresentation::compact) ?: "—")+" shards",6,28,HudRow.GRAY)
        if(count==0)text("Open pages containing Safari shards",6,HEADER,HudRow.GRAY)
        rows.forEachIndexed {index,row->
            val top=HEADER+(index%perColumn)*ROW;val sale=row.advice.sale
            g.pose().pushMatrix();g.pose().translate((index/perColumn*WIDTH).toFloat(),0f)
            val action=sale?.action?.label ?: "NO PRICE"
            val color=sale?.action?.color ?: HudRow.GRAY
            val name=font.plainSubstrByWidth("${scroll+index+1}. "+SaleProducts.names.getValue(row.shard.productId),WIDTH-18-font.width(action))
            text(name,6,top)
            text(action,WIDTH-6-font.width(action),top,color)
            val value=sale?.let {ShardSalePresentation.compact(it.recommended)+" coins"} ?: "—"
            text((if(row.shard.amount==0L)"Each: " else "Value: ")+value,6,top+12,HudRow.GOLD)
            text("7d volume: "+(sale?.weekly?.let(ShardSalePresentation::compact) ?: "—"),6,top+24,HudRow.GRAY)
            val label=ShardSalePresentation.volumeLabel(sale?.ratio)
            val tint=ShardSalePresentation.volumeColor(sale?.ratio)
            text(label,WIDTH-6-font.width(label),top+24,tint)
            // Fixed column for every row; the tick marks 100% of the Safari average.
            val barLeft=WIDTH-94
            val barWidth=34
            g.fill(barLeft,top+27,barLeft+barWidth,top+30,0xFF33333D.toInt())
            sale?.ratio?.let {ratio ->
                g.fill(barLeft,top+27,barLeft+(barWidth*ratio.coerceIn(0.0,1.5)/1.5).toInt(),top+30,tint)
            }
            g.fill(barLeft+23,top+25,barLeft+24,top+31,0xFF888888.toInt())
            if(index%perColumn<perColumn-1 && index<rows.lastIndex)
                g.fill(6,top+ROW-4,WIDTH-6,top+ROW-3,0x553B3B43)
            g.pose().popMatrix()
        }
        text(if(count>capacity)"${scroll+1}–${minOf(scroll+capacity,count)} / $count · Scroll" else "$count shard types · Hover for details",6,height-11,HudRow.GRAY)
        if(count>capacity) {
            val track=perColumn*ROW
            val thumb=(track*capacity/count).coerceAtLeast(10)
            val start=HEADER+(track-thumb)*scroll/(count-capacity)
            g.fill(WIDTH*columns-3,HEADER,WIDTH*columns-2,HEADER+track,0xFF33333D.toInt())
            g.fill(WIDTH*columns-3,start,WIDTH*columns-2,start+thumb,HudRow.GRAY)
        }
        g.pose().popMatrix()
    }
}
