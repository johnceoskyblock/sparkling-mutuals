package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EntryDryRunDispatchTest {
    private val leader = UUID(0, 1)
    private val self = UUID(0, 2)
    private val context = EntryContext(self, "Guest", 1, 1, 1, "main", "mini1", EntryParty(leader, setOf(leader, self), 0))
    private val frame = EntryDryRunFrame(context, true, true, false, 0)

    @Test fun `dispatch accepts only fresh current-context protocol proposals`() {
        val output = EntryDryRunOutput.Party(context, EntryMessage.Query("a".repeat(32), "main", "mini1"), 100)
        assertEquals("pc SMSE1 Q ${"a".repeat(32)} main mini1", EntryDryRunDispatch.command(output, frame, 100))
        assertNull(EntryDryRunDispatch.command(output, frame.copy(enabled = false), 100))
        assertNull(EntryDryRunDispatch.command(output, frame.copy(context = context.copy(world = 2, arrival = 2)), 100))
        assertNull(EntryDryRunDispatch.command(output, frame.copy(atEntry = false), 100))
        assertNull(EntryDryRunDispatch.command(output, frame, 12000))
        assertNull(EntryDryRunDispatch.command(output, frame, 99))
        assertNull(EntryDryRunDispatch.command(output, frame, 3100))
    }

    @Test fun `reply dispatch requires current leader inside run and matching destination`() {
        val lc = context.copy(account = leader, selfName = "Leader")
        val lf = frame.copy(context = lc, atEntry = false, activeRun = true)
        val response = EntryDryRunOutput.Party(lc, EntryMessage.None("Guest", "a".repeat(32), "main", "mini1"), 25000)
        assertNotNull(EntryDryRunDispatch.command(response, lf, 25000))
        assertNull(EntryDryRunDispatch.command(response, lf.copy(activeRun = false), 25000))
        assertNull(EntryDryRunDispatch.command(response.copy(message = EntryMessage.None("Guest", "a".repeat(32), "main", "mini2")), lf, 25000))
    }

    @Test fun `only anchored global capture withdraws evidence before entry`() {
        assertEquals("Doomspiral", EntryCaptureNotice.species("§6SPARKLING! Leader caught a SPARKLING Doomspiral!"))
        assertNull(EntryCaptureNotice.species("Party > Guest: SPARKLING! Leader caught a SPARKLING Doomspiral!"))
        assertNull(EntryCaptureNotice.species("SPARKLING! Leader caught a SPARKLING invented!"))
        assertNull(EntryCaptureNotice.species("SPARKLING! Leader caught a SPARKLING Doomspiral!\nother"))
    }
}
