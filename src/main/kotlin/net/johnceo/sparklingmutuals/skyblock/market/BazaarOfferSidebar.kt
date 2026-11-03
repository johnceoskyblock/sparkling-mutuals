package net.johnceo.sparklingmutuals.skyblock.market

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.mixin.ShardSaleScreenBounds
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component

/** Current-page cached offer advice; scrolling never navigates orders or changes server items. */
internal object BazaarOfferSidebar {
    private var registered=false
    private var menu: Any?=null
    private var scroll=0
    fun register() {
        if(registered)return
        registered=true
        ScreenEvents.AFTER_INIT.register {_,screen,_,_->
            if(screen is AbstractContainerScreen<*> && SellOfferParsing.isOrders(screen.title.string)) {
                ScreenMouseEvents.allowMouseScroll(screen).register {current,mx,my,_,vertical->
                    val box=current as? AbstractContainerScreen<*>
                    if(box==null || !visible(box) || vertical==0.0)true else {
                        sync(box)
                        val rows=ShardSaleGuidance.orders(box,System.currentTimeMillis())
                        val area=bounds(box,rows.size)
                        if(area==null || !area.contains(mx,my))true else {
                            scroll=area.clampScroll(scroll+if(vertical>0)-1 else 1,rows.size);false
                        }
                    }
                }
            }
        }
    }
    private fun visible(screen: AbstractContainerScreen<*>)=ConfigManager.shardSaleGuidance &&
        Minecraft.getInstance().smScreen===screen && SkyblockSidebar.inSkyblock(Minecraft.getInstance()) &&
        SellOfferParsing.isOrders(screen.title.string)
    private fun sync(screen: AbstractContainerScreen<*>) {
        if(menu!==screen.menu) {menu=screen.menu;scroll=0}
    }
    private fun bounds(screen: AbstractContainerScreen<*>,count: Int): ShardSidebarBounds? {
        val container=screen as? ShardSaleScreenBounds ?: return null
        return ShardSaleSidebarLayout.leftBounds(screen.width,screen.height,container.sparklingMutualsShardPanelLeft(),count)
    }
    fun render(screen: AbstractContainerScreen<*>,g: GuiGraphicsExtractor,mx: Int,my: Int) {
        if(!visible(screen))return
        sync(screen)
        val now=System.currentTimeMillis()
        val rows=ShardSaleGuidance.orders(screen,now)
        val area=bounds(screen,rows.size) ?: return
        scroll=area.clampScroll(scroll,rows.size)
        val client=Minecraft.getInstance()
        val width=ShardSaleSidebarLayout.WIDTH
        val header=ShardSaleSidebarLayout.HEADER
        val rowHeight=ShardSaleSidebarLayout.ROW
        val height=header+area.rows*rowHeight+ShardSaleSidebarLayout.FOOTER
        val hover=area.rowAt(mx.toDouble(),my.toDouble())?.let {rows.getOrNull(scroll+it)}
        g.pose().pushMatrix();g.pose().translate(area.x.toFloat(),area.y.toFloat());g.pose().scale(area.scale,area.scale)
        AppearanceConfig.panels.getValue("progress").draw(g,width,height)
        fun text(value: String,x: Int,y: Int,color: Int=0xFF000000.toInt() or SafariTheme.TEXT)=g.text(client.font,value,x,y,color,true)
        text("Safari sell offers",6,6,HudRow.GOLD)
        rows.drop(scroll).take(area.rows).forEachIndexed {index,row->
            val y=header+index*rowHeight
            if(row==hover)g.fill(2,y-1,width-2,y+10,0x66000000 or SafariTheme.HOVER)
            val action=BazaarUnlistPresentation.action(row.state)
            val statusX=width-7-client.font.width(action)
            text(client.font.plainSubstrByWidth(SaleProducts.names.getValue(row.productId),statusX-18),6,y)
            text(action,statusX,y,BazaarUnlistPresentation.actionColor(row.state))
        }
        if(rows.isEmpty())text(if(ShardSaleGuidance.hasVerifiedProfile())"No eligible Safari offers"else "Confirming profile…",6,header,0xFFAAAAAA.toInt())
        g.pose().popMatrix()
        if(hover!=null)g.setComponentTooltipForNextFrame(client.font,
            (listOf(SaleProducts.names.getValue(hover.productId),BazaarUnlistPresentation.action(hover.state),"")+
                ShardSalePresentation.tooltip(hover,now)).map(Component::literal),mx,my)
    }
}
