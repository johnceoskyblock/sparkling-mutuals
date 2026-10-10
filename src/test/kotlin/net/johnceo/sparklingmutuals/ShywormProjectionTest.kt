package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class ShywormProjectionTest {
    private val id = UUID.randomUUID()
    @Test fun `return to start keeps timer despite lingering labels and warns first side throughout rest`() {
        val p = ShywormProjection()
        p.observe(id, 10.0, 58.65, 20.0, true, 0)
        p.observe(id, 10.5, 58.65, 20.0, true, 250)
        p.observe(id, 17.0, 58.65, 20.0, true, 500)
        p.chat("The Shyworm saw a player and fled!", 750, 10.0, 20.0)
        p.observe(id, 10.0, 57.0, 20.0, true, 750)
        p.observe(id, 10.0, 57.0, 20.0, true, 1000)
        p.reconcile(1000)
        assertEquals("7.8s", p.timer(id, 1000)?.text)
        p.observe(id, 10.0, 57.0, 20.0, true, 1500)
        assertNotNull(p.timer(id, 1500))
        assertEquals(58.65, p.hiddenHeight(id))
        val first = p.path(id, 7000)!!
        assertEquals(10.0, first.minX, .001); assertEquals(17.0, first.maxX, .001)
        assertEquals(19.5, first.minZ, .001); assertEquals(20.5, first.maxZ, .001)
        assertEquals(60.0, first.height)
        p.observe(id, 10.5, 58.65, 20.0, true, 9000)
        assertNull(p.timer(id, 9000))
        assertEquals(16.5, p.path(id, 9000)!!.minX, .001)
    }
    @Test fun `all rest messages survive return teleport and use known ground levels`() {
        for ((ground, message) in listOf(40.0 to "The Shyworm hid back into the ground.",
            43.0 to "The Shyworm saw a player and fled!", 60.0 to "The Shyworm fled because it was startled!")) {
            val p = ShywormProjection()
            p.observe(id, 10.0, ground - 1.35, 20.0, true, 0)
            p.observe(id, 10.0, ground - 1.35, 20.5, true, 250)
            p.observe(id, 17.0, ground - 1.35, 27.0, false, 500)
            p.chat(message, 750, 10.0, 20.0)
            p.observe(id, 10.0, 1.0, 20.0, false, 750)
            p.reconcile(1000)
            assertNotNull(p.timer(id, 1000))
            assertEquals(ground, p.path(id, 8750)!!.height)
            assertEquals(7.0, p.path(id, 8750)!!.maxZ - p.path(id, 8750)!!.minZ, .001)
        }
    }
    @Test fun `different worms have independent rest deadlines and deadline colors`() {
        val p = ShywormProjection(); val other = UUID.randomUUID()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.observe(other, -110.0, 50.0, 30.0, true, 0)
        p.chat("The Shyworm hid back into the ground.", 250)
        p.observe(id, -100.0, 1.0, 20.0, false, 500); p.reconcile(500)
        p.observe(other, -110.0, 50.0, 30.0, true, 1500)
        p.chat("The Shyworm saw a player and fled!", 1750)
        p.observe(other, -110.0, 1.0, 30.0, false, 2000); p.reconcile(2000)
        assertNotEquals(p.timer(id, 2250)?.text, p.timer(other, 2250)?.text)
        assertEquals(0xFF55FF55.toInt(), p.timer(id, 250)?.color)
        assertEquals(0xFFFFFF55.toInt(), p.timer(id, 4250)?.color)
        assertEquals(0xFFFF5555.toInt(), p.timer(id, 7250)?.color)
        assertEquals(-1, p.timer(id, 15250)?.color)
    }
    @Test fun `west and north segments also remain one by seven`() {
        val p = ShywormProjection()
        for ((at, point) in listOf(-100.0 to 20.0, -99.5 to 20.0, -93.0 to 20.0,
            -93.0 to 20.5, -93.0 to 27.0, -93.5 to 27.0).withIndex())
            p.observe(id, point.first, 50.0, point.second, true, at * 250L)
        val west = p.path(id, 1250)!!
        assertEquals(-100.5, west.minX, .001); assertEquals(1.0, west.maxX - west.minX, .001)
        p.observe(id, -100.0, 50.0, 27.0, true, 1500)
        p.observe(id, -100.0, 50.0, 26.5, true, 1750)
        val north = p.path(id, 1750)!!
        assertEquals(19.5, north.minZ, .001); assertEquals(1.0, north.maxZ - north.minZ, .001)
    }
    @Test fun `teleports and stale movement remove the inferred path`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.observe(id, -99.5, 50.0, 20.0, true, 250)
        assertNull(p.path(id, 4000))
        p.observe(id, -120.0, 50.0, 40.0, true, 500)
        assertNull(p.path(id, 500))
        p.observe(id, Double.NaN, 1.0, 20.0, false, 750)
        assertNull(p.hiddenHeight(id))
    }
    @Test fun `underground Shyworm ESP survives label loss but sparkling eligibility and UUID expiry still win`() {
        assertTrue(SafariEspRules.renderCurrent("Shyworm", true, true, true, false))
        assertFalse(SafariEspRules.renderCurrent("Shyworm", true, false, true, false))
        val party = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - "Shyworm"))
        }
        val run = SafariRun(0)
        assertTrue(SafariEspRules.neededForSparkling("Shyworm", false, party, false, run, id, 0))
        assertFalse(SafariEspRules.neededForSparkling("Shyworm", false, party, false, run, id, 10000))
        party.accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
        assertFalse(SafariEspRules.neededForSparkling("Shyworm", false, party, false, run, UUID.randomUUID(), 10001))
    }
    @Test fun `clockwise turns warn the next side instead of the active side`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.observe(id, -99.5, 50.0, 20.0, true, 250)
        val east = p.path(id, 250)!!
        assertEquals(1.0, east.maxX - east.minX, .001)
        assertEquals(7.0, east.maxZ - east.minZ, .001)
        assertEquals(-93.5, east.minX, .001)
        p.observe(id, -93.0, 50.0, 20.0, true, 500)
        p.observe(id, -93.0, 50.0, 20.5, true, 750)
        val south = p.path(id, 750)!!
        assertEquals(7.0, south.maxX - south.minX, .001)
        assertEquals(1.0, south.maxZ - south.minZ, .001)
        assertEquals(26.5, south.minZ, .001)
    }
    @Test fun `short underground corner pauses do not start a rest timer`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.observe(id, -99.5, 50.0, 20.0, true, 250)
        p.observe(id, -99.5, 1.0, 20.0, false, 500)
        assertEquals(50.0, p.hiddenHeight(id))
        assertNull(p.timer(id, 1000))
        assertNotNull(p.path(id, 1000))
    }
    @Test fun `rest chat associates a single newly hidden worm and preserves its eight second deadline`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.chat("The Shyworm hid back into the ground.", 250)
        p.observe(id, -100.0, 1.0, 20.0, false, 500)
        p.reconcile(500)
        assertEquals("8.0s", p.timer(id, 250)?.text)
        assertNull(p.path(id, 500))
        p.observe(id, -100.0, 1.0, 20.0, false, 1500)
        p.reconcile(1500)
        assertEquals("7.0s", p.timer(id, 1250)?.text)
        assertEquals("moving soon...", p.timer(id, 8250)?.text)
        assertEquals("moving soon...", p.timer(id, 15250)?.text)
        p.observe(id, -100.0, 50.0, 20.0, true, 16000)
        assertNull(p.timer(id, 16000))
    }
    @Test fun `flee messages work but quoted player messages never start timers`() {
        for (message in listOf("The Shyworm saw a player and fled!", "The Shyworm fled because it was startled!")) {
            val p = ShywormProjection()
            p.observe(id, -100.0, 50.0, 20.0, true, 0)
            p.chat("Party > Friend: $message", 100)
            p.observe(id, -100.0, 1.0, 20.0, false, 250)
            p.reconcile(250)
            assertNull(p.timer(id, 250))
            p.chat(message, 300)
            p.reconcile(500)
            assertNotNull(p.timer(id, 500))
        }
    }
    @Test fun `ambiguous worms and stale messages do not share an invented timer`() {
        val p = ShywormProjection()
        val other = UUID.randomUUID()
        for (worm in listOf(id, other)) p.observe(worm, -100.0, 50.0, 20.0, true, 0)
        p.chat("The Shyworm hid back into the ground.", 250)
        for (worm in listOf(id, other)) p.observe(worm, -100.0, 1.0, 20.0, false, 500)
        p.reconcile(500)
        assertNull(p.timer(id, 500)); assertNull(p.timer(other, 500))
        p.reconcile(3000)
        p.retain(setOf(id))
        p.reconcile(3250)
        assertNull(p.timer(id, 3250))
    }
    @Test fun `unloading and run reset remove projection and timer state`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.chat("The Shyworm hid back into the ground.", 250)
        p.observe(id, -100.0, 1.0, 20.0, false, 500); p.reconcile(500)
        assertNotNull(p.timer(id, 500))
        p.retain(emptySet())
        assertNull(p.hiddenHeight(id)); assertNull(p.timer(id, 750))
        p.reset()
        p.observe(id, -100.0, 1.0, 20.0, false, 1000); p.reconcile(1000)
        assertNull(p.hiddenHeight(id)); assertNull(p.timer(id, 1000)); assertNull(p.path(id, 1000))
    }
    @Test fun `next clockwise side is warned before corner emergence`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.observe(id, -99.5, 50.0, 20.0, true, 250)
        p.observe(id, -93.0, 1.0, 20.0, false, 500)
        val next = p.path(id, 500)!!
        assertEquals(-93.5, next.minX, .001)
        assertEquals(-92.5, next.maxX, .001)
        assertEquals(20.0, next.minZ, .001)
        assertEquals(27.0, next.maxZ, .001)
        assertNull(p.timer(id, 500))
    }
    @Test fun `head bobbing follows live height instead of projecting to old surface`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.observe(id, -99.5, 49.0, 20.0, true, 250)
        assertNull(p.hiddenHeight(id))
        p.observe(id, -99.0, 48.0, 20.0, false, 500)
        assertNull(p.hiddenHeight(id))
        p.observe(id, -98.5, 1.0, 20.0, false, 750)
        assertEquals(49.0, p.hiddenHeight(id))
    }
    @Test fun `queued rest selects nearest stationary underground worm without fresh label requirement`() {
        val p = ShywormProjection(); val farther = UUID.randomUUID(); val moving = UUID.randomUUID()
        p.observe(id, 10.0, 50.0, 0.0, true, 0)
        p.observe(farther, 20.0, 50.0, 0.0, true, 0)
        p.observe(moving, 1.0, 50.0, 0.0, true, 0)
        p.observe(id, 10.0, 1.0, 0.0, false, 500)
        p.observe(farther, 20.0, 1.0, 0.0, false, 500)
        p.observe(moving, 2.0, 1.0, 0.0, false, 5000)
        p.chat("The Shyworm hid back into the ground.", 5000, 0.0, 0.0)
        p.reconcile(5000)
        assertEquals("8.0s", p.timer(id, 5000)?.text)
        assertNull(p.timer(farther, 5000)); assertNull(p.timer(moving, 5000))
        p.chat("The Shyworm saw a player and fled!", 5100, 19.0, 0.0)
        p.reconcile(5100)
        assertEquals("8.0s", p.timer(farther, 5100)?.text)
    }
    @Test fun `warning always shows future clockwise side and keeps ground height while head bobs`() {
        val p = ShywormProjection()
        p.observe(id, -100.0, 50.0, 20.0, true, 0)
        p.observe(id, -99.5, 49.0, 20.0, true, 250)
        val next = p.path(id, 250)!!
        assertEquals(-93.5, next.minX, .001); assertEquals(20.0, next.minZ, .001)
        assertEquals(1.0, next.maxX - next.minX, .001); assertEquals(7.0, next.maxZ - next.minZ, .001)
        p.observe(id, -99.0, 48.0, 20.0, true, 500)
        assertEquals(next.height, p.path(id, 500)!!.height)
    }
}
