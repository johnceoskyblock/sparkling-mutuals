package net.johnceo.sparklingmutuals.alerts

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.config.ConfigManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WarpListDefaultsTest {
    @TempDir lateinit var directory: Path

    @Test fun `fresh installs keep invitation lists empty and leader automation off`() {
        ConfigManager.init(directory)
        assertTrue(WarpList.critters.all { WarpList.names(it).isEmpty() })
        assertFalse(ConfigManager.autoParty)
        assertFalse(ConfigManager.warpAlertsEnabled)
        ConfigManager.save()
        ConfigManager.init(directory)
        assertTrue(WarpList.critters.all { WarpList.names(it).isEmpty() })
        WarpList.set("Cavernfish", "MyFriend SecondFriend")
        ConfigManager.save()
        ConfigManager.init(directory)
        assertEquals(listOf("MyFriend", "SecondFriend"), WarpList.names("Cavernfish"))
    }

    @Test fun `existing saved and intentionally empty lists are not replaced by bundled names`() {
        val properties = Properties().apply {
            setProperty("warpList.Areita", "MyFriend")
            setProperty("warpList.Cavernfish", "")
        }
        Files.newOutputStream(directory.resolve("sparkling-mutuals.properties")).use {
            properties.store(it, null)
        }
        ConfigManager.init(directory)
        assertEquals(listOf("MyFriend"), WarpList.names("Areita"))
        assertTrue(WarpList.names("Cavernfish").isEmpty())
        assertTrue(WarpList.names("Duplico").isEmpty())
        ConfigManager.save()
        ConfigManager.init(directory)
        assertEquals(listOf("MyFriend"), WarpList.names("Areita"))
        assertTrue(WarpList.names("Cavernfish").isEmpty())
    }
}
