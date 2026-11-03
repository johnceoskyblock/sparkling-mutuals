package net.johnceo.sparklingmutuals.skyblock.market

import java.net.URLDecoder
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariEssenceGuidanceTest {
    private val now=MarketFixtures.NOW
    private val id="ESSENCE_SAFARI"
    private val connection=Any()
    @Test fun `essence row and demand exist without ownership ranking or verified profile and stop outside box`() {
        val demands=mutableListOf<Set<String>>()
        val session=SaleGuidanceSession(SellOfferWatch(),{_,_,ids,_->demands.add(ids)},{_,_->})
        fun tick(title: String="Hunting Box",enabled: Boolean=true,advice: Map<String,Recommendation> = emptyMap())=
            session.tick(enabled,true,null,"Player",title,emptyList(),advice,now,connection)
        tick();assertEquals(setOf(id),demands.last());assertEquals(AdviceState.UNKNOWN,session.essence(now)!!.state)
        assertTrue(session.sidebar(now).isEmpty())
        tick(advice=mapOf(id to Recommendation(id,AdviceState.SELL,100.0,now,"EXPERIMENTAL_RANGE",100.0,"range")))
        assertEquals(AdviceState.SELL,session.essence(now)!!.state)
        assertEquals(AdviceState.UNKNOWN,session.essence(now+180001)!!.state)
        tick("Your Skills");assertNull(session.essence(now));assertTrue(demands.last().isEmpty())
        tick(enabled=false);assertNull(session.essence(now));assertTrue(demands.last().isEmpty())
    }
    @Test fun `Safari Essence alone extends history transport cache and service without admitting unrelated products`() {
        val uri=FinanceHistoryTransport.uri(HistoryJob(id,HistoryPart.RECENT),now)
        assertTrue(URLDecoder.decode(uri.rawQuery,Charsets.UTF_8).contains("bz!a!ESSENCE_SAFARI"))
        assertThrows(IllegalArgumentException::class.java) {FinanceHistoryTransport.uri(HistoryJob("ESSENCE_WITHER",HistoryPart.RECENT),now)}
        val folder=Files.createTempDirectory("essence-history")
        val store=MarketHistoryStore(folder.resolve("history.json"),SaleProducts.names.keys+id)
        val at=now/ShardHistoryParsing.STEP*ShardHistoryParsing.STEP
        assertTrue(store.write(mapOf(id to listOf(PriceSample(at,100.0,ShardHistoryParsing.STEP))),now))
        assertEquals(setOf(id),store.read(now).keys)
        assertThrows(IllegalArgumentException::class.java) {MarketHistoryStore(folder.resolve("wrong.json"),setOf("ESSENCE_WITHER"))}
        assertThrows(IllegalArgumentException::class.java) {MarketHistoryStore(folder.resolve("extra.json"),SaleProducts.names.keys+"SHARD_EXTRA")}
        ShardMarketService(SaleProducts.names.keys+id,{now},{it.run()},HistoryTransport {_,_->MarketCancel {}},
            {_,_,_->emptyList()},store,{null},{},{null})
    }
    @Test fun `box layout reserves a fixed essence line while Bazaar header and row hit tests stay unchanged`() {
        val box=ShardSaleSidebarLayout.boxBounds(960,540,600,37)!!
        assertNull(box.rowAt(box.x+8.0,box.y+18.0*box.scale))
        assertEquals(0,box.rowAt(box.x+8.0,box.y+32.0*box.scale))
        val orders=ShardSaleSidebarLayout.leftBounds(960,540,360,37)!!
        assertEquals(0,orders.rowAt(orders.x+8.0,orders.y+18.0*orders.scale))
    }
    @Test fun `essence retains display HOLD through the neutral range without entering shard ownership`() {
        val display=ShardDisplayAdvice()
        fun project(ask: Double,at: Long)=display.project(mapOf(id to Recommendation(id,AdviceState.SELL,100.0,at,
            "EXPERIMENTAL_RANGE",ask,"range"))).getValue(id).state
        assertEquals(AdviceState.SELL,project(100.0,now))
        assertEquals(AdviceState.HOLD,project(98.0,now+1))
        assertEquals(AdviceState.HOLD,project(99.0,now+2))
        assertEquals(AdviceState.SELL,project(100.0,now+3))
    }
}
