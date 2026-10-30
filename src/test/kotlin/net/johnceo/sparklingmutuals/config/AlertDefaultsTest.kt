package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.alerts.AlertManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AlertDefaultsTest {
    @TempDir lateinit var directory: Path
    @Test
    fun `warp reminder starts at 25 seconds`() {
        ConfigManager.init(directory)
        assertEquals(25, AlertManager.getDelay())
    }
}
