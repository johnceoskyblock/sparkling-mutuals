package net.johnceo.sparklingmutuals.skyblock

import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class HuntingBoxLedgerTest {
    @TempDir lateinit var dir: Path
    @Test fun `visited pages accumulate and revisiting replaces without duplicating`() {
        val ledger=HuntingBoxLedger()
        val first=BoxPage("page:1",3,listOf(BoxShard("SHARD_A","A Shard",20)))
        ledger.observe(first);ledger.observe(BoxPage("page:2",3,listOf(BoxShard("SHARD_B","B Shard",5))))
        repeat(5) { ledger.observe(first) }
        assertEquals(2,ledger.scannedPages);assertEquals(3,ledger.totalPages)
        assertEquals(25,ledger.shards().sumOf { it.amount })
        ledger.observe(first.copy(shards=listOf(BoxShard("SHARD_A","A Shard",7))))
        assertEquals(12,ledger.shards().sumOf { it.amount })
        ledger.observe(BoxPage("page:3",3,listOf(BoxShard("SHARD_A","A Shard",7),BoxShard("SHARD_C","C Shard",2))))
        assertEquals(14,ledger.shards().sumOf { it.amount },"Overlapping/reordered shards never count twice")
        ledger.clear();assertEquals(0,ledger.scannedPages);assertTrue(ledger.shards().isEmpty())
    }
    @Test fun `amount comes from Owned lore and borders player inventory and other items are excluded`() {
        val shard=HuntingBoxParsing.shard(10,"SHARD_VORACIOUS_SPIDER","§fVoracious Spider Shard",listOf("§7Owned: §b1,729 Shards"))!!
        assertEquals(1729,shard.amount)
        assertEquals("SHARD_VORACIOUS_SPIDER",HuntingBoxParsing.shard(10,"ATTRIBUTE_SHARD","Voracious Spider Shard",listOf("Owned: 1 Shard"))!!.id)
        for(slot in listOf(0,9,17,44,45,54,80))
            assertNull(HuntingBoxParsing.shard(slot,"SHARD_A","A Shard",listOf("Owned: 6 Shards")))
        assertNull(HuntingBoxParsing.shard(10,"STONE","A Shard",listOf("Owned: 6 Shards")))
        assertNull(HuntingBoxParsing.shard(10,"SHARD_A","A Shard",listOf("Price: 6 coins")))
        assertNull(HuntingBoxParsing.shard(10,"SHARD_A","A Shard",listOf("Owned: -1 Shards")))
    }
    @Test fun `page keys use navigation metadata or stable species identities rather than quantities`() {
        val shard=BoxShard("SHARD_A","A Shard",1)
        assertEquals("page:2",HuntingBoxParsing.page("Hunting Box",listOf("§7Page: 2/7"),listOf(shard)).key)
        assertEquals(7,HuntingBoxParsing.page("(2/7) Hunting Box",emptyList(),listOf(shard)).totalPages)
        assertEquals(HuntingBoxParsing.page("Hunting Box",emptyList(),listOf(shard)).key,
            HuntingBoxParsing.page("Hunting Box",emptyList(),listOf(shard.copy(amount=20))).key)
        assertTrue(HuntingBoxParsing.isBox("Hunting Box"));assertFalse(HuntingBoxParsing.isBox("Fake Hunting Box"))
    }
    @Test fun `prices use shared all-shard quotes sorted by sell value and missing prices stay unknown`() {
        val ledger=HuntingBoxLedger()
        ledger.observe(BoxPage("page:1",1,listOf(BoxShard("SHARD_OUTSIDE_SAFARI","Other",10),BoxShard("SHARD_FOXTROT","Foxtrot",2),BoxShard("SHARD_MISSING","Missing",5))))
        val values=ledger.values(mapOf("SHARD_OUTSIDE_SAFARI" to doubleArrayOf(200.0,300.0),"SHARD_FOXTROT" to doubleArrayOf(10.0,12.0)))
        assertEquals("SHARD_OUTSIDE_SAFARI",values.first().shard.id)
        assertEquals(2020.0,values.sumOf { it.sell ?: 0.0 });assertEquals(3024.0,values.sumOf { it.buy ?: 0.0 })
        assertNull(values.last().sell);assertNull(values.last().buy)
    }
    @Test fun `SkyBlock helper toggle persists and Phantomleaf moves without losing its choice`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val settings=SafariSettings()
        settings.skyblock.helpers.huntingBoxValue=false
        settings.skyblock.endgame.phantomleafSolver=false
        settings.apply();ConfigManager.init(dir)
        assertFalse(ConfigManager.huntingBoxValue);assertFalse(ConfigManager.phantomleafSolver)
        val skyblock=SettingsCatalog(SafariSettings()).pages.single { it.name=="SkyBlock" }
        assertEquals(listOf("Helpers","Endgame"),skyblock.rows.map { it.name })
        assertTrue(skyblock.rows.first().children.any { it.name=="Hunting Box value" })
    }
}
