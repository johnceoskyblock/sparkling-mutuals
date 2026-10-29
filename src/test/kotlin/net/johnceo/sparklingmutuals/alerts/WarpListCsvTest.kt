package net.johnceo.sparklingmutuals.alerts

import java.io.StringReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WarpListCsvTest {
    @Test fun `headers map columns and only ten name rows are used`() {
        val csv = "Foxtrot,Ignored,Mantis Shrimp\r\n" +
            (1..10).joinToString("\r\n") { "Player$it,Other$it,Shrimp$it" } +
            "\r\nShouldNotImport,Extra,Nope\r\n\"malformed footer"
        val imported = WarpListCsv.read(StringReader(csv))
        assertEquals((1..10).map { "Player$it" }, imported["Foxtrot"])
        assertEquals((1..10).map { "Shrimp$it" }, imported["Mantis Shrimp"])
        assertEquals(emptyList<String>(), imported["Tepid"])
        assertEquals(WarpList.critters.toSet(), imported.keys)
    }
    @Test fun `quoted BOM headers names blanks and duplicates work`() {
        val csv = "\uFEFF\" mantis shrimp \",Foxtrot,Notes\n" +
            "\" First_Name \",Fox,\"note with a comma, and \"\"quotes\"\"\"\n" +
            "first_name,fox,ignored\n" +
            "Second,,\n/p Someone,Not-an-IGN,\n"
        val imported = WarpListCsv.read(StringReader(csv))
        assertEquals(listOf("First_Name", "Second"), imported["Mantis Shrimp"])
        assertEquals(listOf("Fox"), imported["Foxtrot"])
    }
    @Test fun `replacement clears previous names including unsupported columns`() {
        WarpList.load(java.util.Properties())
        WarpList.set("Foxtrot", "Old")
        WarpList.set("Tepid", "AlsoOld")
        val imported = WarpListCsv.read(StringReader("Foxtrot\nNew\n"))
        imported.forEach { (critter, names) -> WarpList.set(critter, names.joinToString(" ")) }
        assertEquals(listOf("New"), WarpList.names("Foxtrot"))
        assertTrue(WarpList.names("Tepid").isEmpty())
        WarpList.load(java.util.Properties())
    }
    @Test fun `invalid files fail before replacing anything`() {
        for (csv in listOf("", "Notes\nSomeone\n", "Foxtrot,FOXTROT\nA,B", "Foxtrot\n\"unclosed"))
            assertThrows(IllegalArgumentException::class.java) { WarpListCsv.read(StringReader(csv)) }
        assertTrue(WarpListCsv.read(StringReader("Foxtrot\n\n"))["Foxtrot"].orEmpty().isEmpty())
    }
}
