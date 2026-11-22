package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LegitConfigIsolationTest {
    @TempDir lateinit var root: Path
    @Test fun `legit storage never imports or moves cheat saves`() {
        val legacy = root.resolve("sparkling-mutuals.properties")
        Files.writeString(legacy, "autoClicker=true\nautoParty=true\n")
        val cheat = Files.createDirectories(root.resolve("sparkling-mutuals"))
        Files.writeString(cheat.resolve("sparkling-mutuals-runs.json"), "[]")
        val target = ConfigStorage.migrate(root)
        assertEquals(root.resolve("sparkling-mutuals-legit"), target)
        assertTrue(Files.exists(legacy))
        assertEquals("[]", Files.readString(cheat.resolve("sparkling-mutuals-runs.json")))
        assertFalse(Files.exists(target.resolve("sparkling-mutuals.properties")))
        assertEquals(target, ConfigStorage.migrate(root))
    }
}
