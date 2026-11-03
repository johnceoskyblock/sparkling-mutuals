package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.config.*
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WholeBoxHeaderTest {
    @TempDir lateinit var directory: Path
    @Test fun `compact text shows one total with sell offer basis and honest partial notes`() {
        val summary=WholeBoxSummary(WholeBoxValue(1600.0,1,2,MarketFixtures.NOW),WholeBoxCoverage(2,3,false,1,1),false)
        val lines=WholeBoxHeader.lines(summary)
        assertTrue(lines.first().contains("subtotal",true))
        assertTrue(lines.any {it.contains("Sell offers")})
        assertTrue(lines.any {it.contains("2/3") && it.contains("partial")})
        assertTrue(lines.any {it.contains("older",true)})
        assertTrue(lines.any {it.contains("unpriced",true)})
        assertFalse(lines.joinToString(" ").contains("Bluebird"))
        assertTrue(WholeBoxHeader.lines(summary.copy(value=WholeBoxValue(null,3,0,0))).first().contains("unavailable",true))
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
