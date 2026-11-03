package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WholeBoxTotalStoreTest {
    @TempDir lateinit var directory: Path
    @Test fun `large bounded ownership files recover interrupted writes but reject oversized input`() {
        val file=directory.resolve("totals.json");val store=WholeBoxTotalStore(file)
        assertEquals("",store.read())
        val raw="x".repeat(70000)
        assertTrue(store.write(raw));assertEquals(raw,store.read())
        assertFalse(store.write("é".repeat(WholeBoxLedger.LIMIT/2+1)));assertEquals(raw,store.read())
        Files.writeString(directory.resolve("totals.json.pending"),"interrupted")
        assertTrue(store.write("new"));assertEquals("new",store.read())
        assertFalse(Files.exists(directory.resolve("totals.json.pending")))
        Files.writeString(file,"x".repeat(WholeBoxLedger.LIMIT+1));assertEquals("",store.read())
    }
}
