package net.johnceo.sparklingmutuals.skyblock.market

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MarketHistoryStoreTest {
    @TempDir lateinit var directory: Path
    private val now=1_793_682_000_000L/300_000*300_000
    private val id="SHARD_HIDEONWALL"
    private val other="SHARD_BLOODBAT"
    private fun store(version: Int=1)=MarketHistoryStore(directory.resolve("prices.json"),setOf(id,other),version)
    private fun point(at: Long=now,price: Double=100.0)=PriceSample(at,price,300_000,at-60_000,PriceBasis.VERIFIED_BEST_ASK,at)

    @Test fun `round trip preserves source evidence and excludes unknown products`() {
        assertTrue(store().write(mapOf(id to listOf(point()),"UNRELATED" to listOf(point())),now))
        val read=store().read(now)
        assertEquals(setOf(id),read.keys)
        assertEquals(listOf(point()),read[id])
        assertFalse(Files.readString(directory.resolve("prices.json")).contains("UNRELATED"))
    }
    @Test fun `expired future unaligned and contradictory samples never survive persistence`() {
        val samples=listOf(point(now-8*86_400_000L),point(now+300_000),point(now+1),
            point(now-300_000,100.0),point(now-300_000,110.0),point())
        assertTrue(store().write(mapOf(id to samples),now))
        assertEquals(listOf(point()),store().read(now)[id])
    }
    @Test fun `fine and coarse cache sizes remain bounded and reads prune with time`() {
        val fine=(0..3000).map {point(now-it*300_000L)}
        val coarse=(0..120).map {PriceSample(now/7_200_000*7_200_000-it*7_200_000L,100.0,7_200_000)}
        assertTrue(store().write(mapOf(id to fine+coarse),now))
        val read=store().read(now).getValue(id)
        assertEquals(2304,read.count {it.resolutionMillis==300_000L})
        assertTrue(read.count {it.resolutionMillis==7_200_000L}<=96)
        assertTrue(store().read(now+9*86_400_000L).isEmpty())
    }
    @Test fun `matching duplicate prices cannot upgrade unknown provenance or delivery time`() {
        assertTrue(store().write(mapOf(id to listOf(point(),point().copy(basis=PriceBasis.UNVERIFIED_REFERENCE,availableAt=null))),now))
        val read=store().read(now).getValue(id).single()
        assertEquals(PriceBasis.UNVERIFIED_REFERENCE,read.basis)
        assertNull(read.availableAt)
    }
    @Test fun `different schema model and malformed disk content are rejected`() {
        assertTrue(store().write(mapOf(id to listOf(point())),now))
        assertTrue(store(version=2).read(now).isEmpty())
        val path=directory.resolve("prices.json")
        val valid=Files.readString(path)
        for(raw in listOf("{",valid.replace("\"schema\":1","\"schema\":2"),
            valid.replace("\"schema\":1","\"schema\":1.5"),
            valid.replace("\"schema\":1","\"schema\":\"1\""),valid.dropLast(3))) {
            Files.writeString(path,raw)
            assertTrue(store().read(now).isEmpty())
        }
    }
    @Test fun `disk row cannot assert impossible delivery evidence`() {
        assertTrue(store().write(mapOf(id to listOf(point().copy(availableAt=now-300_000))),now))
        assertTrue(store().read(now).isEmpty())
    }
    @Test fun `oversized otherwise valid files are rejected before processing history`() {
        assertTrue(store().write(mapOf(id to listOf(point())),now))
        val path=directory.resolve("prices.json")
        val root=JsonParser.parseString(Files.readString(path)).asJsonObject
        root.addProperty("ignored","x".repeat(8*1024*1024))
        Files.writeString(path,root.toString())
        assertTrue(store().read(now).isEmpty())
    }
    @Test fun `existing staging file is preserved rather than overwritten by another writer`() {
        assertTrue(store().write(mapOf(id to listOf(point())),now))
        val path=directory.resolve("prices.json")
        val old=Files.readAllBytes(path)
        val stage=directory.resolve("prices.json.pending")
        Files.writeString(stage,"other writer")
        assertFalse(store().write(mapOf(other to listOf(point())),now))
        assertArrayEquals(old,Files.readAllBytes(path))
        assertEquals("other writer",Files.readString(stage))
    }
}
