package net.johnceo.sparklingmutuals.safari

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.ceil
import kotlin.math.exp

class DoomspiralCabinetTest {
    @Test fun `positive Sparkling evidence stays visually hidden until the server reveal`() {
        for (remaining in listOf<Int?>(null, 98, 26)) {
            val frame = DoomspiralCabinet.frame(3f, 1f, remaining, 72.99f, 7)
            assertFalse(frame.revealed)
            assertFalse(frame.jackpot)
        }
        val frame = DoomspiralCabinet.frame(3f, 1f, 25, 73f, 7)
        assertTrue(frame.revealed)
        assertTrue(frame.jackpot)
        assertEquals(0f, frame.revealProgress)
        val lastFrame = DoomspiralCabinet.frame(8f, 1f, 1, 97.99f, 7)
        assertTrue(lastFrame.jackpot)
        assertTrue(lastFrame.revealProgress > .99f)
    }

    @Test fun `all seven ordinary layouts reveal a regular result with 25 ticks to go`() {
        for (mask in 0..6) {
            val frame = DoomspiralCabinet.frame(8f, 1f, 25, 73f, mask)
            assertTrue(frame.revealed)
            assertFalse(frame.jackpot)
        }
    }

    @Test fun `server progress preserves original free spin and quadratic landing for every result`() {
        for (mask in 0..7) for (index in 0..2) {
            val free = DoomspiralCabinet.frame(0f, null, null, 0f, mask)
            assertEquals((index * 2).toFloat(), free.offset(index), .00001f)
            val initial = 7f + 14f * (1f - exp(-1f / 1.8f)) + index * 2
            val end = ceil((initial + 4) / 6) * 6 + if (mask and (1 shl index) != 0) 0 else 1
            assertEquals(initial, DoomspiralCabinet.frame(1f, 1f, 98, 0f, mask).offset(index), .00001f)
            assertEquals(initial + (end - initial) * .75f,
                DoomspiralCabinet.frame(20f, 1f, 62, 36.5f, mask).offset(index), .00001f)
            for ((remaining, tick) in listOf(25 to 73f, 24 to 74f, 1 to 97.99f))
                assertEquals(end, DoomspiralCabinet.frame(100f, 1f, remaining, tick, mask).offset(index), .00001f)
        }
    }

    @Test fun `wall time under server lag cannot advance the landing`() {
        val early = DoomspiralCabinet.frame(2f, 1f, 70, 28f, 7)
        val lagged = DoomspiralCabinet.frame(100f, 1f, 70, 28f, 7)
        for (i in 0..2) assertEquals(early.offset(i), lagged.offset(i))
        assertFalse(lagged.jackpot)
        assertFalse(lagged.revealed)
    }

    @Test fun `cabinet lever and celebration fit the viewport at every GUI scale`() {
        for ((w, h) in listOf(320 to 240, 854 to 480, 1920 to 1080, 160 to 90)) {
            val scale = DoomspiralCabinet.scale(w, h)
            assertTrue(scale > 0f && scale <= 1.6f)
            assertTrue(352 * scale <= w)
            assertTrue(300 * scale <= h)
        }
        assertEquals(0f, DoomspiralCabinet.scale(0, 240))
        assertEquals(0f, DoomspiralCabinet.scale(320, -1))
    }
}
