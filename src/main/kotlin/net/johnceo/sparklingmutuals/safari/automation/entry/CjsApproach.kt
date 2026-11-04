package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import kotlin.math.*

data class CjsApproachFrame(val context: EntryContext?, val enabled: Boolean, val signal: CjsRoomSignal?,
    val target: UUID?, val x: Double, val y: Double, val z: Double, val managerX: Double, val managerZ: Double,
    val yaw: Float, val pitch: Float, val safe: Boolean, val eyeY: Double = y + 1.62, val managerY: Double = y + 1.62,
    val crouched: Boolean = true, val velocity: Double = 0.0, val acceleration: Double = .03, val drag: Double = .546)
data class CjsApproachAction(val forward: Boolean = false, val yaw: Float? = null, val pitch: Float? = null,
    val status: String, val crouch: Boolean = false, val outcome: CjsApproachOutcome = CjsApproachOutcome.MOVING)

/** One instance per independently observed world arrival. Only ordinary input is proposed. */
class CjsApproach {
    private var origin: CjsApproachFrame? = null
    private var last: CjsApproachFrame? = null
    private var started = 0L
    private var progressed = 0L
    private var previous = 0L
    private var travelled = 0.0
    private var yaw = 0f
    private var pitch = 0f
    private var terminal = false
    private var status = "Waiting for room discovery and clear approach"
    private var outcome = CjsApproachOutcome.WAITING
    fun cancel() { stop("Approach cancelled") }
    fun aimMatches(currentYaw: Float, currentPitch: Float): Boolean = origin==null ||
        abs(((currentYaw-yaw+540)%360+360)%360-180)<=3 && abs(currentPitch-pitch)<=3
    fun heading(): Float? = yaw.takeIf { origin!=null }
    private fun stop(reason: String, result: CjsApproachOutcome = CjsApproachOutcome.CANCELLED): CjsApproachAction {
        terminal = true; status = reason; outcome=result
        return CjsApproachAction(status = reason, outcome=result)
    }
    fun tick(f: CjsApproachFrame, now: Long): CjsApproachAction {
        if (terminal) return CjsApproachAction(status = status, outcome=outcome)
        if (now < previous || now < 0) return stop("Approach cancelled: clock rollback")
        previous = now
        val current=f.context
        val valid=current != null && current.account != current.party.leader && current.party.members.size>1 &&
            f.enabled && f.safe && f.target != null && f.signal?.valid(current)==true &&
            listOf(f.x,f.y,f.z,f.managerX,f.managerZ,f.eyeY,f.managerY,f.yaw.toDouble(),f.pitch.toDouble(),
                f.velocity,f.acceleration,f.drag).all { it.isFinite() } &&
            f.velocity in 0.0.. .16 && f.acceleration in 0.0.. .2 && f.drag in .45.. .638
        val first=origin
        if (!valid) return if (first != null) stop("Approach cancelled: context, input or evidence unavailable") else CjsApproachAction(status=status, outcome=outcome)
        if (first == null) {
            val dx=f.managerX-f.x;val dz=f.managerZ-f.z;val distance=hypot(dx,dz)
            if (distance !in 1.0..6.0) return CjsApproachAction(status="Waiting for nearby Manager")
            origin=f;last=f;started=now;progressed=now;outcome=CjsApproachOutcome.MOVING
            yaw=(atan2(-dx,dz)*180/PI).toFloat()
            pitch=(-atan2(f.managerY-f.eyeY,distance)*180/PI).toFloat().coerceIn(-80f,80f)
            status="Facing Manager"
            return CjsApproachAction(yaw=yaw,pitch=pitch,status=status,crouch=true)
        }
        if (f.context != first.context || f.target != first.target || f.signal.generation != first.signal?.generation ||
            hypot(f.managerX-first.managerX,f.managerZ-first.managerZ)>.2 || abs(f.y-first.y)>.2)
            return stop("Approach cancelled: destination or target changed")
        if (now-started>=1500 || now-progressed>=350) return stop("Approach cancelled: time or progress limit")
        val yawError=abs(((f.yaw-yaw+540)%360+360)%360-180)
        if (yawError>3 || abs(f.pitch-pitch)>3) return stop("Approach cancelled: camera takeover")
        val old=last!!;val step=hypot(f.x-old.x,f.z-old.z)
        if (step>.35) return stop("Approach cancelled: unexpected displacement")
        val directionX=-sin(yaw*PI/180);val directionZ=cos(yaw*PI/180)
        val along=(f.x-old.x)*directionX+(f.z-old.z)*directionZ
        val cross=abs((f.x-first.x)*directionZ-(f.z-first.z)*directionX)
        if(along<-.01 || cross>.03) return stop("Approach cancelled: left checked corridor")
        if (along>.002) progressed=now
        travelled+=step;last=f
        if (travelled>=.85) return stop("Approach complete: movement budget used", CjsApproachOutcome.COMPLETE)
        // Native first forward tick otherwise uses the PREVIOUS input's crouch state.
        if(!f.crouched) return CjsApproachAction(yaw=yaw,pitch=pitch,status="Priming crouch",crouch=true)
        // One more commanded tick plus all subsequent ordinary coast must fit the budget.
        if(travelled+(f.velocity+f.acceleration)/(1-f.drag)>.99)
            return stop("Approach complete: reserved stopping distance", CjsApproachOutcome.COMPLETE)
        status="Approaching Manager (one-block maximum)"
        return CjsApproachAction(true,yaw,pitch,status,crouch=true)
    }
}
