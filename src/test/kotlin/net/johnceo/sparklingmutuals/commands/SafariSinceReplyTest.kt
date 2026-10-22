package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.SparklingEntry
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariSinceReplyTest {
    @Test fun `both since forms are party wide and work without an API key in every mode`() {
        for ((word, kind) in listOf("" to PartyCommandKind.SINCE_SPARKLING, "unique" to PartyCommandKind.SINCE_UNIQUE)) {
            val request = PartyCommand.fromChat("§9Party > §a[MVP+] Farmer: ${("!SiNcE   " + word).trim()}")!!
            assertEquals(kind, request.command.kind)
            assertEquals("", request.command.ign)
            assertEquals(("!since " + word).trim(), request.command.text)
            assertEquals("Other", request.command.forResponder("Other").ign)
            assertFalse(request.command.requiresApiKey)
            SafariMode.entries.forEach { assertTrue(request.command.allowed(it)) }
            assertTrue(PartyLookupResult.fetch(request.command, "", cloud = false) { "local result" }.successful)
        }
        for (line in listOf("Guild > Farmer: !since sparkling", "Party > Farmer: !since sparkling", "Party > Farmer: !since all",
            "Party > Farmer: !since unique Other", "Party > Farmer: !since sparkle", "!since unique"))
            assertNull(PartyCommand.fromChat(line), line)
    }
    @Test fun `any party find in recorded runs resets the corresponding streak and the catch run is excluded`() {
        val runs = listOf(9000L, 1000L, 5000L, 7000L, 3000L).map { RunRecord(start = it, length = 1000) }
        val entries = listOf(SparklingEntry(11000, "Outside", "Wumpa", unique = true),
            SparklingEntry(1500, "Farmer", "Foxtrot", unique = true), SparklingEntry(3500, "Other", "Wumpa", unique = true),
            SparklingEntry(7500, "Other", "Foxtrot"))
        assertEquals("Farmer: 1 run since last shiny: Foxtrot caught by Other.", SafariSinceReply.format("Farmer", false, runs, entries))
        assertEquals("Farmer: 3 runs since last unique shiny: Wumpa caught by Other.", SafariSinceReply.format("Farmer", true, runs, entries))
    }
    @Test fun `outside run finds and missing credited history cannot fabricate a streak`() {
        val runs = listOf(RunRecord(start = 1000, length = 500), RunRecord(start = 2000, length = 500), RunRecord())
        val entries = listOf(SparklingEntry(3000, "Other", "Foxtrot", unique = true), SparklingEntry(0, "Farmer", "Foxtrot"))
        assertEquals("Farmer: No shiny in history (2 runs tracked).", SafariSinceReply.format("Farmer", false, runs, entries))
        assertEquals("Farmer: No unique shiny in history (2 runs tracked).", SafariSinceReply.format("Farmer", true, runs, entries))
        assertEquals("Farmer: No shiny in history (0 runs tracked).", SafariSinceReply.format("Farmer", false, emptyList(), emptyList()))
    }
    @Test fun `a teammates catch in the current run resets normal but a repeat does not reset unique`() {
        val runs = listOf(RunRecord(start = 1000, length = 1000), RunRecord(start = 2500, length = 200))
        val active = SafariRun(3000)
        val entries = listOf(SparklingEntry(1500, "Friend", "Foxtrot", unique = true, uniqueEstimated = true),
            SparklingEntry(4000, "Other", "Foxtrot"))
        assertEquals("Farmer: 0 runs since last shiny: Foxtrot caught by Other.", SafariSinceReply.format("Farmer", false, runs, entries, active))
        assertEquals("Farmer: 1 run since last unique shiny: Foxtrot caught by Friend.", SafariSinceReply.format("Farmer", true, runs, entries, active))
        active.endedAt = 3500
        assertEquals("Farmer: 1 run since last shiny: Foxtrot caught by Friend.", SafariSinceReply.format("Farmer", false, runs, entries, active))
    }
    @Test fun `unknown imported duration requires a matching run Sparkling record to establish credit`() {
        val runs = listOf(RunRecord(start = 1000, imported = true, sparklings = mutableListOf("Friend:Foxtrot")),
            RunRecord(start = 3000, length = 500))
        val entries = listOf(SparklingEntry(1500, "Friend", "Foxtrot", unique = true),
            SparklingEntry(2500, "Outside", "Wumpa", unique = true))
        assertEquals("Farmer: 1 run since last unique shiny: Foxtrot caught by Friend.", SafariSinceReply.format("Farmer", true, runs, entries))
    }
    @Test fun `response matching separates streak kinds and help uses the short default command`() {
        val normal = PartyCommand(PartyCommandKind.SINCE_SPARKLING, "Farmer")
        val unique = PartyCommand(PartyCommandKind.SINCE_UNIQUE, "Farmer")
        for (command in listOf(normal, unique)) {
            val isUnique = command == unique
            val found = SafariSinceReply.format("Farmer", isUnique, listOf(RunRecord(start = 500, length = 1000), RunRecord(start = 2000)), listOf(SparklingEntry(1000, "Farmer", "Foxtrot", true)))
            val unknown = SafariSinceReply.format("Farmer", isUnique, emptyList(), emptyList())
            assertTrue(command.matchesResponse(found)); assertTrue(command.matchesResponse(unknown))
            assertTrue(command.matchesResponse(found.replace("caught by Farmer", "caught by Other")))
            assertFalse(command.matchesResponse(found.replace("Farmer:", "Other:")))
            assertFalse(command.matchesResponse("[SM] Checking: ${command.text}"))
            assertFalse((if (isUnique) normal else unique).matchesResponse(found))
        }
        SafariMode.entries.forEach {
            assertTrue(CommandHelp.partyReply(it).contains("!since,"))
            assertFalse(Regex("sparkling|sparkle", RegexOption.IGNORE_CASE).containsMatchIn(CommandHelp.partyReply(it)))
            assertTrue(CommandHelp.partyReply(it).contains("!since unique"))
        }
    }
}
