package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SavedHuntingBoxStoreTest {
    @TempDir lateinit var directory: Path
    @Test fun `ownership cache is bounded atomically and interrupted stages recover`() {
        val file=directory.resolve("box.json");val store=SavedHuntingBoxStore(file)
        assertEquals("",store.read());assertTrue(store.write("{\"scopes\":[]}"))
        assertFalse(store.write("é".repeat(32769)));assertEquals("{\"scopes\":[]}",store.read())
        Files.writeString(directory.resolve("box.json.pending"),"abandoned")
        assertTrue(store.write("new"));assertEquals("new",store.read())
        assertFalse(Files.exists(directory.resolve("box.json.pending")))
        Files.writeString(file,"x".repeat(65537));assertEquals("",store.read())
    }
}
