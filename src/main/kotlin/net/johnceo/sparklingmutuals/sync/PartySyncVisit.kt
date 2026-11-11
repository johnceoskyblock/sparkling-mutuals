package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.UUID

/** Visit identity survives socket replacement. Only an explicit new visit clears the run. */
internal class PartySyncVisit {
    var visitId = UUID.randomUUID().toString()
        private set
    var runId: String? = null
        private set
    var connectionId: String? = null
        private set
    var reconciled = false
        private set
    var replayComplete = false
        private set
    var status = "awaiting"
        private set

    fun welcome(id: String): Boolean {
        if (!validId(id)) return false
        disconnected()
        connectionId = id
        return true
    }
    fun disconnected() {
        connectionId = null
        reconciled = false
        replayComplete = false
        status = "awaiting"
    }
    fun changeAuthority() {
        disconnected()
        runId = null
    }
    fun newVisit() {
        disconnected()
        visitId = UUID.randomUUID().toString()
        runId = null
    }
    fun matchesConnection(root: JsonObject) =
        PartySyncProtocol.integer(root.get("version"), 1, 1) != null &&
            PartySyncProtocol.text(root.get("connectionId")) == connectionId && connectionId != null &&
            PartySyncProtocol.text(root.get("visitId")) == visitId

    fun reconcile(root: JsonObject): Boolean {
        if (!matchesConnection(root)) return false
        val nextStatus = PartySyncProtocol.text(root.get("status")) ?: return false
        if (nextStatus == "pending") {
            if (root.get("runId")?.isJsonNull != true || replayComplete) return false
            reconciled = false
            status = nextStatus
            return true
        }
        if (nextStatus !in setOf("new", "joined", "resumed")) return false
        val id = PartySyncProtocol.text(root.get("runId"))?.takeIf(::validId) ?: return false
        if (runId != null && runId != id) return false
        runId = id
        reconciled = true
        replayComplete = false
        status = nextStatus
        return true
    }
    fun matchesReplay(root: JsonObject) = matchesConnection(root) && reconciled &&
        PartySyncProtocol.text(root.get("runId")) == runId
    fun finishReplay() { check(reconciled); replayComplete = true }
    fun accepts(root: JsonObject): Boolean {
        if (!reconciled) return false
        val id = if (PartySyncProtocol.text(root.get("type")) == "hotspots")
            root.get("payload")?.takeIf { it.isJsonObject }?.asJsonObject?.get("run")
        else root.get("runId")
        return PartySyncProtocol.text(id) == runId
    }
    fun operation(payload: String): String {
        val root = com.google.gson.JsonParser.parseString(payload).asJsonObject
        root.addProperty("visitId", visitId)
        if (PartySyncProtocol.text(root.get("action")) == "hello") {
            root.add("capabilities", JsonArray().apply {
                add("run-reconciliation-v1"); add("critter-completion-v1")
            })
            root.addProperty("resumeRunId", runId)
        } else root.addProperty("runId", runId.takeIf { reconciled })
        return root.toString()
    }
    companion object {
        fun validId(value: String) = runCatching {
            UUID.fromString(value).toString().equals(value, true)
        }.getOrDefault(false)
    }
}
