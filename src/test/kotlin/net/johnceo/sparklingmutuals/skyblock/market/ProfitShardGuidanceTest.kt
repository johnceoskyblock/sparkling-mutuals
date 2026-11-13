package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.commands.ProfitableCritters
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProfitShardGuidanceTest : ProfitableEspPriceFixture() {
    private val now=MarketFixtures.NOW
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private var bootstrap=0
    private val selection=ProfitGuidanceSelection(ProfitableCritters::cachedSpecies,{bootstrap++})
    private val demands=mutableListOf<Set<String>>()
    private val ledger=SavedHuntingBoxLedger(SaleProducts.names.keys).also {
        it.select(scope,null);it.observe(SaleProducts.names.keys.map {id->BoxShard(10,id,20)},now-1000)
    }
    private fun controller()=SaleGuidanceSession(SellOfferWatch(),{_,_,ids,_->demands.add(ids)},{_,_->},ledger,
        profitProducts={selection.products()},ownedProducts={ledger.rows().map {it.first.productId}.toSet()})
    private fun expected()=ProfitableCritters.cachedTop().map {BazaarPrices.shardId(it.name)}.toSet()
    private fun advice()=SaleProducts.names.keys.associateWith {Recommendation(it,AdviceState.SELL,100.0,now,"EXPERIMENTAL_RANGE")}
    private fun slots(ids: List<String>)=ids.mapIndexed {i,id->MenuSlot(if(i<7)10+i else 19+i-7,id,
        "${SaleProducts.names.getValue(id)} Shard",listOf("Owned: 20 Shards"))}
    private fun tick(c: SaleGuidanceSession,ids: List<String> = emptyList())=
        c.tick(true,true,scope,"Local","Hunting Box",slots(ids),advice(),now)

    @Test fun `box slots sidebar and demand include all Safari shards`() {
        prices(SaleProducts.names.keys.associateWith {doubleArrayOf(100.0,100.0)})
        val c=controller();val selected=expected();assertEquals(10,selected.size)
        for(page in SaleProducts.names.keys.chunked(14))
            assertEquals(page.toSet(),tick(c,page).values.map {it.productId}.toSet())
        assertEquals(SaleProducts.names.keys,c.sidebar(now).map {it.shard.productId}.toSet())
        assertEquals(SaleMarketProducts.ids,demands.last());assertEquals(37,ledger.rows().size)
        assertEquals(0,bootstrap)
    }
    @Test fun `changed prices ticket and sell preference update selection and retain cached ownership`() {
        val c=controller()
        prices(SaleProducts.names.keys.associateWith {when(it) {
            "SHARD_WUMPA"->doubleArrayOf(1e9,1.0)
            "SHARD_NOZZLENOSE"->doubleArrayOf(1.0,1e9)
            else->doubleArrayOf(100.0,100.0)
        }}+(BazaarPrices.SAFARI_ESSENCE to doubleArrayOf(50.0,50.0)))
        for(ticket in 0..3)for(insta in listOf(false,true)) {
            ConfigManager.profitsTicketMode=ticket;ConfigManager.runCoinsInstaSell=insta
            tick(c)
            assertEquals(SaleProducts.names.keys,c.sidebar(now).map {it.shard.productId}.toSet())
            assertTrue(if(insta)"SHARD_WUMPA" in demands.last()else "SHARD_NOZZLENOSE" in demands.last())
        }
        assertEquals(37,ledger.rows().size)
    }
    @Test fun `empty cached ranking does not limit box eligibility`() {
        prices(emptyMap());val c=controller()
        assertEquals(setOf("SHARD_BLUEBIRD"),tick(c,listOf("SHARD_BLUEBIRD")).values.map {it.productId}.toSet())
        assertEquals(37,c.sidebar(now).size);assertEquals(SaleMarketProducts.ids,demands.last());assertEquals(1,bootstrap)
        repeat(50){c.sidebar(now)};assertEquals(1,bootstrap,"Rendering does not request quotes")
        prices(mapOf("SHARD_BLUEBIRD" to doubleArrayOf(100.0,100.0)))
        tick(c);assertEquals(SaleMarketProducts.ids,demands.last())
        assertEquals(37,c.sidebar(now).size)
    }
    @Test fun `partial rankings retain Rockmite and Snoozle exactly as command lists them`() {
        prices(listOf("SHARD_ROCKMITE","SHARD_SNOOZLE").associateWith {doubleArrayOf(100.0,100.0)})
        val c=controller();tick(c)
        assertEquals(setOf("SHARD_ROCKMITE","SHARD_SNOOZLE"),expected())
        assertEquals(SaleProducts.names.keys,c.sidebar(now).map {it.shard.productId}.toSet())
    }
    @Test fun `owned watch retains its demand outside the top ten box selection`() {
        val outside=(SaleProducts.names.keys-expected()).first();val c=controller()
        val order=MenuSlot(10,outside,"§6§lSELL §6${SaleProducts.names.getValue(outside)} Shard",
            listOf("Offer amount: 2x","Price per unit: 100 coins"))
        c.tick(true,true,scope,"Local","Your Bazaar Orders",listOf(order),advice(),now)
        assertEquals(setOf(outside),demands.last());assertTrue(c.sidebar(now).isEmpty())
        tick(c);assertEquals(SaleMarketProducts.ids,demands.last())
        assertTrue(c.sidebar(now).any {it.shard.productId==outside})
    }
}
