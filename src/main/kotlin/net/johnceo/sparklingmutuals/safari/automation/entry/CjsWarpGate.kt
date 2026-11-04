package net.johnceo.sparklingmutuals.safari.automation.entry

import net.johnceo.sparklingmutuals.safari.SafariRules

/** Non-overlay server GAME notice, then one world transition, then current leader attribution. */
class CjsWarpGate {
    private var currentWorld: Any? = null
    private var origin: Any? = null
    private var destination: Any? = null
    private var name: String? = null
    private var at = 0L
    private var arrivedAt = 0L
    private var verified: EntryContext? = null
    var generation = 0L
        private set
    fun observe(raw: String, now: Long) {
        if (currentWorld==null || raw.length>512 || '\n' in raw || '\r' in raw || now<0) return
        val leader=Regex("^Party Leader, (?:\\[[^]\\r\\n]+] )*([A-Za-z0-9_]{1,16}), summoned you to their server\\.$")
            .matchEntire(SafariRules.strip(raw))?.groupValues?.get(1) ?: return
        // Duplicate delivery before a transfer does not replenish time or input budget.
        if(origin===currentWorld && name==leader && now>=at && now-at<10000) return
        origin=currentWorld;destination=null;name=leader;at=now;verified=null;generation++
    }
    fun world(current: Any?, now: Long) {
        if(currentWorld===current) return
        destination=if(name!=null && origin===currentWorld && current!=null && now>=at && now-at<10000) current else null
        if(destination!=null) arrivedAt=now
        currentWorld=current
    }
    fun allows(context: EntryContext?, profiles: List<EntryPeer>, now: Long): Boolean {
        if(context==null || destination==null || destination!==currentWorld || now<arrivedAt || now-arrivedAt>=12000) return false
        if(verified!=null && verified!=context) { destination=null;return false }
        val leader=profiles.filter { it.name.equals(name,true) }.singleOrNull() ?: return false
        if(leader.id!=context.party.leader || leader.id !in context.party.members || context.account==leader.id) return false
        verified=context
        return true
    }
}
