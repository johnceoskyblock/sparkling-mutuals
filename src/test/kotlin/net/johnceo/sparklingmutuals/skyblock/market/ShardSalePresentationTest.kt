package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ShardSalePresentationTest {
    @TempDir lateinit var directory: Path
    private val now=MarketFixtures.NOW
    @Test fun `guidance defaults on and saved preferences persist`() {
        ConfigManager.init(directory);ContestConfig.init(directory)
        assertTrue(ConfigManager.shardSaleGuidance)
        val settings=SafariSettings()
        settings.skyblock.helpers.shardSaleGuidance=true;settings.apply();ConfigManager.init(directory)
        assertTrue(ConfigManager.shardSaleGuidance)
        val children=SettingsCatalog(SafariSettings()).pages.first {it.name=="SkyBlock"}.rows.first {it.name=="Helpers"}.children
        assertTrue(children.any {it.name=="Shard sale guidance"})
        assertFalse(children.any {it.name.contains("Hunting Box value",true)})
        val disable=SafariSettings();disable.skyblock.helpers.shardSaleGuidance=false;disable.apply();ConfigManager.init(directory)
        assertFalse(ConfigManager.shardSaleGuidance)
    }
    @Test fun `color explanations explicitly describe references without forecast promises`() {
        val sell=Recommendation("SHARD_BLUEBIRD",AdviceState.SELL,100.0,now,"EXPERIMENTAL_RANGE",101.0)
        val hold=sell.copy(state=AdviceState.HOLD,ask=90.0)
        assertNotEquals(ShardSalePresentation.color(sell.state),ShardSalePresentation.color(hold.state))
        val lines=ShardSalePresentation.tooltip(hold,now+60000).joinToString(" ")
        assertFalse(lines.contains("Experimental"));assertTrue(lines.contains("HOLD"));assertTrue(lines.contains("reference"));assertTrue(lines.contains("not a forecast"))
        assertFalse(lines.contains("three-hour"));assertTrue(lines.contains("60s"))
        assertTrue(ShardSalePresentation.tooltip(sell.copy(state=AdviceState.UNKNOWN,reason="STALE_HISTORY"),now).any {it.contains("unavailable",true)})
    }
    @Test fun `a new page menu revision replacement item and stale data invalidate a cached slot immediately`() {
        val menu=Any();val stack=Any()
        val advice=Recommendation("SHARD_BLUEBIRD",AdviceState.SELL,100.0,now,"EXPERIMENTAL_RANGE",100.0)
        val snapshot=SaleMenuSnapshot(menu,7,mapOf(10 to stack),mapOf(10 to advice))
        assertNotNull(snapshot.advice(menu,7,10,stack,now))
        assertNull(snapshot.advice(Any(),7,10,stack,now))
        assertNull(snapshot.advice(menu,8,10,stack,now))
        assertNull(snapshot.advice(menu,7,10,Any(),now))
        assertEquals(AdviceState.UNKNOWN,snapshot.advice(menu,7,10,stack,now+180001)!!.state)
    }
}
