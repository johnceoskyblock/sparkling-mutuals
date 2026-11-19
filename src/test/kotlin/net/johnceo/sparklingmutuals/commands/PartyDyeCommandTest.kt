package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartyDyeCommandTest {
    @Test fun `dye is a party-only no-argument command available without an API key in every mode`() {
        val command = PartyCommand.fromChat("Party > [MVP+] Farmer: !DyE")!!.command
        assertEquals(PartyCommandKind.DYE, command.kind)
        assertEquals("!dye", command.text)
        assertFalse(command.requiresApiKey)
        for (mode in SafariMode.entries) {
            assertTrue(command.allowed(mode))
            assertTrue(CommandHelp.partyReply(mode).contains("!dye"))
        }
        for (line in listOf("Guild > Farmer: !dye", "Party > Farmer: !dye Farmer", "Party > Farmer: I typed !dye"))
            assertNull(PartyCommand.fromChat(line))
    }

    @Test fun `each responder reports their own count and equal counts from other players do not match`() {
        val command = PartyCommand.fromChat("Party > Farmer: !dye")!!.command
        val alice = command.forResponder("Alice")
        val bob = command.forResponder("Bob")
        assertEquals("Alice: Emerald Dyes (0)", alice.dyeResult(0))
        assertEquals("Alice: Emerald Dyes (12)", alice.dyeResult(12))
        assertTrue(alice.matchesResponse(alice.dyeResult(0)))
        assertFalse(alice.matchesResponse(bob.dyeResult(0)))
        assertFalse(alice.matchesResponse(alice.failureResult("Unavailable")))
        assertFalse(alice.matchesResponse("Alice: Emerald Dyes (-1)"))
    }
}
