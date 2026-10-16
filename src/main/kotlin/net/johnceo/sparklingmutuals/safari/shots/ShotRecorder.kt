package net.johnceo.sparklingmutuals.safari.shots

import java.nio.file.Path
import java.util.UUID
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.compat.ShotPacketCompat.vector
import net.minecraft.client.Minecraft

data class ShotCommandResult(val ok:Boolean,val message:String)
data class ShotStartContext(val world:Any,val origin:ShotVector,val metadata:ShotMetadata,val inSafari:Boolean=true)

/** Client-thread state machine. Worker completions are marshalled and epoch-checked before changing state. */
class ShotRecorderController @JvmOverloads constructor(private val directory:Path,private val context:()->ShotStartContext?,
    private val nanoTime:()->Long,private val schedule:((()->Unit))->Unit,private val notice:(String)->Unit,
    private val bind:(ShotObservationAdapter?,()->ShotStamp,(Throwable)->Unit)->Unit,
    private val open:(Path,String,ShotByteBudget,(ShotExportResult)->Unit)->ShotExportWriter=ShotExportWriter::open,
    private val diagnose:(ShotFailure)->Unit=ShotDiagnostics::report) {
    private var epoch=0L
    private var started=0L
    private var tick=0L
    private var world:Any?=null
    private var session:ShotSession?=null
    private var writer:ShotExportWriter?=null
    private var adapter:ShotObservationAdapter?=null
    private var requestedStop:ShotStopReason?=null
    private var last=ShotStatus(null,false,null,0,0,0,0)
    private var dispatch=-1L
    private var error:String?=null
    var lastExport:Path?=null
        private set
    val phase get()=if(writer==null) "off" else if(session==null && requestedStop==null) "opening" else if(session?.status()?.recording==true) "recording" else "finalizing"
    private fun stamp()=ShotStamp(((nanoTime()-started)/1000).coerceAtLeast(0),tick)
    fun status()=session?.status() ?: last.copy(recording=false)
    fun describe():String {
        val s=status();val elapsed=if(writer==null) 0 else stamp().micros/1_000_000
        return "Shot recorder: $phase; session=${s.session ?: "none"}; elapsed=${elapsed}s; remaining=${(session?.metadata?.durationSeconds?.minus(elapsed) ?: 0).coerceAtLeast(0)}s; inputs=${s.inputCount}/300; events=${s.eventCount}/100000; bytes=${s.encodedBytes}/10485760; queue=${s.queuedBytes}/4194304; stop=${s.stopReason ?: "none"}; error=${error ?: "none"}. Last export: ${lastExport ?: "none"}"
    }
    fun start(seconds:Int=180):ShotCommandResult {
        if(seconds !in 10..600) return ShotCommandResult(false,"Duration must be 10–600 seconds.")
        if(writer!=null) return ShotCommandResult(false,"Shot recorder is $phase; wait for export to finish.")
        val c=context()?.takeIf { it.inSafari } ?: return ShotCommandResult(false,"Start recording in a loaded Safari world.")
        val token=UUID.randomUUID().toString();val metadata=c.metadata.copy(session=token,durationSeconds=seconds)
        val budget=ShotByteBudget();val current=++epoch
        started=nanoTime();tick=0;world=c.world;requestedStop=null;dispatch=-1;error=null
        last=ShotStatus(token,false,null,0,0,0,0)
        val w=open(directory,token,budget) { result -> schedule { workerCompleted(current,result) } }
        writer=w
        w.ready.whenComplete { _,failure -> schedule {
            if(current!=epoch || writer!==w) return@schedule
            if(failure!=null) return@schedule // Completion owns the single local error notice.
            val reducer=ShotSession(metadata,ShotLimits(),w,budget,::recordFailure);session=reducer
            if(requestedStop==null) requestedStop=boundaryReason()
            if(requestedStop!=null) { reducer.stop(stamp(),requestedStop!!);last=reducer.status();return@schedule }
            adapter=ShotObservationAdapter(c.origin) { _,observation -> accept(observation) }
            bind(adapter,::stamp) { failure ->
                if(failure is ShotObservationFailure) fail(failure.stage,failure.cause ?: failure) else fail("observation",failure)
            }
            notice("Shot recording started ($seconds seconds), local files only. Use /sm shots status or stop.")
        } }
        return ShotCommandResult(true,"Opening local shot recorder…")
    }
    private fun recordFailure(failure:ShotFailure) {
        if(error!=null) return
        error="${failure.stage}/${failure.cause.javaClass.simpleName}; session=${failure.session}; see latest.log"
        ShotDiagnostics.safely(diagnose,failure)
        notice("Shot recorder error: $error")
    }
    fun fail(stage:String,failure:Throwable) {
        if(writer==null || requestedStop!=null) return
        recordFailure(ShotFailure(status().session ?: "opening",stage,stamp(),failure))
        stop(ShotStopReason.ERROR)
    }
    private fun boundaryReason():ShotStopReason? {
        val c=context() ?: return ShotStopReason.DISCONNECT
        if(c.world!==world) return ShotStopReason.WORLD_CHANGE
        if(!c.inSafari) return ShotStopReason.SAFARI_EXIT
        return null
    }
    private fun valid():Boolean {
        if(writer==null || requestedStop!=null) return false
        boundaryReason()?.let { stop(it);return false }
        return session?.status()?.recording==true
    }
    fun accept(observation:ShotObservation):Boolean {
        if(!valid()) return false
        val accepted=session!!.accept(stamp(),observation)
        if(!session!!.status().recording) finishObservation()
        return accepted
    }
    fun feedback(value:ShotFeedbackObservation,sequence:Long) {
        if(!valid() || sequence<=dispatch) return
        dispatch=sequence;accept(ShotObservation.Feedback(value,sequence))
    }
    fun label(scenario:ShotScenario,split:ShotSplit):ShotCommandResult {
        if(!valid()) return ShotCommandResult(false,"Start a recording before labelling inputs.")
        adapter?.scenario=scenario;adapter?.split=split
        return ShotCommandResult(true,"Subsequent inputs: ${scenario.name.lowercase()} / ${split.name.lowercase()}.")
    }
    fun mark(token:String,outcome:ShotManualOutcome):ShotCommandResult {
        if(!token.matches(Regex("[A-Za-z0-9_-]{1,48}")) || !valid()) return ShotCommandResult(false,"Use an existing shot ID in an active recording.")
        return if(accept(ShotObservation.Annotation(token,outcome))) ShotCommandResult(true,"Manual annotation recorded for $token: ${outcome.name.lowercase()}.")
        else ShotCommandResult(false,"Unknown shot ID: $token.")
    }
    fun tick() {
        if(writer==null) return
        tick++
        if(!valid()) return
        session!!.advance(stamp())
        if(!session!!.status().recording) finishObservation()
    }
    private fun finishObservation() {
        last=session?.status() ?: last
        requestedStop=last.stopReason ?: requestedStop
        bind(null,::stamp) {};adapter?.reset();adapter=null
    }
    fun stop(reason:ShotStopReason):ShotCommandResult {
        if(writer==null) return ShotCommandResult(true,"Recorder is off. Last export: ${lastExport ?: "none"}")
        if(requestedStop==null) requestedStop=reason
        session?.stop(stamp(),requestedStop!!);finishObservation()
        return ShotCommandResult(true,"Shot observation stopped; local export is finalizing.")
    }
    fun workerCompleted(completedEpoch:Long,result:ShotExportResult) {
        if(completedEpoch!=epoch || writer==null) return
        if(session?.status()?.recording==true) session!!.stop(stamp(),ShotStopReason.ERROR)
        finishObservation()
        if(result.error==null) {
            lastExport=result.path
            notice("Shot export saved${if(result.complete) "" else " (incomplete)"}: ${result.path}")
        } else { error="${result.error}; session=${last.session}; see latest.log";notice("Shot recording stopped: $error. A partial file may remain in $directory.") }
        writer=null;session=null;world=null
    }
    fun shutdown() { stop(ShotStopReason.SHUTDOWN);writer?.shutdown(250) }
}

object ShotRecorder {
    private var registered=false
    private var gameDispatch=0L
    private val client get()=Minecraft.getInstance()
    val directory:Path get()=FabricLoader.getInstance().gameDir.resolve("sparkling-mutuals/shots")
    private val controller by lazy { ShotRecorderController(directory,::context,System::nanoTime,
        { task -> client.execute(task) },{ text -> client.player?.sendSystemMessage(LocalChat.component(text)) },ShotObservationHooks::bind) }
    private fun context():ShotStartContext? {
        val level=client.level ?: return null;val p=client.player ?: return null
        val loader=FabricLoader.getInstance()
        fun version(id:String)=loader.getModContainer(id).map { it.metadata.version.friendlyString }.orElse("unknown")
        return ShotStartContext(level,vector(p.eyePosition),ShotMetadata("pending",version("minecraft"),version("sparkling-mutuals"),
            version("fabricloader"),level.dimension().identifier().toString(),ConfigManager.hideFlyingCapsules,ConfigManager.hideGroundCapsules),SafariAssist.inSafari)
    }
    fun register() {
        if(registered) return;registered=true
        ClientReceiveMessageEvents.GAME.register { message,overlay ->
            if(!overlay && controller.status().recording) runCatching { ShotFeedback.parse(message.string)?.let { controller.feedback(it,++gameDispatch) } }
                .onFailure { controller.fail("feedback",it) }
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register { shutdown() }
    }
    fun start(seconds:Int=180)=controller.start(seconds)
    fun stop(reason:ShotStopReason)=controller.stop(reason)
    fun status()=controller.status()
    fun describe()=controller.describe()
    fun label(scenario:ShotScenario,split:ShotSplit)=controller.label(scenario,split)
    fun mark(token:String,outcome:ShotManualOutcome)=controller.mark(token,outcome)
    fun tick(client:Minecraft) {
        try { controller.tick();if(controller.status().recording) ShotObservationHooks.tick(client) }
        catch(failure:Exception) { controller.fail("tick",failure) }
    }
    fun disconnect() { controller.stop(ShotStopReason.DISCONNECT) }
    fun shutdown() { controller.shutdown() }
}
