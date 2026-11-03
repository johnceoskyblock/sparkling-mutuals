package net.johnceo.sparklingmutuals.skyblock.market

import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FinanceHistoryParsingTest {
    private val now=Instant.parse("2026-11-03T12:05:00Z").toEpochMilli()
    private fun bar(minute: String, close: String="100", high: String=close, low: String=close, open: String=close): String =
        """{"time":${Instant.parse("2026-11-03T$minute:00Z").toEpochMilli()},"open":$open,"high":$high,"low":$low,"close":$close,"volume":999}"""
    private fun response(vararg bars: String)="""[{"result":{"data":{"json":{"bars":[${bars.joinToString(",")}],"noData":false}}}}]"""

    @Test fun `last completed minute close represents a five minute bin without publication claims`() {
        val parsed=FinanceHistoryParsing.parse(response(bar("12:04","110"),bar("12:00")),now)
        assertEquals(1,parsed.size)
        assertEquals(now,parsed.single().at)
        assertEquals(now,parsed.single().observedAt)
        assertEquals(110.0,parsed.single().ask)
        assertEquals(PriceBasis.UNVERIFIED_REFERENCE,parsed.single().basis)
        assertNull(parsed.single().availableAt)
    }
    @Test fun `wick highs and carried prior opens never become target touch samples`() {
        val sample=FinanceHistoryParsing.parse(response(bar("12:04","100","1000","99","2000")),now).single()
        assertEquals(100.0,sample.ask)
    }
    @Test fun `unfinished minute and unfinished five minute bin are excluded`() {
        val raw=response(bar("12:04"),bar("12:05"))
        assertTrue(FinanceHistoryParsing.parse(raw,now-1).isEmpty())
        assertEquals(1,FinanceHistoryParsing.parse(raw,now).size)
    }
    @Test fun `duplicate order does not change the latest close and conflicting closes are discarded`() {
        val older=bar("12:00","100")
        val newer=bar("12:04","110")
        assertEquals(FinanceHistoryParsing.parse(response(older,newer),now),FinanceHistoryParsing.parse(response(newer,older,newer),now))
        assertTrue(FinanceHistoryParsing.parse(response(newer,bar("12:04","120")),now).isEmpty())
    }
    @Test fun `missing intervals are not manufactured by parsing`() {
        val values=FinanceHistoryParsing.parse(response(bar("11:00"),bar("12:04")),now)
        assertEquals(2,values.size)
        assertEquals(60*60_000L,values.last().at-values.first().at)
    }
    @Test fun `invalid close range malformed row and timestamp type are rejected independently`() {
        val raw=response(bar("12:00","0"),bar("12:01","100","99","90"),
            """{"time":"1793707440000","open":100,"high":100,"low":100,"close":100}""",bar("12:04","105"))
        val values=FinanceHistoryParsing.parse(raw,now)
        assertEquals(1,values.size)
        assertEquals(105.0,values.single().ask)
    }
    @Test fun `error malformed or ambiguous batch envelope cannot yield prices`() {
        for(raw in listOf("[]","{}","[{\"error\":{}}]","[null]",response(bar("12:04"))+"garbage",
            response(bar("12:04")).replace("false","true"),response(bar("12:04")).dropLast(1)+",{}]")) {
            assertTrue(FinanceHistoryParsing.parse(raw,now).isEmpty(),raw)
        }
    }
    @Test fun `response bound counts utf8 bytes before json parsing`() {
        val raw=response(bar("12:04")).replace("\"noData\":false","\"noData\":false,\"ignored\":\"${"é".repeat(1_050_000)}\"")
        assertTrue(FinanceHistoryParsing.parse(raw,now).isEmpty())
    }
    @Test fun `null volume candles mark suspect sampling and cannot fill missing history`() {
        val suspect=bar("12:04","110").replace("\"volume\":999","\"volume\":null")
        assertTrue(FinanceHistoryParsing.parse(response(suspect),now).isEmpty())
        val earlier=bar("12:00","100")
        assertEquals(100.0,FinanceHistoryParsing.parse(response(earlier,suspect),now).single().ask)
    }
}
