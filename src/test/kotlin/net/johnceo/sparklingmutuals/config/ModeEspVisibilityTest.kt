package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.safari.SafariMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModeEspVisibilityTest {
    @TempDir lateinit var dir: Path
    @Test fun `ESP hides default off reports default on and independent per mode choices persist`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        for(mode in SafariMode.entries) {
            assertFalse(ConfigManager.hideCritterEsp(mode))
            assertFalse(ConfigManager.hideFloorDropEsp(mode))
        }
        assertTrue(ConfigManager.autoBiomeReports)
        val settings=SafariSettings()
        settings.modes.unique.hideCritters=true
        settings.modes.unique.profitable=true
        settings.modes.fullClear.hideDrops=true
        settings.modes.sparkling.hideDrops=true
        settings.modes.sparkling.profitable=true
        settings.party.biomeReports=false
        val mode=net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode
        settings.apply()
        assertFalse(settings.modes.unique.profitable)
        assertFalse(settings.modes.sparkling.profitable)
        assertEquals(mode,net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode)
        ConfigManager.init(dir)
        assertTrue(ConfigManager.hideCritterEsp(SafariMode.UNIQUE))
        assertFalse(ConfigManager.hideFloorDropEsp(SafariMode.UNIQUE))
        assertFalse(ConfigManager.hideCritterEsp(SafariMode.FULL_CLEAR))
        assertTrue(ConfigManager.hideFloorDropEsp(SafariMode.FULL_CLEAR))
        assertFalse(ConfigManager.hideCritterEsp(SafariMode.SPARKLING))
        assertTrue(ConfigManager.hideFloorDropEsp(SafariMode.SPARKLING))
        assertFalse(ConfigManager.profitableShardEsp);assertFalse(ConfigManager.sparklingProfitableShardEsp)
        assertFalse(ConfigManager.autoBiomeReports)
    }
}
