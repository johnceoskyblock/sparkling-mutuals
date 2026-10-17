package net.johnceo.sparklingmutuals.safari.shots
import net.johnceo.sparklingmutuals.safari.shots.preview.contacts.*
import net.johnceo.sparklingmutuals.safari.shots.preview.*
import net.minecraft.client.Minecraft
data class ShotProbeTerrain(val fromParameter:Double,val toParameter:Double,val position:ShotVector?,val normal:ShotVector?,val block:String?,val shapes:List<ShotBounds>,val boundary:ContactStop?)
data class ShotContactProbeData(val shot:String,val model:String,val hypothesis:String,val eye:ShotVector,val aim:ShotVector,val pitch:Double,val sourceTick:Long,
 val coverage:ContactCoverage,val nodes:List<ContactNode>,val targets:List<ContactTarget>,val terrain:List<ShotProbeTerrain>,val primary:ContactCandidate?,val stop:ContactStop,
 val truncated:Boolean=false,val omittedNodes:Int=0,val omittedTargets:Int=0,val omittedTerrain:Int=0)
object ShotContactProbe {
 fun fromSnapshot(input:ShotObservation.Input,origin:ShotVector,s:ContactSceneSnapshot):ShotContactProbeData {
  val aliases=s.targets.take(64).mapIndexed{i,t->t.key to "p${i+1}"}.toMap()
  fun bounds(b:ShotBounds)=ShotBounds(b.min-origin,b.max-origin)
  return ShotContactProbeData(input.token,CapsulePathModel.MODEL_ID,ContactCurveSampler.HYPOTHESIS,input.pose.eye,input.pose.aim,input.pose.pitch.toDouble(),s.sourceTick,
   s.coverage,s.trace.nodes.map{it.copy(position=it.position-origin)},s.targets.take(64).map{it.copy(key=aliases.getValue(it.key),generation=1,bounds=bounds(it.bounds))},
   s.terrain.take(256).map{ShotProbeTerrain(it.fromParameter,it.toParameter,it.position?.minus(origin),it.normal,it.block,it.shapes.take(32).map(::bounds),it.boundary)},
   s.trace.primary?.let{it.copy(position=it.position-origin,targetKey=it.targetKey?.let(aliases::get))},s.trace.stop)
 }
 fun capture(client:Minecraft,input:ShotObservation.Input,origin:ShotVector):ShotContactProbeData =
  fromSnapshot(input,origin,CapsuleContactScene.calibrationSnapshot(client,input.pose.eye.let{ShotVector(it.x+origin.x,it.y+origin.y,it.z+origin.z)},input.pose.aim,input.pose.pitch.toDouble()))
 fun produce(input:ShotObservation.Input,eligible:Boolean,snapshot:()->ShotContactProbeData):ShotObservation.ContactProbe? =
  if(eligible && input.variant==ShotVariant.ORDINARY && input.hand==ShotHand.MAIN) ShotObservation.ContactProbe(snapshot()) else null

 internal fun validate(p:ShotContactProbeData) {
  require(p.shot.matches(Regex("[A-Za-z0-9_-]{1,48}")) && p.model==CapsulePathModel.MODEL_ID && p.hypothesis==ContactCurveSampler.HYPOTHESIS)
  require(p.pitch.isFinite() && p.pitch in -90.0..90.0 && p.sourceTick>=0 && p.nodes.size<=257 && p.targets.size<=64 && p.terrain.size<=256)
  require(p.coverage.reasons.all{it in ContactWarnings.allowed})
  require(p.targets.map{it.key}.distinct().size==p.targets.size)
  p.targets.forEach{require(it.key.matches(Regex("p([1-9]|[1-5][0-9]|6[0-4])")) && it.generation==1 &&
   it.proxy in setOf("client_body_v1","display_body_v1","armor_stand_body_v1") && net.johnceo.sparklingmutuals.safari.SafariRoster.all.any{mob->mob.name==it.species})}
  p.terrain.forEach{require(it.shapes.size<=32 && (it.block==null || it.block.matches(Regex("minecraft:[a-z0-9_]{1,96}"))))}
  p.primary?.let{require(it.targetKey==null || p.targets.any{t->t.key==it.targetKey} || p.truncated)
   require(it.proxy==null || it.proxy in setOf("client_body_v1","display_body_v1","armor_stand_body_v1"))}
 }
}
