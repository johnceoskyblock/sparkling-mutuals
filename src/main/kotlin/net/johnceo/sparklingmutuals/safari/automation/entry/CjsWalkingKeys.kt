package net.johnceo.sparklingmutuals.safari.automation.entry

import net.minecraft.client.KeyMapping

/** Native key boundary; supports hold and toggle mappings without changing preferences. */
class CjsWalkingKeys(private val forward: KeyMapping, private val sneak: KeyMapping,
    private val toggle: ()->Boolean, private val physicalForward: ()->Boolean, private val physicalSneak: ()->Boolean) {
    var owns = false
        private set
    private var initialSneak = false
    private fun sneakDown(down: Boolean) {
        if(toggle()) { if(sneak.isDown!=down) sneak.setDown(true) }
        else sneak.setDown(down)
    }
    fun drive(walk: Boolean, crouch: Boolean) {
        if(walk || crouch) {
            if(!owns) { initialSneak=sneak.isDown;owns=true }
            forward.setDown(walk);sneakDown(true)
        } else if(owns) {
            owns=false
            try { forward.setDown(physicalForward()) }
            finally {
                val restore=if(toggle()) { if(physicalSneak()) sneak.isDown else initialSneak } else physicalSneak()
                sneakDown(restore)
            }
        }
    }
}
