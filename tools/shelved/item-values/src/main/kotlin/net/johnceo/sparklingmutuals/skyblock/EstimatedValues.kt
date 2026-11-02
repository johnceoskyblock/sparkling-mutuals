/* Adapted from SkyHanni's EstimatedItemValue and ChestValue, LGPL-2.1. See licenses/SkyHanni-Value-NOTICE.txt. */
package net.johnceo.sparklingmutuals.skyblock


import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SettingsPalette
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.mixin.ContainerScreenAccessor
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToLong

internal object StorageValueRules {
    fun accepts(title: String): Boolean {
        val name=SafariRules.strip(title).trim()
        return name in setOf("Chest","Large Chest","Ender Chest","Personal Vault","Chest Storage","Wood Chest+") ||
            Regex("Ender Chest \\(\\d+/\\d+\\)").matches(name) ||
            Regex(".*Backpack.*Slot #\\d+.*").matches(name)
    }
}

/** Screen-local estimates; never performs a lookup for an individual item or inventory slot. */
object EstimatedValues {
    private data class Entry(val stack: ItemStack,val estimate: ItemEstimate,val slots: List<Int>)
    private val cache=object: LinkedHashMap<ValueItem,ItemEstimate>(128,.75f,true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ValueItem,ItemEstimate>?)=size>256
    }
    private var marketRevision=-1
    private var bazaar: Any?=null
    private var screen: Screen?=null
    private var chest=emptyList<Entry>()
    private var item: Entry?=null
    private var overItemPanel=false
    private var itemScroll=0
    private var chestScroll=0
    private var ticks=0
    private data class Area(val x: Int,val y: Int,val scale: Float,val rows: Int) {
        val height get()=63+rows*12
        fun contains(mx: Double,my: Double)=mx>=x && mx<x+WIDTH*scale && my>=y && my<y+height*scale
    }
    private const val WIDTH=238
    private fun area(screen: Screen,left: Boolean): Area {
        val access=screen as ContainerScreenAccessor
        val start=access.`sm$getLeftPos`()
        val end=start+access.`sm$getImageWidth`()
        val free=(if(left) start-10 else screen.width-end-10).coerceAtLeast(30)
        val scale=minOf(1f,free/WIDTH.toFloat()).coerceAtLeast(.15f)
        val rows=minOf(15,((screen.height/scale-72)/12).toInt()).coerceAtLeast(3)
        val x=if(left) (start-5-WIDTH*scale).toInt() else end+5
        return Area(x,((screen.height-(63+rows*12)*scale)/2).toInt().coerceAtLeast(2),scale,rows)
    }
    private fun enabled()=SkyblockSidebar.inSkyblock(Minecraft.getInstance())
    fun register() {
        ScreenEvents.AFTER_INIT.register { _,current,_,_ ->
            if(current is AbstractContainerScreen<*>) {
                ScreenEvents.afterExtract(current).register { s,g,mx,my,_ -> render(s,g,mx,my) }
                ScreenMouseEvents.allowMouseScroll(current).register { s,mx,my,_,vertical ->
                    if(!enabled()) true else {
                        val direction=if(vertical>0) -1 else if(vertical<0) 1 else 0
                        when {
                            ConfigManager.estimatedItemValue && item!=null && area(s,true).contains(mx,my) -> {
                                itemScroll=(itemScroll+direction).coerceIn(0,(item!!.estimate.parts.size-area(s,true).rows).coerceAtLeast(0));false
                            }
                            ConfigManager.estimatedChestValue && chest.isNotEmpty() && area(s,false).contains(mx,my) -> {
                                chestScroll=(chestScroll+direction).coerceIn(0,(chest.size-area(s,false).rows).coerceAtLeast(0));false
                            }
                            else -> true
                        }
                    }
                }
            }
        }
    }
    private fun read(stack: ItemStack): ValueItem? {
        if(stack.isEmpty) return null
        val name=SafariRules.strip(stack.hoverName.string).trim()
        if(name.isEmpty()) return null
        val tag=stack.get(DataComponents.CUSTOM_DATA)?.copyTag()
        val extra=tag?.let { NbtOps.INSTANCE.convertTo(com.mojang.serialization.JsonOps.INSTANCE,it).asJsonObject } ?: com.google.gson.JsonObject()
        val id=extra.str("id").ifBlank { BuiltInRegistries.ITEM.getKey(stack.item).path.uppercase(java.util.Locale.ROOT) }
        if(id in setOf("STAINED_GLASS_PANE","BLACK_STAINED_GLASS_PANE","GRAY_STAINED_GLASS_PANE") && name.isBlank()) return null
        return ValueItem(id,name,stack.count,extra,stack.get(DataComponents.LORE)?.lines().orEmpty().map { SafariRules.strip(it.string) })
    }
    private fun estimate(stack: ItemStack): ItemEstimate? = runCatching {
        val value=read(stack) ?: return null
        cache.getOrPut(value) { ItemValuation(ValueDefinitions.bundled,ValueMarket.prices,BazaarPrices.cachedPrices).estimate(value) }
    }.getOrNull()
    fun tick(client: Minecraft) {
        val current=client.smScreen as? AbstractContainerScreen<*>
        if(!enabled() || current==null || (!ConfigManager.estimatedItemValue && !ConfigManager.estimatedChestValue)) {
            if(screen!=null) { cache.clear();screen=null;chest=emptyList();item=null;overItemPanel=false }
            return
        }
        if(screen!==current) { screen=current;chest=emptyList();item=null;overItemPanel=false;itemScroll=0;chestScroll=0 }
        ValueMarket.ensureFresh()
        if(marketRevision!=ValueMarket.revision || bazaar!==BazaarPrices.cachedPrices) {
            cache.clear();marketRevision=ValueMarket.revision;bazaar=BazaarPrices.cachedPrices
        }
        val hovered=(current as ContainerScreenAccessor).`sm$getHoveredSlot`()?.item
        val previous=item?.estimate?.item
        item=when {
            !ConfigManager.estimatedItemValue -> null
            hovered!=null -> estimate(hovered)?.let { Entry(hovered.copy(),it,emptyList()) }
            overItemPanel -> item
            else -> null
        }
        if(previous!=item?.estimate?.item) itemScroll=0
        if(++ticks%5!=0) return
        chest=if(ConfigManager.estimatedChestValue && StorageValueRules.accepts(current.title.string)) {
            current.menu.slots.filter { it.container !is Inventory }.mapNotNull { slot ->
                estimate(slot.item)?.let { Entry(slot.item.copy(),it,listOf(slot.index)) }
            }.groupBy { it.estimate.item.copy(count=1) }.values.map { group ->
                val first=group.first();first.copy(estimate=first.estimate.copy(item=first.estimate.item.copy(count=group.sumOf { it.estimate.item.count })),slots=group.flatMap { it.slots })
            }.sortedByDescending { it.estimate.total }
        } else emptyList()
    }
    private fun money(value: Double)=BazaarPrices.format(value.roundToLong())
    private fun render(s: Screen,g: GuiGraphicsExtractor,mx: Int,my: Int) {
        if(screen!==s || !enabled()) return
        overItemPanel=item!=null && area(s,true).contains(mx.toDouble(),my.toDouble())
        if(ConfigManager.estimatedItemValue) item?.let { entry ->
            panel(s,g,mx,my,true,"Estimated Item Value",entry.estimate.item.name,
                entry.estimate.parts.map { part -> part.label+(if(part.amount!=1.0) " ×${money(part.amount)}" else "") to (part.coins?.let(::money) ?: "?") },
                itemScroll,listOf(entry.estimate),entry.estimate.parts.map { part -> listOf(part.product,"Price: ${part.quote?.source ?: "Unavailable"}","Applied: ${part.amount}") })
        }
        if(ConfigManager.estimatedChestValue && StorageValueRules.accepts(s.title.string) && chest.isNotEmpty()) {
            panel(s,g,mx,my,false,"Estimated Chest Value","${chest.sumOf { it.estimate.item.count }} items · Scroll list",
                chest.map { it.estimate.item.name+" ×${it.estimate.item.count}" to if(it.estimate.available) money(it.estimate.total) else "?" },
                chestScroll,chest.map { it.estimate },chest.map { entry -> details(entry.estimate) },chest)
        }
    }
    private fun details(estimate: ItemEstimate): List<String> = listOf(estimate.item.name,"Replacement estimate: ${money(estimate.total)} coins")+
        estimate.parts.map { "${it.label}: ${it.coins?.let(::money) ?: "?"} / item" }+estimate.notes
    private fun panel(s: Screen,g: GuiGraphicsExtractor,mx: Int,my: Int,left: Boolean,title: String,subtitle: String,
        rows: List<Pair<String,String>>,scroll: Int,estimates: List<ItemEstimate>,tips: List<List<String>>,entries: List<Entry> = emptyList()) {
        val client=Minecraft.getInstance();val a=area(s,left)
        val offset=scroll.coerceIn(0,(rows.size-a.rows).coerceAtLeast(0))
        val lx=(mx-a.x)/a.scale;val ly=(my-a.y)/a.scale
        val hover=if(lx>=0 && lx<WIDTH && ly>=30 && ly<30+a.rows*12) offset+((ly-30)/12).toInt() else -1
        g.pose().pushMatrix();g.pose().translate(a.x.toFloat(),a.y.toFloat());g.pose().scale(a.scale,a.scale)
        g.fill(0,0,WIDTH,a.height,0xED101015.toInt());g.fill(0,0,WIDTH,1,SettingsPalette.accent)
        fun text(value: String,y: Int,color: Int=-1)=g.text(client.font,value,6,y,color,true)
        text(title,6,SettingsPalette.accent);text(client.font.plainSubstrByWidth(subtitle,WIDTH-12),18,0xFFBBBBBB.toInt())
        rows.drop(offset).take(a.rows).forEachIndexed { i,(name,price) ->
            val y=30+i*12
            if(offset+i==hover) g.fill(2,y-1,WIDTH-2,y+11,0x55444466)
            text(client.font.plainSubstrByWidth(name,WIDTH-20-client.font.width(price)),y)
            g.text(client.font,price,WIDTH-6-client.font.width(price),y,0xFFFFCC77.toInt(),true)
        }
        val partial=estimates.any { it.partial || !it.available }
        val base=34+a.rows*12
        text("${if(partial) "Known total" else "Total"}: ${money(estimates.sumOf { it.total })} coins",base,SettingsPalette.accent)
        text(if(partial) "Incomplete estimate · hover for details" else "Base + upgrades · not sale proceeds",base+12,0xFFAAAAAA.toInt())
        g.pose().popMatrix()
        if(hover in rows.indices) {
            val lines=tips.getOrNull(hover).orEmpty()
            g.setComponentTooltipForNextFrame(client.font,lines.map(Component::literal),mx,my)
            val access=s as ContainerScreenAccessor
            entries.getOrNull(hover)?.slots?.forEach { index ->
                (s as AbstractContainerScreen<*>).menu.slots.firstOrNull { it.index==index }?.let { slot ->
                    val x=access.`sm$getLeftPos`()+slot.x;val y=access.`sm$getTopPos`()+slot.y
                    g.fill(x,y,x+16,y+16,0x66FFD066)
                }
            }
        } else if(a.contains(mx.toDouble(),my.toDouble()) && ly>=base) {
            g.setComponentTooltipForNextFrame(client.font,(listOf("Market prices cached for three hours.","Estimates include installed upgrades.","Not guaranteed resale value.")+estimates.flatMap { it.notes }.distinct()).map(Component::literal),mx,my)
        }
    }
}
