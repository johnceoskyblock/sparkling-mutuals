package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class ContactTargetProxiesTest {
    private val p=ShotVector(10.0,20.0,30.0)
    private val native=ShotBounds(p,ShotVector(11.0,22.0,31.0))
    @Test fun `identified bodies use unpadded visual placement or native bounds`() {
        val display=ContactTargetProxies.body("display",p,ShotBounds(p,p),true,false)!!
        assertEquals("display_body_v1",display.second)
        assertEquals(19.55,display.first.min.y,1e-10)
        assertEquals(20.15,display.first.max.y,1e-10)
        assertEquals(native,ContactTargetProxies.body("native",p,native,true,false)!!.first)
        assertEquals(21.35,ContactTargetProxies.body("armor_stand",p,native,true,false)!!.first.min.y,1e-10)
    }
    @Test fun `labels players capsules invalid or unidentified models have no guessed proxy`() {
        for(kind in listOf("player","capsule","label")) assertNull(ContactTargetProxies.body(kind,p,native,true,false))
        assertNull(ContactTargetProxies.body("display",p,native,false,false))
        assertNull(ContactTargetProxies.body("native",p,ShotBounds(p,p),true,false))
    }
}
