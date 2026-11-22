package net.johnceo.sparklingmutuals.safari.visibility

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalVisibilityPolicyTest {
    @Test fun `only currently loaded visible locally identifiable targets qualify`() {
        assertTrue(LocalVisibilityPolicy.allows(true, true, true, true, 10.0, 100.0))
        assertFalse(LocalVisibilityPolicy.allows(false, true, true, true, 10.0, 100.0))
        assertFalse(LocalVisibilityPolicy.allows(true, false, true, true, 10.0, 100.0))
        assertFalse(LocalVisibilityPolicy.allows(true, true, false, true, 10.0, 100.0))
        assertFalse(LocalVisibilityPolicy.allows(true, true, true, false, 10.0, 100.0))
        assertFalse(LocalVisibilityPolicy.allows(true, true, true, true, 101.0, 100.0))
        assertFalse(LocalVisibilityPolicy.allows(true, true, true, true, Double.NaN, 100.0))
    }
    @Test fun `invisible carriers require visible names or equipped heads`() {
        assertFalse(LocalVisibilityPolicy.identifiable(false, false, false))
        assertTrue(LocalVisibilityPolicy.identifiable(false, true, false))
        assertTrue(LocalVisibilityPolicy.identifiable(false, false, true))
    }
    @Test fun `queued discovery discarded on lost sight can be discovered after visibility returns`() {
        val queue = net.johnceo.sparklingmutuals.hud.AlertSequence()
        val memory = VisibleDiscoveryMemory()
        var visible = true
        var fired = 0
        queue.submit("discovery", 100, "critter", valid = { visible }) {
            if (memory.first("critter", visible)) fired++
        }
        visible = false
        queue.tick(0)
        assertEquals(0, fired)
        assertFalse(memory.contains("critter"))
        visible = true
        queue.submit("discovery", 100, "critter", valid = { visible }) {
            if (memory.first("critter", visible)) fired++
        }
        queue.tick(1)
        assertEquals(1, fired)
        visible = false
        queue.tick(2)
        assertFalse(queue.busy)
    }
    @Test fun `Hideyho sighting is committed only when its banner starts`() {
        val watch = net.johnceo.sparklingmutuals.safari.hideyho.HideyhoLocationWatch()
        watch.context(true, net.johnceo.sparklingmutuals.safari.SafariBiome.HAUNTED)
        val spot = net.johnceo.sparklingmutuals.safari.hideyho.HideyhoHiding.SPOTS.first()
        assertEquals(spot, watch.settled(spot, 0, commit = false))
        assertEquals(spot, watch.settled(spot, 100, commit = false))
        assertEquals(spot, watch.settled(spot, 200))
        assertNull(watch.settled(spot, 300))
    }
    @Test fun `remote sightings do not consume first local discovery`() {
        val gate = VisibleDiscoveryMemory()
        assertFalse(gate.first("critter", false))
        assertFalse(gate.first("critter", false))
        assertTrue(gate.first("critter", true))
        assertFalse(gate.first("critter", true))
        gate.reset()
        assertTrue(gate.first("critter", true))
    }
}
