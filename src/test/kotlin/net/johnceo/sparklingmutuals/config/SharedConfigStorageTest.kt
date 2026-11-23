package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SharedConfigStorageTest {
    @TempDir lateinit var root: Path
    @Test fun `imports config and history for players coming from the separate Standard folder`() {
        val previous = Files.createDirectories(root.resolve("sparkling-mutuals-legit"))
        val settings = "sparkling-mutuals.properties"
        val runs = "sparkling-mutuals-runs.json"
        Files.writeString(previous.resolve(settings), "progressHud=false\n")
        Files.writeString(previous.resolve(runs), """{"runs": []}""")

        val target = ConfigStorage.migrate(root)
        assertEquals(root.resolve("sparkling-mutuals"), target)
        assertEquals(Files.readString(previous.resolve(settings)), Files.readString(target.resolve(settings)))
        assertEquals(Files.readString(previous.resolve(runs)), Files.readString(target.resolve(runs)))

        Files.delete(target.resolve(settings))
        assertEquals(target, ConfigStorage.migrate(root))
        assertFalse(Files.exists(target.resolve(settings)))
    }

    @Test fun `shared saves take precedence and conflicting old saves are kept`() {
        val previous = Files.createDirectories(root.resolve("sparkling-mutuals-legit"))
        val shared = Files.createDirectories(root.resolve("sparkling-mutuals"))
        val settings = "sparkling-mutuals.properties"
        val runs = "sparkling-mutuals-runs.json"
        Files.writeString(previous.resolve(settings), "progressHud=false\n")
        Files.writeString(shared.resolve(settings), "progressHud=true\n")
        Files.writeString(previous.resolve(runs), """{"runs": []}""")

        assertEquals(shared, ConfigStorage.migrate(root))
        assertEquals("progressHud=true\n", Files.readString(shared.resolve(settings)))
        assertEquals("progressHud=false\n", Files.readString(previous.resolve(settings)))
        assertEquals(Files.readString(previous.resolve(runs)), Files.readString(shared.resolve(runs)))
    }
}
