package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SharedCritterProgress

/** Uses negotiated shared completion, with the independent completion service as legacy fallback. */
internal class PartyCritterSync {
    private val transport = PartySyncTransport()
    private var run: SafariRun? = null
    private var sequence = 0L
    private var sent = ""
    private var due = 0L
    private var sampleAt = 0L
    private var shared = false
    private var sharedRun: String? = null
    private var receiveOrdinal = 0L
    private data class Snapshot(val ordinal: Long, val rows: Map<String,SharedCritterProgress.Evidence>,
        val age: Long, val receivedAt: Long)
    private val sharedSnapshots = mutableMapOf<String,Snapshot>()
    init {
        transport.onReady = { sent = ""; due = 0 }
        transport.onMessage = { root, mapping ->
            if (!shared) receive(root, mapping, transport.roomStatus().context?.self)
        }
    }
    private fun receive(root: JsonObject, mapping: Map<String, String>, self: String?) {
        val sender = PartySyncProtocol.text(root.get("sender"))?.let(mapping::get)
        if (sender == null || sender == self || PartySyncProtocol.text(root.get("type")) != "state" ||
            PartySyncProtocol.text(root.get("kind")) != "C") return
        val seq = PartySyncProtocol.integer(root.get("sequence"), 1)
        val age = PartySyncProtocol.integer(root.get("ageMillis"), 0, SharedCritterProgress.TTL - 1)
        val rows = PartySyncProtocol.text(root.get("payload"), 3072)?.let(CritterProgressProtocol::decode)
        if (seq != null && age != null && rows != null) run?.sharedCritters?.replace(sender, seq, rows, age)
    }
    fun receiveShared(root: JsonObject, mapping: Map<String, String>, self: String, runId: String): Boolean {
        if (PartySyncProtocol.text(root.get("type")) != "state" || PartySyncProtocol.text(root.get("kind")) != "C") return false
        if (!PartySyncVisit.validId(runId) || PartySyncProtocol.text(root.get("runId")) != runId) return false
        val sender=PartySyncProtocol.text(root.get("sender"))?.let(mapping::get) ?: return false
        if(sender==self || PartySyncProtocol.integer(root.get("sequence"),1)==null) return false
        val age=PartySyncProtocol.integer(root.get("ageMillis"),0,SharedCritterProgress.TTL-1) ?: return false
        val rows=PartySyncProtocol.text(root.get("payload"),3072)?.let { CritterProgressProtocol.decode(it,runId) } ?: return false
        if(sharedRun!=runId) { run?.sharedCritters?.clear(); sharedSnapshots.clear(); sharedRun=runId }
        // The server orders accepted active-socket messages; sender counters can restart.
        sharedSnapshots[sender]=Snapshot(++receiveOrdinal,rows,age,System.nanoTime())
        return true
    }
    fun tick(context: PartySyncTransport.Context, origin: URI?, generation: String?, nonce: String,
        active: SafariRun?, party: PartySparklingState, now: Long, eligible: Boolean,
        useShared: Boolean = false, sharedRunId: String? = null, sharedVisitId: String? = null,
        sendShared: (String) -> Boolean = { false }) {
        if (!eligible || !useShared && (origin == null || generation == null)) { reset(); return }
        if (shared != useShared) {
            transport.close(false)
            run?.sharedCritters?.clear()
            shared = useShared
            sent = ""; due = 0; sampleAt = 0
        }
        if (run !== active) { run?.sharedCritters?.clear(); transport.close(false); run = active; sent = ""; sampleAt = 0 }
        if (!shared) {
            transport.useCompletion(checkNotNull(origin), checkNotNull(generation), nonce)
            transport.tick(context, now)
            if (!transport.ready) { run?.sharedCritters?.clear(); return }
        }
        if (active == null || active.endedAt != null || now < sampleAt) return
        if(shared) {
            if(sharedRunId==null || sharedVisitId==null) return
            if(sharedRun!=sharedRunId) { active.sharedCritters.clear(); sharedSnapshots.clear(); sharedRun=sharedRunId }
            sharedSnapshots.forEach { (sender,snapshot) ->
                val age=queuedCompletionAge(snapshot.age,System.nanoTime()-snapshot.receivedAt)
                active.sharedCritters.replace(sender,snapshot.ordinal,snapshot.rows,age)
            }
        }
        sampleAt = now + 500
        val payload = CritterProgressProtocol.encode(active, party,
            sharedRunId.takeIf { shared },sharedVisitId.takeIf { shared })
        if (payload != sent || now >= due) {
            val submitted = if (shared) sendShared(payload) else transport.send(++sequence, "C", payload)
            if (submitted) { sent = payload; due = now + 10_000 }
        }
    }
    fun developerSnapshot() = mapOf("provider" to if (shared) "shared" else "separate",
        "transport" to transport.developerSnapshot())
    fun reset() {
        transport.close(false); run?.sharedCritters?.clear(); run = null; sent = ""; due = 0; sampleAt = 0; shared = false
        sharedRun=null; sharedSnapshots.clear(); receiveOrdinal=0
    }
}

/** Stable species order is shared with S, but C evidence never enters S/capture/history ledgers. */
internal object CritterProgressProtocol {
    fun encode(run: SafariRun, party: PartySparklingState, runId: String? = null, visitId: String? = null): String = JsonObject().apply {
        addProperty("version", 1)
        if(runId!=null || visitId!=null) {
            require(runId!=null && visitId!=null && PartySyncVisit.validId(runId) && PartySyncVisit.validId(visitId))
            addProperty("runId",runId);addProperty("visitId",visitId)
        }
        add("rows", JsonArray().apply { PartySyncProtocol.species.forEach { name ->
            // Export only existing local proof, never any newly received C data.
            val unique = run.uniqueComplete(name, party, localOnly = true) || run.uniqueUnavailable(name)
            val checked = run.sparklingComplete(name, party, localOnly = true)
            val full = run.captureComplete(name)
            add(JsonArray().apply {
                add((if (unique) 1 else 0) or (if (checked) 2 else 0) or (if (full) 4 else 0))
                add(run.sparklingChecks.checkCount(name).coerceIn(0, 256)); add(run.count(name).coerceIn(0, 256))
            })
        } })
    }.toString()
    fun decode(payload: String, runId: String? = null): Map<String, SharedCritterProgress.Evidence>? = runCatching {
        if (payload.length > 3072) return null
        val p = JsonParser.parseString(payload).asJsonObject
        val keys=if(runId==null) setOf("version","rows") else setOf("version","rows","runId","visitId")
        if (p.keySet() != keys || PartySyncProtocol.integer(p.get("version"), 1, 1) == null) return null
        if(runId!=null && (PartySyncProtocol.text(p.get("runId"))!=runId ||
            PartySyncProtocol.text(p.get("visitId"))?.let(PartySyncVisit::validId)!=true)) return null
        val rows = p.getAsJsonArray("rows")
        if (rows.size() != PartySyncProtocol.species.size) return null
        PartySyncProtocol.species.mapIndexed { i, name ->
            val row = rows[i].asJsonArray
            if (row.size() != 3) return null
            val flags = PartySyncProtocol.integer(row[0], 0, 7)?.toInt() ?: return null
            val checks = PartySyncProtocol.integer(row[1], 0, 256)?.toInt() ?: return null
            val captures = PartySyncProtocol.integer(row[2], 0, 256)?.toInt() ?: return null
            name to SharedCritterProgress.Evidence(flags and 1 != 0, flags and 2 != 0, flags and 4 != 0, checks, captures)
        }.toMap()
    }.getOrNull()
}
