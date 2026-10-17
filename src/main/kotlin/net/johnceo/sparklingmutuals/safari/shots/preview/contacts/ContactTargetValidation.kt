package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
class ContactTargetValidation {
    private data class Checked(val target:ContactTarget,val error:String?)
    private val checked=HashMap<String,Checked>()
    private var remaining=0
    fun beginTick() { checked.clear();remaining=16 }
    fun reset() { checked.clear();remaining=0 }
    fun invalidate(key:String?=null) { if(key==null) checked.clear() else checked.remove(key) }
    fun validate(target:ContactTarget,inspect:(ContactTarget)->ContactTarget?):String? {
        checked[target.key]?.takeIf{it.target==target}?.let{return it.error}
        if(remaining==0) return "identity_budget"
        remaining--
        val error=if(inspect(target)==target) null else "stale_target"
        checked[target.key]=Checked(target,error)
        return error
    }
}
