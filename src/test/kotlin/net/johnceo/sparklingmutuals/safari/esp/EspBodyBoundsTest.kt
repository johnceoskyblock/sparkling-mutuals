package net.johnceo.sparklingmutuals.safari.esp
import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class EspBodyBoundsTest {
    @Test fun `body offsets preserve existing display and armor stand placement without padding`() {
        val p=ShotVector(1.0,2.0,3.0)
        assertEquals(.5,EspBodyBounds.visual(p,"display",true)!!.min.x)
        assertEquals(3.0,EspBodyBounds.visual(p,"display",true)!!.max.y)
        assertEquals(3.35,EspBodyBounds.visual(p,"armor_stand",false)!!.min.y)
        assertNull(EspBodyBounds.visual(p,"native",false))
    }
}

