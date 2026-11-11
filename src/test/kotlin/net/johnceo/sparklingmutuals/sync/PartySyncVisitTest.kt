package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncVisitTest {
    private val connection="10000000-0000-0000-0000-000000000001"
    private val run="20000000-0000-0000-0000-000000000001"
    private fun boundary(visit: PartySyncVisit,status: String="joined")=JsonObject().apply {
        addProperty("version",1);addProperty("connectionId",connection);addProperty("visitId",visit.visitId)
        addProperty("status",status);addProperty("runId",run)
    }
    @Test fun `pending and old socket boundaries cannot establish replay readiness`() {
        val visit=PartySyncVisit()
        assertTrue(visit.welcome(connection))
        assertTrue(visit.reconcile(boundary(visit,"pending").apply { add("runId",com.google.gson.JsonNull.INSTANCE) }))
        assertFalse(visit.reconciled)
        assertFalse(visit.matchesReplay(boundary(visit)))
        assertTrue(visit.reconcile(boundary(visit)))
        assertTrue(visit.matchesReplay(boundary(visit)))
        assertFalse(visit.matchesReplay(boundary(visit).apply { addProperty("connectionId","30000000-0000-0000-0000-000000000001") }))
        assertFalse(visit.reconcile(boundary(visit).apply { addProperty("runId","40000000-0000-0000-0000-000000000001") }))
        visit.finishReplay()
        assertTrue(visit.replayComplete)
    }
    @Test fun `visit and resume survive reconnect but a new visit cannot inherit the old run`() {
        val visit=PartySyncVisit()
        visit.welcome(connection);visit.reconcile(boundary(visit))
        val first=visit.visitId
        visit.disconnected()
        assertEquals(first,visit.visitId);assertEquals(run,visit.runId)
        val hello=JsonParser.parseString(visit.operation("""{"version":1,"action":"hello"}""")).asJsonObject
        assertEquals(run,hello["resumeRunId"].asString)
        assertEquals(first,hello["visitId"].asString)
        visit.newVisit()
        assertNotEquals(first,visit.visitId);assertNull(visit.runId)
        assertFalse(visit.accepts(JsonObject().apply { addProperty("type","state");addProperty("runId",run) }))
    }
}
