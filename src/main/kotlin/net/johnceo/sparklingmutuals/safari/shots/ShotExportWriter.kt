package net.johnceo.sparklingmutuals.safari.shots

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.io.IOException
import com.google.gson.JsonParser

/** One worker owns disk I/O. The monitor is never held during open/write/close/move. */
class ShotExportWriter private constructor(private val directory: Path, token: String,
    private val budget: ShotByteBudget, private val completed: (ShotExportResult)->Unit,
    private val openStream: (Path)->OutputStream,private val diagnose:(ShotFailure)->Unit) : ShotSink {
    private val sessionToken=token
    val ready = CompletableFuture<Unit>()
    val result = CompletableFuture<ShotExportResult>()
    private val partial = directory.resolve("$token.jsonl.part")
    private val destination = directory.resolve("$token.jsonl")
    private val lock = Any()
    private val queue = ArrayDeque<ByteArray>()
    private var pending = 0
    private var footer: ByteArray? = null
    private var closing = false
    private var finished = false
    @Volatile private var aborted = false
    private val worker = Thread(::write, "sparkling-shot-export").apply { isDaemon=true;start() }
    override fun queuedBytes() = synchronized(lock) { pending }
    override fun offer(line: ByteArray): Boolean = synchronized(lock) {
        if (closing || finished || aborted || line.size !in 1..65536 || pending > budget.maxBytes-line.size) return false
        queue.addLast(line);pending+=line.size;true
    }
    override fun finish(footer: ByteArray) {
        synchronized(lock) {
            if (finished || aborted) { budget.release(65536);return }
            if (closing) return
            closing=true;this.footer=footer
        }
    }
    private fun write() {
        var outcome = ShotExportResult(partial,false,"io_error")
        var stage="writer/open"
        try {
            Files.createDirectories(directory)
            openStream(partial).use { output ->
                ready.complete(Unit)
                stage="writer/write"
                while (!aborted) {
                    var terminal = false
                    val line = synchronized(lock) {
                        if (queue.isNotEmpty()) queue.removeFirst()
                        else footer?.also { footer=null;terminal=true }
                    }
                    if (line == null) { Thread.sleep(5);continue }
                    try { output.write(line) }
                    finally {
                        if (terminal) budget.release(65536)
                        else { budget.release(line.size);synchronized(lock) { pending-=line.size } }
                    }
                    if (terminal) {
                        output.flush()
                        if (aborted) break
                        val complete = JsonParser.parseString(line.toString(Charsets.UTF_8)).asJsonObject["complete"].asBoolean
                        outcome=ShotExportResult(destination,complete)
                        break
                    }
                }
                stage="writer/close"
            }
            if (!aborted && outcome.path == destination) {
                // Default move has no REPLACE_EXISTING; ATOMIC_MOVE may overwrite an existing target on some providers.
                stage="writer/publish"
                Files.move(partial,destination)
            } else outcome=ShotExportResult(partial,false,"shutdown_timeout")
        } catch (failure: Exception) {
            ShotDiagnostics.safely(diagnose,ShotFailure(sessionToken,stage,null,failure))
            outcome=ShotExportResult(partial,false,if (aborted) "shutdown_timeout" else "io_error")
        }
        finally {
            synchronized(lock) {
                finished=true
                while (queue.isNotEmpty()) budget.release(queue.removeFirst().size)
                pending=0
                if (footer != null) { footer=null;budget.release(65536) }
            }
            if (!ready.isDone) ready.completeExceptionally(IOException("shot_export_open_failed"))
            result.complete(outcome)
            runCatching { completed(outcome) }
        }
    }
    fun shutdown(timeoutMillis: Long = 250) {
        worker.join(timeoutMillis.coerceIn(1,250))
        if (worker.isAlive) { aborted=true;worker.interrupt() }
    }
    companion object {
        fun openWithDiagnostics(directory:Path,sessionToken:String,budget:ShotByteBudget,completed:(ShotExportResult)->Unit,
            diagnose:(ShotFailure)->Unit,openStream:(Path)->OutputStream={ Files.newOutputStream(it,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE) }):ShotExportWriter {
            require(sessionToken.matches(Regex("[A-Za-z0-9_-]{1,48}"))) { "invalid_session_token" }
            return ShotExportWriter(directory,sessionToken,budget,completed,openStream,diagnose)
        }
        fun open(directory: Path, sessionToken: String, budget: ShotByteBudget, completed: (ShotExportResult)->Unit,
            openStream: (Path)->OutputStream = { Files.newOutputStream(it,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE) }): ShotExportWriter {
            require(sessionToken.matches(Regex("[A-Za-z0-9_-]{1,48}"))) { "invalid_session_token" }
            return openWithDiagnostics(directory,sessionToken,budget,completed,ShotDiagnostics::report,openStream)
        }
    }
}
