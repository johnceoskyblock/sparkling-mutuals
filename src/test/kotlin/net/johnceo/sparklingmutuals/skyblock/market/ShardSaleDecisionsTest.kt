package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShardSaleDecisionsTest {
    @Test fun `display ranks holding proceeds first without mixing in unowned per shard prices`() {
        fun row(id: String,amount: Long,value: Double?)=SavedBoxRow(SavedBoxShard(id,amount,0),true,
            Recommendation(id,AdviceState.UNKNOWN,null,0,"",sale=value?.let {
                ShardSaleComparison(ShardSaleAction.OFFER,it,it,it,null,null,"")
            }))
        val rows=listOf(row("SHARD_MACAW",0,100000.0),row("SHARD_FOXTROT",10,1000.0),
            row("SHARD_BLUEBIRD",20,5000.0),row("SHARD_HONEYBUG",30,null))
        assertEquals(listOf("SHARD_BLUEBIRD","SHARD_FOXTROT","SHARD_HONEYBUG","SHARD_MACAW"),
            ShardSalePresentation.rank(rows).map {it.shard.productId})
        assertEquals("Unknown",ShardSalePresentation.volumeLabel(null))
        assertEquals("Low",ShardSalePresentation.volumeLabel(.49))
        assertEquals("Typical",ShardSalePresentation.volumeLabel(1.0))
        assertEquals("High",ShardSalePresentation.volumeLabel(1.5))
    }
    @Test fun `low activity favours salvage for a small premium but not a large premium`() {
        fun decision(ask: Double,week: Double)=ShardSaleDecisions.compare(ask,80.0,100.0,1,100,week,30_000.0)
        assertEquals(ShardSaleAction.SALVAGE,decision(120.0,14_000.0).action)
        assertEquals(ShardSaleAction.OFFER,decision(120.0,40_000.0).action)
        assertEquals(ShardSaleAction.OFFER,decision(300.0,14_000.0).action)
        assertEquals(14_000.0/30_000.0,decision(120.0,14_000.0).ratio)
    }
    @Test fun `holding more than a day of activity increases the cost of a modest premium`() {
        assertEquals(ShardSaleAction.SALVAGE,ShardSaleDecisions.compare(140.0,80.0,100.0,1,1000,700.0,500.0).action)
        assertEquals(ShardSaleAction.OFFER,ShardSaleDecisions.compare(140.0,80.0,100.0,1,10,700.0,500.0).action)
        assertEquals(ShardSaleAction.INSTANT,ShardSaleDecisions.compare(105.0,100.0,50.0,1,10,100.0,1000.0).action)
    }
    @Test fun `salvage yields and fresh catalogue average apply without price history`() {
        val now=MarketFixtures.NOW
        val quotes=SaleProducts.names.keys.associateWith {MarketQuote(it,120.0,80.0,now,14000.0)}+
            (SaleMarketProducts.ESSENCE to MarketQuote(SaleMarketProducts.ESSENCE,100.0,90.0,now))
        val snapshot=QuoteSnapshot(now,quotes,emptyMap())
        fun rec(id: String)=Recommendation(id,AdviceState.UNKNOWN,null,0,"HISTORY_COVERAGE")
        val common=ShardSaleDecisions.apply(rec("SHARD_FOXTROT"),100,snapshot,now)
        val legendary=ShardSaleDecisions.apply(rec("SHARD_DOOMSPIRAL"),100,snapshot,now)
        assertEquals(10_000.0,common.sale!!.salvage)
        assertEquals(160_000.0,legendary.sale!!.salvage)
        assertEquals(14_000.0,common.sale!!.average)
        assertNull(ShardSaleDecisions.apply(rec("SHARD_FOXTROT"),100,snapshot,now+180001).sale)
        assertNull(ShardSaleDecisions.apply(rec("SHARD_FOXTROT"),100,snapshot.copy(quotes=quotes-"SHARD_WUMPA"),now).sale!!.average)
    }
    @Test fun `API activity excludes live sell offers and missing activity stays unknown`() {
        val now=MarketFixtures.NOW
        val product="""{"buy_summary":[{"pricePerUnit":120,"amount":10,"orders":1}],"sell_summary":[{"pricePerUnit":80,"amount":10,"orders":1}],"quick_status":{"buyVolume":5000,"buyMovingWeek":19000,"sellPrice":80,"buyPrice":120}}"""
        val raw="""{"success":true,"lastUpdated":$now,"products":{"SHARD_FOXTROT":$product}}"""
        assertEquals(14_000.0,BazaarQuoteParsing.parse(raw,now)!!.quotes.getValue("SHARD_FOXTROT").weeklyBuys)
        assertNull(BazaarQuoteParsing.parse(raw.replace("\"buyVolume\":5000,",""),now)!!.quotes.getValue("SHARD_FOXTROT").weeklyBuys)
    }
}
