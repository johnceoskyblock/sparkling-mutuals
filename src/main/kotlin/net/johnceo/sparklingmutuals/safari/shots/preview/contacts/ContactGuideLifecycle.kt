package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreviewFrame
/** Shared only within one render frame, and only for the same current view and scene. */
class ContactGuideLifecycle {
 private var world:Any?=null;private var view:Any?=null;private var frame:CapsulePreviewFrame?=null
 fun resolve(world:Any,view:Any,build:()->CapsulePreviewFrame):CapsulePreviewFrame {
  if(this.world===world && this.view==view) frame?.let{return it}
  return build().also{this.world=world;this.view=view;frame=it}
 }
 fun reset() { world=null;view=null;frame=null }
}
