package net.johnceo.sparklingmutuals.skyblock

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SettingsPalette
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToLong

/** SkyHanni-style value list, implemented with our screen events and shared three-hour Bazaar cache. */
object HuntingBoxValue {
    private val ledger=HuntingBoxLedger()
    private val icons=mutableMapOf<String,ItemStack>()
    private var identity: String?=null
    private var connection: Any?=null
    private var lastOpen=0L
    private var fingerprint: BoxPage?=null
    private var stableSince=0L
    private var viewSignature: List<String>?=null
    private var scroll=0
    private var ticks=0
    private data class Bounds(val x: Int,val y: Int,val scale: Float,val rows: Int) {
        fun contains(mx: Double,my: Double)=mx>=x && mx<x+WIDTH*scale && my>=y && my<y+height*scale
        val height get()=84+rows*15
    }
    private const val WIDTH=240
    private fun bounds(screen: Screen): Bounds {
        val free=((screen.width-176)/2-10).coerceAtLeast(60)
        val scale=minOf(1f,free/WIDTH.toFloat()).coerceAtLeast(.25f)
        val rows=minOf(12,((screen.height/scale-90)/15).toInt()).coerceAtLeast(3)
        return Bounds((screen.width+176)/2+5,((screen.height-(84+rows*15)*scale)/2).toInt().coerceAtLeast(4),scale,rows)
    }
    fun register() {
        ScreenEvents.AFTER_INIT.register { _,screen,_,_ ->
            if (screen is AbstractContainerScreen<*> && HuntingBoxParsing.isBox(screen.title.string)) {
                ScreenEvents.afterExtract(screen).register { current,g,mx,my,_ -> render(current,g,mx,my) }
                ScreenMouseEvents.allowMouseScroll(screen).register { current,mx,my,_,vertical ->
                    if (enabled() && bounds(current).contains(mx,my)) {
                        val maximum=(ledger.values(BazaarPrices.cachedPrices).size-bounds(current).rows).coerceAtLeast(0)
                        scroll=(scroll-if(vertical>0) 1 else if(vertical<0) -1 else 0).coerceIn(0,maximum)
                        false
                    } else true
                }
            }
        }
        ClientReceiveMessageEvents.GAME.register { text,overlay ->
            if (!overlay && SafariRules.strip(text.string).startsWith("Profile ID: ")) Minecraft.getInstance().execute { reset() }
        }
    }
    private fun enabled()=ConfigManager.huntingBoxValue && SkyblockSidebar.inSkyblock(Minecraft.getInstance())
    fun tick(client: Minecraft) {
        val account=client.player?.uuid?.toString()
        if (identity!=account || connection!==client.connection) { reset();identity=account;connection=client.connection }
        if (++ticks%5!=0) return
        val screen=client.smScreen as? AbstractContainerScreen<*>
        val now=System.currentTimeMillis()
        if (!enabled() || screen==null || !HuntingBoxParsing.isBox(screen.title.string)) {
            if (now-lastOpen>2000) clearPages()
            return
        }
        lastOpen=now
        BazaarPrices.ensureFresh()
        val slots=screen.menu.slots.filter { it.container !is Inventory }
        if (slots.none { !it.item.isEmpty }) return // Menu packets have not arrived yet.
        val view=slots.filter { slot ->
            val label=SafariRules.strip(slot.item.hoverName.string).lowercase()
            listOf("sort","filter","search").any { it in label }
        }.flatMap { slot -> listOf(slot.item.hoverName.string)+slot.item.get(DataComponents.LORE)?.lines().orEmpty().map { it.string } }
        if (viewSignature!=null && viewSignature!=view) clearPages()
        viewSignature=view
        val shards=slots.mapNotNull { slot ->
            val item=slot.item
            val id=item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","") ?: return@mapNotNull null
            HuntingBoxParsing.shard(slot.index,id,item.hoverName.string,
                item.get(DataComponents.LORE)?.lines().orEmpty().map { it.string })?.also { icons[it.id]=item.copy() }
        }
        val navigation=slots.filter { it.index !in 9..44 || it.index%9 in setOf(0,8) }.filterNot {
            SafariRules.strip(it.item.hoverName.string).trim().equals("Next Page",true) ||
                SafariRules.strip(it.item.hoverName.string).trim().equals("Previous Page",true)
        }.flatMap {
            listOf(it.item.hoverName.string)+it.item.get(DataComponents.LORE)?.lines().orEmpty().map { line -> line.string }
        }
        val page=HuntingBoxParsing.page(screen.title.string,navigation,shards)
        if (fingerprint!=page) { fingerprint=page;stableSince=now;return }
        if (now-stableSince>=250) ledger.observe(page)
    }
    private fun render(screen: Screen,g: GuiGraphicsExtractor,mx: Int,my: Int) {
        if (!enabled()) return
        val client=Minecraft.getInstance()
        val box=screen as? AbstractContainerScreen<*> ?: return
        val area=bounds(screen)
        val values=ledger.values(BazaarPrices.cachedPrices)
        scroll=scroll.coerceIn(0,(values.size-area.rows).coerceAtLeast(0))
        val lx=(mx-area.x)/area.scale;val ly=(my-area.y)/area.scale
        val hovered=if(lx in 0f..WIDTH.toFloat() && ly>=30 && ly<30+area.rows*15)
            values.getOrNull(scroll+((ly-30)/15).toInt()) else null
        g.pose().pushMatrix();g.pose().translate(area.x.toFloat(),area.y.toFloat());g.pose().scale(area.scale,area.scale)
        g.fill(0,0,WIDTH,area.height,0xE8101015.toInt())
        g.fill(0,0,WIDTH,1,SettingsPalette.accent)
        fun text(value: String,y: Int,color: Int=-1)=g.text(client.font,value,6,y,color,true)
        text("Hunting Box Value",6,SettingsPalette.accent)
        val coverage=ledger.totalPages?.let { "${ledger.scannedPages}/$it pages scanned" } ?: "${ledger.scannedPages} pages scanned"
        text(coverage+" · Scroll list",18,0xFFAAAAAA.toInt())
        if (ledger.scannedPages==0) text("Reading Hunting Box…",34,0xFFAAAAAA.toInt())
        values.drop(scroll).take(area.rows).forEachIndexed { index,value ->
            val y=30+index*15
            if (value==hovered) g.fill(2,y-1,WIDTH-2,y+13,0x553B3B50)
            icons[value.shard.id]?.let {
                g.pose().pushMatrix();g.pose().translate(5f,y.toFloat());g.pose().scale(.7f,.7f)
                g.item(it,0,0);g.pose().popMatrix()
            }
            val amount=" ×${value.shard.amount}"
            val name=client.font.plainSubstrByWidth(value.shard.name.removeSuffix(" Shard"),125-client.font.width(amount).coerceAtMost(60))
            g.text(client.font,name+amount,19,y+2,-1,true)
            val price=value.sell?.let { BazaarPrices.format(it.roundToLong()) } ?: "?"
            g.text(client.font,price,WIDTH-7-client.font.width(price),y+2,0xFFFFCC66.toInt(),true)
        }
        val base=34+area.rows*15
        val missing=values.count { it.sell==null || it.buy==null }
        val prefix=if(missing>0) "≥" else ""
        text("Shards: ${ledger.shards().sumOf { it.amount }}",base)
        text("Instant sell: $prefix${BazaarPrices.format(values.sumOf { it.sell ?: 0.0 }.roundToLong())}",base+11,0xFFFFCC66.toInt())
        text("Instant buy: $prefix${BazaarPrices.format(values.sumOf { it.buy ?: 0.0 }.roundToLong())}",base+22,0xFF99DDFF.toInt())
        if(missing>0) text("$missing shard prices unavailable",base+33,0xFFFF9999.toInt())
        g.pose().popMatrix()
        hovered?.let { value ->
            val quantity=value.shard.amount
            fun coins(value: Double?)=value?.let { "%,.0f coins".format(it) } ?: "Price unavailable"
            g.setComponentTooltipForNextFrame(client.font,listOf(
                value.shard.name,"Owned: $quantity", "",
                "Instant sell / shard: ${coins(value.sell?.div(quantity))}",
                "Instant buy / shard: ${coins(value.buy?.div(quantity))}", "",
                "Instant sell total: ${coins(value.sell)}","Instant buy total: ${coins(value.buy)}"
            ).map(Component::literal),mx,my)
            val slot=fingerprint?.shards?.firstOrNull { it.id==value.shard.id }?.slot
            val current=box.menu.slots.firstOrNull { it.container !is Inventory && it.index==slot }
            if(current!=null) {
                val upper=box.menu.slots.count { it.container !is Inventory }
                val x=(screen.width-176)/2+current.x;val y=(screen.height-(114+upper/9*18))/2+current.y
                g.fill(x,y,x+16,y+16,0x66FFD066)
            }
        }
    }
    private fun clearPages() { ledger.clear();icons.clear();viewSignature=null;fingerprint=null;stableSince=0;scroll=0 }
    fun reset() { clearPages();identity=null;connection=null;lastOpen=0;ticks=0 }
}
