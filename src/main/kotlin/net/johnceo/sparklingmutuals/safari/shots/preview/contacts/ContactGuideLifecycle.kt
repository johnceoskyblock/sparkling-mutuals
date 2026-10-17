package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreviewFrame
class ContactGuideLifecycle {
 private var world:Any?=null;private var aim:ShotVector?=null;private var frame:CapsulePreviewFrame?=null
 fun store(world:Any,aim:ShotVector,frame:CapsulePreviewFrame) { this.world=world;this.aim=aim;this.frame=frame }
 fun current(world:Any?,aim:ShotVector):CapsulePreviewFrame? = frame.takeIf{this.world===world && this.aim==aim}
 fun reset() { world=null;aim=null;frame=null }
}
