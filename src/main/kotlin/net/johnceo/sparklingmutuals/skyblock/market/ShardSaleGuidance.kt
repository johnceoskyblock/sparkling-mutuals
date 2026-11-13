package net.johnceo.sparklingmutuals.skyblock.market

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigStorage
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.commands.ProfitableCritters
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

/** One client-owned integration and one lazy daemon worker. No collectors while Minecraft is closed. */
object ShardSaleGuidance {
    private val worker by lazy {Executors.newSingleThreadExecutor {task ->Thread(task,"SM shard ranges").apply {isDaemon=true}}}
    private val watchFile by lazy {OrderWatchPersistence(ConfigStorage.directory.resolve("sparkling-mutuals-sale-watches.json"))}
    private val boxFile by lazy {SavedHuntingBoxStore(ConfigStorage.directory.resolve("sparkling-mutuals-box-guidance.json"))}
    private val totalFile by lazy {WholeBoxTotalStore(ConfigStorage.directory.resolve("sparkling-mutuals-box-total.json"))}
    private val total=WholeBoxTotal(BazaarPrices::ensureGuidanceFresh)
    private val watches=SellOfferWatch()
    private val profitSelection by lazy {ProfitGuidanceSelection(ProfitableCritters::cachedSpecies,BazaarPrices::ensureGuidanceFresh)}
    private val market by lazy {ShardMarketService(SaleMarketProducts.ids,System::currentTimeMillis,worker,
        FinanceHistoryTransport(),{job,raw,now ->FinanceHistoryParsing.runtime(raw,now,job.part)},
        MarketHistoryStore(ConfigStorage.directory.resolve("sparkling-mutuals-shard-history.json"),SaleMarketProducts.ids),
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
    },{_,text ->Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component(text));save(force=true)},
        profitProducts={profitSelection.products()},
        ownedProducts={total.ownedSafariProducts(SaleProfileEvidence.scope(account,profile),connection)})
    private var registered=false
    private var loaded=false
    private var connection: Any?=null
    private var account: String?=null
    private var profile: String?=null
    private val profileConfirmation=SaleProfileConfirmation()
    private var menu: AbstractContainerScreen<*>?=null
    private var view: SaleMenuSnapshot?=null
    private var orderView: OrderMarkerSnapshot?=null
    private var tooltipSlot: Slot?=null
    private var savedAt=0L
    private val watchSaver by lazy {WatchSaveQueue(worker,{watchFile.write(it)})}
    private val boxSaver by lazy {WatchSaveQueue(worker,{boxFile.write(it)})}
    private val totalSaver by lazy {WatchSaveQueue(worker,{totalFile.write(it)})}

    fun register() {
        if(registered)return
        registered=true
        ShardSaleSidebar.register()
        BazaarOfferSidebar.register()
        worker.execute {
            val raw=watchFile.read()
            val boxes=boxFile.read()
            val totals=totalFile.read()
            Minecraft.getInstance().execute {
                val now=System.currentTimeMillis()
                watches.restore(raw,now);session.restoreBoxes(boxes,now);session.reset()
                total.restore(boxes,now);total.restore(totals,now);total.reset();loaded=true
            }
        }
        ClientReceiveMessageEvents.GAME.register {message,overlay ->
            val client=Minecraft.getInstance()
            if(!overlay && SkyblockSidebar.inSkyblock(client)) {
                syncIdentity(client)
                val plain=SafariRules.strip(message.string)
                val now=System.currentTimeMillis()
                if(SaleProfileEvidence.changing(plain)) {profile=null;profileConfirmation.reset(now);session.reset();total.reset();view=null;orderView=null}
                if(!ConfigManager.shardSaleGuidance)profileConfirmation.next(false,hasVerifiedProfile(),now)
                val confirmed=if(ConfigManager.shardSaleGuidance)profileConfirmation.accept(message,now)
                    else if(ConfigManager.huntingBoxValue)profileConfirmation.passiveLegacy(message,now)else null
                confirmed?.let {profile=it;session.reset();total.reset();view=null;orderView=null}
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
            profileConfirmation.reset(System.currentTimeMillis())
            profile=null;session.reset();total.reset();view=null;orderView=null;tooltipSlot=null
            connection=client.connection;account=owner
        }
    }
    fun tick(client: Minecraft) {
        syncIdentity(client)
        if(!loaded)return
        val now=System.currentTimeMillis()
        val inSkyblock=client.player!=null && client.connection!=null && SkyblockSidebar.inSkyblock(client)
        val enabled=ConfigManager.shardSaleGuidance
        if(!inSkyblock)profile=null
        if(profileConfirmation.next(enabled && inSkyblock,hasVerifiedProfile(),now))client.connection?.let { handler -> net.johnceo.sparklingmutuals.alerts.AutomatedCommands.send(handler,"profileid") }
        val screen=client.smScreen as? AbstractContainerScreen<*>
        val entries=if((enabled || ConfigManager.huntingBoxValue) && inSkyblock && screen!=null)screen.menu.slots.filter {it.container !is Inventory && it.index in 0..53}.map {slot ->
            val stack=slot.item
            MenuSlot(slot.index,stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id","") ?: "",MarketSlotName.capture(stack.hoverName),
                stack.get(DataComponents.LORE)?.lines().orEmpty().map {it.string})
        }else emptyList()
        total.tick(ConfigManager.huntingBoxValue || enabled,inSkyblock,SaleProfileEvidence.scope(account,profile),client.connection,
            screen?.title?.string.orEmpty(),entries,BazaarPrices.guidanceQuotes,now)
        val recommendations=if(enabled && inSkyblock)market.snapshot(now)else emptyMap()
        val projected=session.tick(enabled,inSkyblock,SaleProfileEvidence.scope(account,profile),client.player?.name?.string.orEmpty(),
            screen?.title?.string.orEmpty(),entries,recommendations,now,client.connection)
        menu=screen
        view=if(screen!=null && projected.isNotEmpty())SaleMenuSnapshot(screen.menu,screen.menu.stateId,
            screen.menu.slots.filter {it.index in projected}.associate {it.index to it.item},projected)else null
        val offers=session.orderAdvice()
        orderView=if(screen!=null && offers.isNotEmpty())OrderMarkerSnapshot(screen.menu,screen.menu.stateId,
            screen.menu.slots.filter {it.index in offers}.associate {it.index to it.item},offers)else null
        tooltipSlot=null
        save()
    }
    private fun save(force: Boolean=false) {
        if(!loaded)return
        val now=System.currentTimeMillis()
        if(!force && now>=savedAt && now-savedAt<30000)return
        savedAt=now
        watchSaver.offer(watches.export(now))
        boxSaver.offer(session.exportBoxes())
        totalSaver.offer(total.export())
    }
    internal fun sidebar(screen: AbstractContainerScreen<*>,now: Long): List<SavedBoxRow> {
        if(!ConfigManager.shardSaleGuidance || menu!==screen || Minecraft.getInstance().smScreen!==screen ||
            !SkyblockSidebar.inSkyblock(Minecraft.getInstance()) || !HuntingBoxShardParsing.isBox(screen.title.string))return emptyList()
        val owned=session.sidebar(now).associateBy {it.shard.productId}
        return ShardSalePresentation.rank(SaleProducts.names.keys.map {id ->
            val row=owned[id] ?: SavedBoxRow(SavedBoxShard(id,0,now),false,Recommendation(id,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE"))
            row.copy(advice=ShardSaleDecisions.apply(row.advice,row.shard.amount.coerceAtLeast(1),BazaarPrices.guidanceQuotes,now))
        })
    }
    internal fun hasVerifiedProfile()=SaleProfileEvidence.scope(account,profile)!=null
    internal fun essence(screen: AbstractContainerScreen<*>,now: Long): Recommendation? {
        if(!ConfigManager.shardSaleGuidance || menu!==screen || Minecraft.getInstance().smScreen!==screen ||
            !SkyblockSidebar.inSkyblock(Minecraft.getInstance()) || !HuntingBoxShardParsing.isBox(screen.title.string))return null
        return session.essence(now)
    }
    internal fun orders(screen: AbstractContainerScreen<*>,now: Long): List<Recommendation> {
        if(!ConfigManager.shardSaleGuidance || menu!==screen || Minecraft.getInstance().smScreen!==screen ||
            !SkyblockSidebar.inSkyblock(Minecraft.getInstance()) || !hasVerifiedProfile() ||
            !SellOfferParsing.isOrders(screen.title.string))return emptyList()
        return orderView?.rows(screen.menu,screen.menu.stateId,{screen.menu.slots.getOrNull(it)?.item},now).orEmpty()
    }
    internal fun totalSummary(screen: AbstractContainerScreen<*>,now: Long): WholeBoxSummary? =
        if(!ConfigManager.huntingBoxValue || menu!==screen || Minecraft.getInstance().smScreen!==screen ||
            !SkyblockSidebar.inSkyblock(Minecraft.getInstance()) || !HuntingBoxShardParsing.isBox(screen.title.string))null
        else total.snapshot(now)
    internal fun sidebarOpening()=session.sidebarOpening
    internal fun profitRankingReady()=session.profitRankingReady()
    private fun advice(screen: AbstractContainerScreen<*>,slot: Slot): Recommendation? {
        if(!ConfigManager.shardSaleGuidance || menu!==screen || Minecraft.getInstance().smScreen!==screen ||
            !SkyblockSidebar.inSkyblock(Minecraft.getInstance()) || !HuntingBoxShardParsing.isBox(screen.title.string))return null
        val now=System.currentTimeMillis()
        val original=view?.advice(screen.menu,screen.menu.stateId,slot.index,slot.item,now) ?: return null
        val amount=session.sidebar(now).firstOrNull {it.shard.productId==original.productId}?.shard?.amount ?: return original
        return ShardSaleDecisions.apply(original,amount,BazaarPrices.guidanceQuotes,now)
    }
    fun draw(screen: AbstractContainerScreen<*>,graphics: GuiGraphicsExtractor,slot: Slot) {
        if(SellOfferParsing.isOrders(screen.title.string)) {
            val client=Minecraft.getInstance()
            if(ConfigManager.shardSaleGuidance && menu===screen && client.smScreen===screen && hasVerifiedProfile() &&
                SkyblockSidebar.inSkyblock(client) &&
                orderView?.needsUnlist(screen.menu,screen.menu.stateId,slot.index,slot.item,System.currentTimeMillis())==true)
                graphics.outline(slot.x-1,slot.y-1,18,18,BazaarUnlistPresentation.COLOR)
            return
        }
        val advice=advice(screen,slot) ?: return
        graphics.outline(slot.x-1,slot.y-1,18,18,ShardSalePresentation.color(advice))
    }
    fun beginTooltip(screen: AbstractContainerScreen<*>,slot: Slot?) {tooltipSlot=slot?.takeIf {menu===screen}}
    fun endTooltip() {tooltipSlot=null}
    fun reset() {
        profileConfirmation.reset(System.currentTimeMillis())
        save(force=true);session.reset();total.reset();profile=null;account=null;connection=null;view=null;orderView=null;menu=null;tooltipSlot=null
    }
}
