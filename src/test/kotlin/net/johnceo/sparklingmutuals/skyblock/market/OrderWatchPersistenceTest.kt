package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OrderWatchPersistenceTest {
    @TempDir lateinit var directory: Path
    @Test fun `bounded private cache preserves existing data on oversized write or abandoned staging file`() {
        val file=directory.resolve("orders.json");val store=OrderWatchPersistence(file)
        assertEquals("",store.read());assertTrue(store.write("{\"scopes\":[]}"));assertEquals("{\"scopes\":[]}",store.read())
        assertFalse(store.write("é".repeat(300000)));assertEquals("{\"scopes\":[]}",store.read())
        Files.writeString(directory.resolve("orders.json.pending"),"preserve")
        assertFalse(store.write("new"));assertEquals("{\"scopes\":[]}",store.read())
        Files.writeString(file,"x".repeat(524289));assertEquals("",store.read())
    }
}
