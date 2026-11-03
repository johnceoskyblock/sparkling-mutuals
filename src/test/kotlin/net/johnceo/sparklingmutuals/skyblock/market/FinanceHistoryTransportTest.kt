package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonParser
import java.net.URLDecoder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FinanceHistoryTransportTest {
    @Test fun `public requests use exact allowlisted symbols closed UTC ranges and bounded resolutions`() {
        val now=MarketFixtures.NOW
        for(part in HistoryPart.entries) {
            val uri=FinanceHistoryTransport.uri(HistoryJob("SHARD_HIDEONWALL",part),now)
            assertEquals("https",uri.scheme);assertEquals("skyblock.finance",uri.host)
            val input=URLDecoder.decode(uri.rawQuery.substringAfter("&input="),Charsets.UTF_8)
            val json=JsonParser.parseString(input).asJsonObject.getAsJsonObject("0").getAsJsonObject("json")
            assertEquals("bz!a!SHARD_HIDEONWALL",json["symbol"].asString)
            assertEquals(if(part==HistoryPart.RECENT)"1" else "15",json["resolution"].asString)
            assertEquals(now/60000*60,json["to"].asLong)
            assertEquals(if(part==HistoryPart.RECENT)2*86400L else 8*86400L,json["to"].asLong-json["from"].asLong)
        }
        assertThrows(IllegalArgumentException::class.java) {FinanceHistoryTransport.uri(HistoryJob("../../secret",HistoryPart.RECENT),now)}
    }
    @Test fun `retry after supports seconds and HTTP date with sane saturation`() {
        assertEquals(120000,FinanceHistoryTransport.retryAfter("120",1000))
        assertEquals(60000,FinanceHistoryTransport.retryAfter("Thu, 01 Jan 1970 00:01:00 GMT",0))
        assertNull(FinanceHistoryTransport.retryAfter("bad",0))
        assertNull(FinanceHistoryTransport.retryAfter("-1",0))
    }
}
