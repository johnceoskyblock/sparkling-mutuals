package net.johnceo.sparklingmutuals.safari.shots

import org.slf4j.LoggerFactory

/** Local diagnostics only: never an observation, JSONL field, chat dump or upload. */
data class ShotFailure(val session:String,val stage:String,val stamp:ShotStamp?,val cause:Throwable)
class ShotObservationFailure(val stage:String,cause:Throwable):RuntimeException(cause)
object ShotDiagnostics {
    private val logger=LoggerFactory.getLogger("sparkling-mutuals/shot-recorder")
    fun report(failure:ShotFailure) {
        logger.error("Shot recorder failure: session={} stage={} micros={} tick={}; recording incomplete",
            failure.session,failure.stage,failure.stamp?.micros,failure.stamp?.tick,failure.cause)
    }
    fun safely(report:(ShotFailure)->Unit,failure:ShotFailure) {
        try { report(failure) } catch(_:Exception) { /* Diagnostics must never interrupt gameplay or export cleanup. */ }
    }
}
