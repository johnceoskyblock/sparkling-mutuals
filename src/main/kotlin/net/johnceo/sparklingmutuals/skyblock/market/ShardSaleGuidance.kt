package net.johnceo.sparklingmutuals.skyblock.market

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** One client-owned integration and one lazy daemon worker. No collectors while Minecraft is closed. */
object ShardSaleGuidance {
    private val worker by lazy {Executors.newSingleThreadExecutor {task ->Thread(task,"SM shard ranges").apply {isDaemon=true}}}
    private val watchFile by lazy {OrderWatchPersistence(FabricLoader.getInstance().configDir.resolve("sparkling-mutuals-sale-watches.json"))}
    private val watches=SellOfferWatch()
    private val market by lazy {ShardMarketService(SaleProducts.names.keys,System::currentTimeMillis,worker,
        FinanceHistoryTransport(),{job,raw,now ->FinanceHistoryParsing.runtime(raw,now,job.part)},
        MarketHistoryStore(FabricLoader.getInstance().configDir.resolve("sparkling-mutuals-shard-history.json"),SaleProducts.names.keys),
        {BazaarPrices.guidanceQuotes},{BazaarPrices.ensureGuidanceFresh()},{null},experimental=true)}
    private var lastDemand: List<Any>?=null
    private var demandAt=0L
    private val session=SaleGuidanceSession(watches,{enabled,skyblock,products,generation ->
        val signature=listOf(enabled,skyblock,products,generation)
        val now=System.currentTimeMillis()
        if(signature!=lastDemand || now-demandAt>=1000 || now<demandAt) {
            lastDemand=signature;demandAt=now
            market.updateDemand(enabled,skyblock,products,generation)
        }
    },{_,text ->Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component(text));save(force=true)})
    private var registered=false
    private var loaded=false
    private var connection: Any?=null
    private var account: String?=null
    private var profile: String?=null
    private var menu: AbstractContainerScreen<*>?=null
    private var view: SaleMenuSnapshot?=null
    private var tooltipSlot: Slot?=null
    private var savedAt=0L
    private var savedText=""
    private val pendingSave=AtomicReference<String?>()
    private val saving=AtomicBoolean(false)

    fun register() {
        if(registered)return
        registered=true
        worker.execute {
            val raw=watchFile.read()
            Minecraft.getInstance().execute {watches.restore(raw,System.currentTimeMillis());session.reset();loaded=true}
        }
        ClientReceiveMessageEvents.GAME.register {message,overlay ->
            val client=Minecraft.getInstance()
            if(!overlay && SkyblockSidebar.inSkyblock(client)) {
                syncIdentity(client)
                val plain=SafariRules.strip(message.string)
                if(SaleProfileEvidence.changing(plain)) {profile=null;session.reset();view=null}
                SaleProfileEvidence.profile(plain)?.let {profile=it;session.reset();view=null}
            }
        }
        ItemTooltipCallback.EVENT.register {stack,_,_,lines ->
            val screen=menu ?: return@register
            val slot=tooltipSlot ?: return@register
            if(stack!==slot.item)return@register
            val advice=advice(screen,slot) ?: return@register
            lines.add(Component.empty())
            ShardSalePresentation.tooltip(advice,System.currentTimeMillis()).forEach {lines.add(Component.literal(it).withStyle(net.minecraft.ChatFormatting.GRAY))}
        }
    }
    private fun syncIdentity(client: Minecraft) {
        val owner=client.player?.uuid?.toString()
        if(connection!==client.connection || owner!=account) {
            profile=null;session.reset();view=null;tooltipSlot=null
            connection=client.connection;account=owner
        }
    }
    fun tick(client: Minecraft) {
        syncIdentity(client)
        if(!loaded)return
        val now=System.currentTimeMillis()
        val inSkyblock=client.player!=null && client.connection!=null && SkyblockSidebar.inSkyblock(client)
        val enabled=ConfigManager.shardSaleGuidance
        val screen=client.smScreen as? AbstractContainerScreen<*>
        val entries=if(enabled && inSkyblock && screen!=null)screen.menu.slots.filter {it.container !is Inventory && it.index in 0..53}.map {slot ->
            val stack=slot.item
            MenuSlot(slot.index,stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","") ?: "",stack.hoverName.string,
                stack.get(DataComponents.LORE)?.lines().orEmpty().map {it.string})
        }else emptyList()
        val recommendations=if(enabled && inSkyblock)market.snapshot(now)else emptyMap()
        val projected=session.tick(enabled,inSkyblock,SaleProfileEvidence.scope(account,profile),client.player?.name?.string.orEmpty(),
            screen?.title?.string.orEmpty(),entries,recommendations,now)
        menu=screen
        view=if(screen!=null && projected.isNotEmpty())SaleMenuSnapshot(screen.menu,screen.menu.stateId,
            screen.menu.slots.filter {it.index in projected}.associate {it.index to it.item},projected)else null
        tooltipSlot=null
        save()
    }
    private fun save(force: Boolean=false) {
        if(!loaded)return
        val now=System.currentTimeMillis()
        if(!force && now>=savedAt && now-savedAt<30000)return
        savedAt=now
        val raw=watches.export(now)
        if(raw==savedText)return
        savedText=raw;pendingSave.set(raw)
        if(!saving.compareAndSet(false,true))return
        worker.execute {
            do {
                pendingSave.getAndSet(null)?.let {watchFile.write(it)}
                saving.set(false)
            }while(pendingSave.get()!=null && saving.compareAndSet(false,true))
        }
    }
    private fun advice(screen: AbstractContainerScreen<*>,slot: Slot): Recommendation? {
        if(!ConfigManager.shardSaleGuidance || menu!==screen || Minecraft.getInstance().smScreen!==screen)return null
        return view?.advice(screen.menu,screen.menu.stateId,slot.index,slot.item,System.currentTimeMillis())
    }
    fun draw(screen: AbstractContainerScreen<*>,graphics: GuiGraphicsExtractor,slot: Slot) {
        val advice=advice(screen,slot) ?: return
        graphics.outline(slot.x-1,slot.y-1,18,18,ShardSalePresentation.color(advice.state))
    }
    fun beginTooltip(screen: AbstractContainerScreen<*>,slot: Slot?) {tooltipSlot=slot?.takeIf {menu===screen}}
    fun endTooltip() {tooltipSlot=null}
    fun reset() {
        save(force=true);session.reset();profile=null;account=null;connection=null;view=null;menu=null;tooltipSlot=null
    }
}
