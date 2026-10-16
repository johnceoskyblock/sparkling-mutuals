package net.johnceo.sparklingmutuals.safari.shots

import java.nio.file.Path

data class ShotStamp(val micros: Long, val tick: Long)
data class ShotVector(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: ShotVector) = ShotVector(x - other.x, y - other.y, z - other.z)
    fun distanceSquared(other: ShotVector): Double = (x-other.x)*(x-other.x)+(y-other.y)*(y-other.y)+(z-other.z)*(z-other.z)
}
data class ShotBounds(val min: ShotVector, val max: ShotVector)
data class ShotQuaternion(val x: Float, val y: Float, val z: Float, val w: Float)
enum class ShotVariant(val itemId: String) { ORDINARY("CRITTER_CAPSULE"), MASTERFUL("MASTERFUL_CRITTER_CAPSULE");
    companion object { fun fromId(id: String?) = entries.firstOrNull { it.itemId == id } }
}
enum class ShotHand { MAIN, OFF }
enum class ShotUseRoute { AIR, BLOCK }
enum class ShotUseResult { PASS, SUCCESS, FAIL, OTHER }
data class ShotInputToken(val value: String)
enum class ShotScenario { UNSPECIFIED, STATIONARY_LEVEL, STATIONARY_UP, STATIONARY_DOWN, WALKING_FORWARD,
    WALKING_SIDEWAYS, MOVING_TARGET, SOLID_OBSTRUCTION, EDGE_OBSTRUCTION, RAPID_INPUTS, NEARBY_THROWER, HIDING_COMPARISON }
enum class ShotSplit { FIT, VALIDATION, UNASSIGNED }
enum class ShotManualOutcome { HIT, CAPTURE, ESCAPE, MISS, UNKNOWN }
enum class ShotUnavailable { NOT_EXPOSED, NOT_OBSERVED, MISSING_BASE, UNLOADED, TRUNCATED }
enum class ShotEntityAction { DISCOVERED, SAMPLE, REMOVED }
enum class ShotDiscovery { SPAWN, METADATA, BOOTSTRAP }
enum class ShotRemoval { SERVER_REMOVE, UNLOADED, WINDOW_EXPIRED, RECORDING_STOPPED }
enum class ShotUpdateKind { SPAWN, MOVE, POSITION_SYNC, TELEPORT, MOTION, METADATA }
enum class ShotGap { CACHE_BACKLOG, CAPSULE_LIMIT, TARGET_LIMIT, MISSING_BASE, WINDOW_EXPIRED, RAW_PATH_TRUNCATED, SCAN_PENDING }
enum class ShotAssociation { UNMATCHED, CANDIDATE_UNIQUE, AMBIGUOUS, MANUAL }
enum class ShotFeedbackKind { HIT, ESCAPE, PERSONAL_CAPTURE, SHARED_CAPTURE, STARTLE }
enum class ShotFeedbackSource { GAME }
enum class ShotStopReason { USER_STOP, EXPORT, DURATION_LIMIT, INPUT_LIMIT, EVENT_LIMIT, BYTE_LIMIT, QUEUE_LIMIT,
    WORLD_CHANGE, SAFARI_EXIT, DISCONNECT, SHUTDOWN, ERROR }
data class ShotFeedbackObservation(val kind: ShotFeedbackKind, val species: String, val variant: ShotVariant? = null)
data class ShotPose(val position: ShotVector, val eye: ShotVector, val camera: ShotVector, val yaw: Float,
    val pitch: Float, val aim: ShotVector, val velocity: ShotVector, val onGround: Boolean = false,
    val sneaking: Boolean = false, val sprinting: Boolean = false, val thirdPerson: Boolean = false,
    val latencyMs: Int? = null, val latencyUnavailable: ShotUnavailable? = ShotUnavailable.NOT_EXPOSED,
    val crosshair: ShotCrosshair? = null, val nearbyPlayers: Int = 0)
data class ShotCrosshair(val kind: String, val position: ShotVector?, val target: String? = null)
data class ShotPathPoint(val position: ShotVector, val tickOffset: Int = 0)

/** Only typed, allowlisted values cross the export boundary. Runtime entity IDs and UUIDs stay in the adapter. */
sealed interface ShotObservation {
    data class PlayerState(val pose:ShotPose):ShotObservation
    data class Input(val token: String, val hand: ShotHand, val variant: ShotVariant, val route: ShotUseRoute,
        val pose: ShotPose, val scenario: ShotScenario = ShotScenario.UNSPECIFIED, val split: ShotSplit = ShotSplit.UNASSIGNED,
        val standalone: Boolean = false) : ShotObservation
    data class Continuation(val token: String, val result: ShotUseResult, val route: ShotUseRoute) : ShotObservation
    data class Capsule(val token: String, val generation: Int, val variant: ShotVariant, val action: ShotEntityAction,
        val position: ShotVector, val velocity: ShotVector? = null, val discovery: ShotDiscovery = ShotDiscovery.BOOTSTRAP,
        val flying: Boolean = true, val removal: ShotRemoval? = null, val spawnMicros: Long? = null) : ShotObservation
    data class Target(val token: String, val generation: Int, val species: String, val position: ShotVector,
        val bounds: ShotBounds, val velocity: ShotVector? = null, val source: String = "descriptor") : ShotObservation
    data class Packet(val token: String, val update: ShotUpdateKind, val points: List<ShotPathPoint> = emptyList(),
        val relative: Boolean = false, val baseKnown: Boolean = true, val motion: ShotVector? = null,
        val yaw: Float? = null, val pitch: Float? = null, val relativeFlags: List<String> = emptyList(),
        val observedMicros: Long? = null, val base: ShotVector? = null, val truncated: Boolean = false,
        val resolvedPoints:List<ShotPathPoint> = emptyList(),val endPosition:ShotVector?=null,val resolvedEnd:ShotVector?=null) : ShotObservation
    data class DisplayState(val token: String, val context: String, val translation: ShotVector, val scale: ShotVector,
        val leftRotation: ShotQuaternion, val rightRotation: ShotQuaternion, val interpolationDelay: Int,
        val interpolationDuration: Int, val positionInterpolationDuration: Int) : ShotObservation
    data class Obstruction(val shot: String?, val track: String?, val loaded: Boolean, val from: ShotVector,
        val to: ShotVector, val position: ShotVector? = null, val face: String? = null, val block: String? = null,
        val bounds: List<ShotBounds> = emptyList()) : ShotObservation
    data class Feedback(val feedback: ShotFeedbackObservation, val dispatch: Long, val source:ShotFeedbackSource=ShotFeedbackSource.GAME) : ShotObservation
    data class Annotation(val token: String, val outcome: ShotManualOutcome) : ShotObservation
    data class Gap(val reason: ShotGap) : ShotObservation
}
data class ShotMetadata(val session: String, val minecraft: String, val mod: String, val fabric: String,
    val dimension: String, val hideFlying: Boolean, val hideGround: Boolean, val durationSeconds: Int = 180,
    val recorderVersion: Int = 2, val schemaVersion: Int = 1, val modelVersion: String? = null,
    val coordinates: String = "session_relative_game_axes", val timeBasis: String = "client_monotonic_microseconds")
data class ShotSummary(val token: String, val variant: ShotVariant, val scenario: ShotScenario, val split: ShotSplit,
    val tracks: List<String>, val association: ShotAssociation, val feedback: List<Long>, val annotations: List<ShotManualOutcome>)
sealed interface ShotRecord {
    data class Header(val metadata: ShotMetadata, val limits: ShotLimits) : ShotRecord
    data class Event(val sequence: Long, val micros: Long, val tick: Long, val observation: ShotObservation) : ShotRecord
    data class Footer(val sequence: Long, val micros: Long, val tick: Long, val reason: ShotStopReason,
        val complete: Boolean, val eventCount: Int, val inputCount: Int, val encodedBytesBeforeFooter: Long,
        val shots: List<ShotSummary>, val gaps: Boolean) : ShotRecord
}
data class ShotExportResult(val path: Path?, val complete: Boolean, val error: String? = null)
data class ShotStatus(val session: String?, val recording: Boolean, val stopReason: ShotStopReason?,
    val inputCount: Int, val eventCount: Int, val encodedBytes: Long, val queuedBytes: Int)
interface ShotBufferBudget { fun tryReserve(bytes: Int): Boolean; fun release(bytes: Int); val usedBytes: Int }
/** The producer reserves bytes. An accepted sink owns and eventually releases them; rejection leaves ownership with producer. */
interface ShotSink { fun offer(line: ByteArray): Boolean; fun finish(footer: ByteArray); fun queuedBytes(): Int }
