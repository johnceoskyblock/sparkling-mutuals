package net.johnceo.sparklingmutuals.safari.visibility

import net.johnceo.sparklingmutuals.hud.AlertSequence
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VisibleSparklingPolicyTest {
    @Test fun `started visible alert survives occlusion until its normal expiry`() {
        val queue = AlertSequence()
        var visible = true
        var started = false
        queue.submit("sparkling", 7000, valid = { VisibleSparklingPolicy.alertVisible(visible, started) }) { started = true }
        queue.tick(0)
        assertTrue(started)
        visible = false
        queue.tick(1)
        assertTrue(queue.busy)
        queue.tick(6999)
        assertTrue(queue.busy)
        queue.tick(7000)
        assertFalse(queue.busy)
    }
    @Test fun `occluded pending alert never starts and reset still clears a started alert`() {
        val queue = AlertSequence()
        var started = false
        queue.submit("sparkling", 7000, valid = { VisibleSparklingPolicy.alertVisible(false, started) }) { started = true }
        queue.tick(0)
        assertFalse(started)
        queue.submit("sparkling", 7000, valid = { VisibleSparklingPolicy.alertVisible(true, started) }) { started = true }
        queue.tick(1)
        queue.clear("sparkling")
        assertFalse(queue.busy)
    }
    @Test fun `ordinary species accept visible body or vanilla name but hidden species need body`() {
        assertTrue(VisibleSparklingPolicy.sighting(true,false,false))
        assertTrue(VisibleSparklingPolicy.sighting(false,true,false))
        assertFalse(VisibleSparklingPolicy.sighting(false,false,false))
        assertFalse(VisibleSparklingPolicy.sighting(false,true,true))
        assertTrue(VisibleSparklingPolicy.sighting(true,false,true))
    }
}
