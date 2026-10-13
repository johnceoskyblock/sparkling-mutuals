package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.LitterbugProjection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class LitterbugTimerTest {
    private val id = UUID.randomUUID()
    private fun label(projection: LitterbugProjection, now: Long) = projection.hiddenMarker(id, 13.4, 1.0, -66.8, now)
    private fun marker(projection: LitterbugProjection, now: Long) = label(projection, now).also {
        assertNotNull(it, "Observed hidden Litterbug should have a countdown marker")
    }!!
    @Test fun `wall entry starts an eight second countdown at the projected box`() {
        val projection = LitterbugProjection()
        projection.observe(id, 73.0, 0); projection.observe(id, 1.0, 1000)
        val marker = marker(projection, 1000)
        assertEquals(73.0, marker.height); assertEquals("8.0s", marker.text)
        assertEquals(0xFF55FF55.toInt(), marker.color)
    }
    @Test fun `repeated hidden observations do not restart the countdown`() {
        val projection = LitterbugProjection(); projection.observe(id, 1.0, 1000)
        projection.observe(id, 1.0, 3500)
        assertEquals("5.5s", marker(projection, 3500).text)
    }
    @Test fun `timer colors progress through green yellow red then white moving soon`() {
        val projection = LitterbugProjection(); projection.observe(id, 1.0, 1000)
        for ((now, text, color) in listOf(
            Triple(3999L, "5.1s", 0xFF55FF55.toInt()),
            Triple(4000L, "5.0s", 0xFFFFFF55.toInt()),
            Triple(7000L, "2.0s", 0xFFFF5555.toInt()),
            Triple(8951L, "0.1s", 0xFFFF5555.toInt()),
            Triple(9000L, "Moving soon..", -1),
            Triple(15000L, "Moving soon..", -1))) {
            val marker = marker(projection, now)
            assertEquals(text, marker.text); assertEquals(color, marker.color)
        }
    }
    @Test fun `each bug keeps an independent timer including repeated unload and reload observations`() {
        val projection = LitterbugProjection(); val second = UUID.randomUUID()
        projection.observe(id, 1.0, 1000); projection.observe(second, 1.0, 4000)
        projection.observe(id, 1.0, 5000) // Same hidden UUID observed again after being out of render.
        assertEquals("4.0s", marker(projection, 5000).text)
        val other = projection.hiddenMarker(second, 13.4, 1.0, -66.8, 5000)
        assertNotNull(other); assertEquals("7.0s", other!!.text)
    }
    @Test fun `emergence removes the timer and the next hiding cycle restarts it`() {
        val projection = LitterbugProjection(); projection.observe(id, 1.0, 1000)
        projection.observe(id, 85.0, 10000)
        assertNull(projection.hiddenMarker(id, 13.4, 85.0, -66.8, 10000))
        assertNull(label(projection, 10000)) // No new observed wall entry yet.
        projection.observe(id, 1.0, 11000)
        val marker = marker(projection, 11000)
        assertEquals("8.0s", marker.text); assertEquals(85.0, marker.height)
    }
    @Test fun `unknown or ambiguous heights do not receive floating placeholder timers`() {
        val projection = LitterbugProjection(); projection.observe(id, 1.0, 1000)
        assertNull(projection.hiddenMarker(id, 30.0, 1.0, -75.0, 1000))
        assertNull(projection.hiddenMarker(id, 7.5, 1.0, -66.5, 1000))
        assertNull(projection.hiddenMarker(id, 13.4, Double.NaN, -66.8, 1000))
    }
    @Test fun `run reset forgets the old countdown and observed emergence height`() {
        val projection = LitterbugProjection(); projection.observe(id, 85.0, 0); projection.observe(id, 1.0, 1000)
        projection.reset(); assertNull(label(projection, 2000))
        projection.observe(id, 1.0, 2000)
        val marker = marker(projection, 2000)
        assertEquals("8.0s", marker.text); assertEquals(73.0, marker.height)
    }
    @Test fun `clock correction cannot display more than the initial countdown`() {
        val projection = LitterbugProjection(); projection.observe(id, 1.0, 1000)
        assertEquals("8.0s", marker(projection, 0).text)
    }
}
