package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import net.johnceo.sparklingmutuals.safari.automation.SafariTicketSelect
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjsEntryInputTest {
    @Test fun `Basic first entry can dispatch while higher ticket lore is still loading`() {
        val input = CjsEntryInput(CjsApproachInput())
        val menu = Any()
        val basic = SafariTicketSelect.Option(10, SafariTicketType.BASIC, true)
        val economy = SafariTicketSelect.Option(12, SafariTicketType.ECONOMY, true)
        val premium = SafariTicketSelect.Option(14, SafariTicketType.PREMIUM, null)
        val selected = input.ticketMenu(menu, 2, listOf(basic, economy, premium))
        assertTrue(selected.ready)
        assertEquals(10, selected.slot)
        assertEquals(SafariTicketType.BASIC, selected.tier)
        assertFalse(input.ticketMenu(menu, 2, listOf(basic.copy(available = null), economy)).ready)
        val fallback = input.ticketMenu(menu, 2, listOf(basic.copy(available = false), economy, premium))
        assertTrue(fallback.ready)
        assertEquals(SafariTicketType.ECONOMY, fallback.tier)
    }
    @Test fun `manager ray requires real hitbox reach aim and clear line of sight`() {
        val box=AABB(-.3,0.0,2.5,.3,1.8,3.1)
        val eye=Vec3(0.0,1.6,0.0);val forward=Vec3(0.0,0.0,1.0)
        assertNotNull(CjsManagerGeometry.hit(eye,forward,3.0,box,null))
        assertNull(CjsManagerGeometry.hit(eye,forward,2.0,box,null))
        assertNull(CjsManagerGeometry.hit(eye,Vec3(1.0,0.0,0.0),3.0,box,null))
        assertNull(CjsManagerGeometry.hit(eye,forward,3.0,box,Vec3(0.0,1.6,1.5)))
        assertNull(CjsManagerGeometry.hit(eye,forward,Double.NaN,box,null))
    }
    @Test fun `real policy dispatch consumes once and refuses changed authority before sending`() {
        val c=EntryFixtures.context();val target=UUID(0,99);val flow=CjsEntryFlow()
        val f=CjsEntryFrame(c,CjsRoomSignal(c,7,setOf("Bluebird")),1,true,0,true,true,false,
            true,true,false,false,target,true,true,CjsApproachOutcome.COMPLETE,null)
        val effect=flow.tick(f,6000) as CjsEntryEffect.Manager
        val packets=mutableListOf<String>()
        assertFalse(CjsEntryDispatch.send(flow,effect,{f},6100,{false},{packets.add("manager $it")},{_,_->error("ticket")}))
        assertTrue(packets.isEmpty());assertTrue(flow.suppressesAutomaticMenus)
        val other=CjsEntryFlow();val e=other.tick(f,6000) as CjsEntryEffect.Manager
        assertTrue(CjsEntryDispatch.send(other,e,{f},6100,{it()},{packets.add("manager $it")},{_,_->error("ticket")}))
        assertFalse(CjsEntryDispatch.send(other,e,{f},6101,{it()},{packets.add("manager $it")},{_,_->error("ticket")}))
        assertEquals(listOf("manager $target"),packets)
    }
}
