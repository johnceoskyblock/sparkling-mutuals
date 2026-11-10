package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SharedCritterProgress

/** Completion-only channel on our service, independent of Serkosea's main room/allocator. */
internal class PartyCritterSync {
    private val transport = PartySyncTransport()
    private var run: SafariRun? = null
    private var sequence = 0L
    private var sent = ""
    private var due = 0L
    private var sampleAt = 0L
    init {
        transport.onReady = { sent = ""; due = 0 }
        transport.onMessage = { root, mapping ->
            val sender = PartySyncProtocol.text(root.get("sender"))?.let(mapping::get)
            val self = transport.roomStatus().context?.self
            if (sender != null && sender != self && PartySyncProtocol.text(root.get("type")) == "state" &&
                PartySyncProtocol.text(root.get("kind")) == "C") {
                val seq = PartySyncProtocol.integer(root.get("sequence"), 1)
                val age = PartySyncProtocol.integer(root.get("ageMillis"), 0, SharedCritterProgress.TTL - 1)
                val rows = PartySyncProtocol.text(root.get("payload"), 3072)?.let(CritterProgressProtocol::decode)
                if (seq != null && age != null && rows != null) run?.sharedCritters?.replace(sender, seq, rows, age)
            }
        }
    }
    fun tick(context: PartySyncTransport.Context, origin: URI?, generation: String?, nonce: String,
        active: SafariRun?, party: PartySparklingState, now: Long, eligible: Boolean) {
        if (!eligible || origin == null || generation == null) { reset(); return }
        if (run !== active) { run?.sharedCritters?.clear(); transport.close(false); run = active; sent = ""; sampleAt = 0 }
        transport.useCompletion(origin, generation, nonce)
        transport.tick(context, now)
        if (!transport.ready) { run?.sharedCritters?.clear(); return }
        if (active == null || active.endedAt != null || now < sampleAt) return
        sampleAt = now + 500
        val payload = CritterProgressProtocol.encode(active, party)
        if ((payload != sent || now >= due) && transport.send(++sequence, "C", payload)) { sent = payload; due = now + 10_000 }
    }
    fun developerSnapshot() = transport.developerSnapshot()
    fun reset() { transport.close(false); run?.sharedCritters?.clear(); run = null; sent = ""; due = 0; sampleAt = 0 }
}

/** Stable species order is shared with S, but C evidence never enters S/capture/history ledgers. */
internal object CritterProgressProtocol {
    fun encode(run: SafariRun, party: PartySparklingState): String = JsonObject().apply {
        addProperty("version", 1)
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
    fun decode(payload: String): Map<String, SharedCritterProgress.Evidence>? = runCatching {
        if (payload.length > 3072) return null
        val p = JsonParser.parseString(payload).asJsonObject
        if (p.keySet() != setOf("version", "rows") || PartySyncProtocol.integer(p.get("version"), 1, 1) == null) return null
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
