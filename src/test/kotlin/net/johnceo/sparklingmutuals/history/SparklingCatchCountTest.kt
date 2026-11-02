package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SparklingCatchCountTest {
    @TempDir lateinit var dir: Path
    @Test fun `duplicate number includes earlier party and imported catches but no other species or future catches`() {
        val current=SparklingEntry(3000,"Me","Foxtrot")
        val history=listOf(SparklingEntry(1000,"Friend","Foxtrot"),SparklingEntry(2000,"Other","foxtrot"),
            SparklingEntry(2500,"Me","Polaris"),current,SparklingEntry(4000,"Me","Foxtrot"))
        assertEquals("Sparkling Foxtrot caught! #3",SparklingCatchCount.text(current,history))
    }
    @Test fun `only confirmed unique catches say NEW and missing earlier history does not claim uniqueness`() {
        val first=SparklingEntry(1000,"Friend","Polaris").apply { unique=true }
        assertEquals("Sparkling Polaris caught! NEW!",SparklingCatchCount.text(first,listOf(first)))
        first.unique=false
        assertEquals("Sparkling Polaris caught! #1",SparklingCatchCount.text(first,listOf(first)))
    }
    @Test fun `detection and alerts toggle defaults on and persists independently from catch alert`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val settings=SafariSettings()
        assertTrue(settings.safari.alerts.catchCount)
        settings.safari.alerts.catchCount=false;settings.apply();ConfigManager.init(dir)
        assertFalse(ConfigManager.sparklingCatchCount);assertTrue(ConfigManager.specialCatchAlert)
    }
}
