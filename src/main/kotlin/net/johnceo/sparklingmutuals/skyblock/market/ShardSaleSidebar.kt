package net.johnceo.sparklingmutuals.skyblock.market

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SettingsPalette
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.mixin.ShardSaleScreenBounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component

/** Cached read-only panel; callbacks belong to Fabric's per-initialization screen events. */
internal object ShardSaleSidebar {
    private var registered=false
    private var scroll=0
    private var opening=Long.MIN_VALUE
    fun register() {
        if(registered)return
        registered=true
        ScreenEvents.AFTER_INIT.register {_,screen,_,_->
            // Fabric recreates this event on resize as well as initial screen setup.
            if(screen is AbstractContainerScreen<*> && HuntingBoxShardParsing.isBox(screen.title.string)) {
                ScreenMouseEvents.allowMouseScroll(screen).register {current,mx,my,_,vertical->
                    val box=current as? AbstractContainerScreen<*>
                    if(box==null || !visible(box) || vertical==0.0)true
                    else {
                        syncOpening()
                        val rows=ShardSaleGuidance.sidebar(box,System.currentTimeMillis())
                        val bounds=bounds(box,rows.size)
                        if(bounds==null || !bounds.contains(mx,my))true
                        else {scroll=bounds.clampScroll(scroll+if(vertical>0)-1 else 1,rows.size);false}
                    }
                }
            }
        }
    }
    fun render(screen: AbstractContainerScreen<*>,g: GuiGraphicsExtractor,mx: Int,my: Int) {
        if(!visible(screen))return
        syncOpening()
        val now=System.currentTimeMillis()
        draw(screen,g,mx,my,ShardSaleGuidance.sidebar(screen,now),now,ShardSaleGuidance.hasVerifiedProfile())
    }
    private fun visible(screen: AbstractContainerScreen<*>)=ConfigManager.shardSaleGuidance &&
        Minecraft.getInstance().smScreen===screen && SkyblockSidebar.inSkyblock(Minecraft.getInstance()) &&
        HuntingBoxShardParsing.isBox(screen.title.string)
    private fun syncOpening() {
        val current=ShardSaleGuidance.sidebarOpening()
        if(current!=opening) {opening=current;scroll=0}
    }
    private fun bounds(screen: AbstractContainerScreen<*>,count: Int): ShardSidebarBounds? {
        val container=screen as? ShardSaleScreenBounds ?: return null
        return ShardSaleSidebarLayout.bounds(screen.width,screen.height,
            container.sparklingMutualsShardPanelLeft()+container.sparklingMutualsShardPanelWidth(),count)
    }
    fun draw(screen: AbstractContainerScreen<*>,g: GuiGraphicsExtractor,mx: Int,my: Int,rows: List<SavedBoxRow>,now: Long,verifiedProfile: Boolean) {
        val area=bounds(screen,rows.size) ?: return
        scroll=area.clampScroll(scroll,rows.size)
        val client=Minecraft.getInstance()
        val width=ShardSaleSidebarLayout.WIDTH
        val header=ShardSaleSidebarLayout.HEADER
        val rowHeight=ShardSaleSidebarLayout.ROW
        val hover=area.rowAt(mx.toDouble(),my.toDouble())?.let {rows.getOrNull(scroll+it)}
        g.pose().pushMatrix();g.pose().translate(area.x.toFloat(),area.y.toFloat());g.pose().scale(area.scale,area.scale)
        val height=header+area.rows*rowHeight+ShardSaleSidebarLayout.FOOTER
        g.fill(0,0,width,height,SettingsPalette.background)
        g.fill(0,0,width,1,SettingsPalette.accent)
        fun text(value: String,x: Int,y: Int,color: Int=SettingsPalette.text)=g.text(client.font,value,x,y,color,true)
        text("Top /profit shards",6,6,SettingsPalette.accent)
        text("${rows.size} owned · Scroll list",6,18,SettingsPalette.muted)
        if(rows.isEmpty())text(if(ShardSaleGuidance.profitRankingReady())"Visit pages to find top shards" else "Waiting for /profit prices",6,header+2,SettingsPalette.muted)
        rows.drop(scroll).take(area.rows).forEachIndexed {index,row->
            val y=header+index*rowHeight
            if(row==hover)g.fill(2,y-1,width-2,y+12,SettingsPalette.hover)
            val state=when(row.advice.state) {AdviceState.SELL->"SELL";AdviceState.HOLD->"HOLD";AdviceState.UNKNOWN->"UNAVAILABLE"}
            val color=ShardSalePresentation.color(row.advice.state) or 0xFF000000.toInt()
            val statusX=width-7-client.font.width(state)
            val name=client.font.plainSubstrByWidth(SaleProducts.names.getValue(row.shard.productId),statusX-24)
            text(name+(if(row.refreshedThisOpening)"" else " *"),6,y+2)
            text(state,statusX,y+2,color)
        }
        val footer=header+area.rows*rowHeight+4
        text(if(verifiedProfile)"* Previously observed ownership" else "Session list · /profileid to save",6,footer,SettingsPalette.muted)
        text("Experimental price ranges",6,footer+11,SettingsPalette.muted)
        g.pose().popMatrix()
        if(hover!=null) {
            val age=((now-hover.shard.observedAt).coerceAtLeast(0)/1000)
            val lines=listOf(SaleProducts.names.getValue(hover.shard.productId),"Last observed owned: ${hover.shard.amount}",
                "Ownership observed ${age}s ago"+(if(hover.refreshedThisOpening)"" else " · previous opening"),"")+
                ShardSalePresentation.tooltip(hover.advice,now)
            g.setComponentTooltipForNextFrame(client.font,lines.map(Component::literal),mx,my)
        }
    }
}
