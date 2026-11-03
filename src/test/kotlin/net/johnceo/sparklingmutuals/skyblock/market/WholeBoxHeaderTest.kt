package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.config.*
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WholeBoxHeaderTest {
    @TempDir lateinit var directory: Path
    @Test fun `box value is one plainly named coin amount without helper lines`() {
        val summary=WholeBoxSummary(WholeBoxValue(1600.0,1,2,MarketFixtures.NOW),WholeBoxCoverage(2,3,false,1,1),false)
        val lines=WholeBoxHeader.lines(summary)
        assertEquals(listOf("Hunting Box value: 2k coins"),lines)
        assertEquals(listOf("Hunting Box value: unavailable"),WholeBoxHeader.lines(summary.copy(value=WholeBoxValue(null,3,0,0))))
    }
    @Test fun `header respects actual bounds scales readably and hides without space above`() {
        val bounds=WholeBoxHeader.bounds(640,232,97,176,170,5)!!
        assertEquals(232,bounds.x);assertTrue(bounds.y>=4);assertTrue(bounds.y+bounds.height<97)
        assertTrue(bounds.scale>=.75f)
        assertNull(WholeBoxHeader.bounds(640,232,20,176,170,5))
        assertNull(WholeBoxHeader.bounds(640,232,97,176,400,5))
        assertNull(WholeBoxHeader.bounds(320,232,97,176,170,5))
    }
    @Test fun `total has an independent searchable saved setting alongside guidance`() {
        ConfigManager.init(directory);ContestConfig.init(directory)
        assertTrue(ConfigManager.huntingBoxValue)
        val settings=SafariSettings()
        val helpers=SettingsCatalog(settings).pages.first {it.name=="SkyBlock"}.rows.first {it.name=="Helpers"}.children
        assertTrue(helpers.any {it.name=="Hunting Box total value"})
        settings.skyblock.helpers.huntingBoxValue=false;settings.skyblock.helpers.shardSaleGuidance=true
        settings.apply();ConfigManager.init(directory)
        assertFalse(ConfigManager.huntingBoxValue);assertTrue(ConfigManager.shardSaleGuidance)
    }
}
