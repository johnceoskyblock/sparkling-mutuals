package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.hud.SafariHud
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ConfigLayoutTest {
    @TempDir lateinit var dir: Path

    @Test fun `narrow cards stack controls while wide cards reserve a text gap`() {
        val narrow=ConfigLayout.columns(220,120)
        assertTrue(narrow.stacked)
        assertEquals(196,narrow.textWidth)
        val wide=ConfigLayout.columns(500,120)
        assertFalse(wide.stacked)
        assertEquals(500-24-16,wide.textWidth+wide.controlWidth)
        assertTrue(ConfigLayout.columns(500,120,true).stacked)
        for(width in listOf(100,200,330,700)) {
            val header=ConfigLayout.header(20,20+width)
            assertTrue(20+header.searchWidth+10<=header.themeX)
            assertEquals(20+width,header.themeX+header.themeWidth)
        }
    }

    @Test fun `every HUD links to a real toggle through its containing groups`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val catalog=SettingsCatalog(SafariSettings())
        val targets=SafariHud.entries.filter { it != SafariHud.DEVELOPER }.map(HudConfigLinks::target)+HudConfigLinks.contest
        targets.forEach { target ->
            val located=catalog.locate(target)
            assertNotNull(located,"Missing HUD setting: $target")
            val chain=located!!.second
            assertTrue(chain.last().read() is Boolean,"HUD destination is not a toggle: $target")
            assertTrue(chain.dropLast(1).all { it.group },"Cannot expand HUD ancestors: $target")
        }
    }
}
