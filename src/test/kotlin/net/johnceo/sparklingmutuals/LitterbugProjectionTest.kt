package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.LitterbugProjection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class LitterbugProjectionTest {
    @Test fun `hidden bugs project to the confirmed room and ledge heights`() {
        val projection = LitterbugProjection()
        for ((x, z, height) in listOf(
            Triple(-2.0591906038116057, -66.27959530190579, 85.0),
            Triple(13.400413507947363, -66.89834596821055, 73.0),
            Triple(15.048465821555784, -79.42474430359262, 69.0),
            Triple(12.595414326675147, -75.97707163337576, 69.0))) {
            assertEquals(height, projection.hiddenHeight(UUID.randomUUID(), x, 1.0, z), "$x,$z")
        }
    }
    @Test fun `ledge uses the confirmed bounds without extending into other paths`() {
        val projection = LitterbugProjection(); val id = UUID.randomUUID()
        for (x in listOf(13.0, 19.99)) for (z in listOf(-66.99, -66.0))
            assertEquals(73.0, projection.hiddenHeight(id, x, 1.0, z))
        assertEquals(69.0, projection.hiddenHeight(id, 12.999, 1.0, -66.5))
        assertEquals(69.0, projection.hiddenHeight(id, 13.4, 1.0, -67.0))
        assertNull(projection.hiddenHeight(id, 20.0, 1.0, -66.5))
        assertNull(projection.hiddenHeight(id, 13.4, 1.0, -65.999))
    }
    @Test fun `unknown spawn learns its own height and surfaced bugs use real positions`() {
        val projection = LitterbugProjection(); val id = UUID.randomUUID()
        assertNull(projection.hiddenHeight(id, 30.0, 1.0, -75.0))
        projection.observe(id, 81.25)
        assertNull(projection.hiddenHeight(id, 30.0, 81.25, -75.0))
        assertEquals(81.25, projection.hiddenHeight(id, 30.0, 1.0, -75.0))
        assertNull(projection.hiddenHeight(UUID.randomUUID(), 30.0, 1.0, -75.0))
    }
    @Test fun `observed height wins over area estimates and resets with the run`() {
        val projection = LitterbugProjection(); val id = UUID.randomUUID()
        projection.observe(id, 85.0)
        projection.observe(id, 1.0)
        assertEquals(85.0, projection.hiddenHeight(id, 13.4, 1.0, -66.8))
        projection.observe(id, 69.0)
        assertEquals(69.0, projection.hiddenHeight(id, 13.4, 1.0, -66.8))
        projection.reset()
        assertEquals(73.0, projection.hiddenHeight(id, 13.4, 1.0, -66.8))
    }
    @Test fun `overlapping rooms wait for an observed emergence height`() {
        val projection = LitterbugProjection(); val id = UUID.randomUUID()
        assertNull(projection.hiddenHeight(id, 7.5, 1.0, -66.5))
        projection.observe(id, 85.0)
        assertEquals(85.0, projection.hiddenHeight(id, 7.5, 1.0, -66.5))
        projection.reset()
        assertNull(projection.hiddenHeight(id, 7.5, 1.0, -66.5))
    }
}
