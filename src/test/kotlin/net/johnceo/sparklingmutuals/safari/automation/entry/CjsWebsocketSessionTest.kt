package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.sync.PartyRoomDiscoveries
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjsWebsocketSessionTest {
    private val c=EntryFixtures.context()
    private val target=UUID(0,99)
    private fun frame(room: PartyRoomDiscoveries, epoch: Long=7)=CjsEntryFrame(c,
        room.snapshot(epoch)?.let { CjsRoomSignal(c,it.generation,it.active) },1,true,0,true,true,false,
        true,true,false,false,target,true,true,CjsApproachOutcome.COMPLETE,null)
    @Test fun `socket capture between menu proposal and dispatch prevents ticket and keeps ownership`() {
        val room=PartyRoomDiscoveries();room.welcome(7,true);room.receive(7,"536870912,0,0,0")
        val flow=CjsEntryFlow();val inputs=mutableListOf<String>()
        val manager=flow.tick(frame(room),6000)!!
        assertTrue(CjsEntryDispatch.send(flow,manager,{frame(room)},6000,{it()},{inputs.add("manager")},{_,_->error("ticket too soon")}))
        val menu=CjsTicketMenu(Any(),2,true,true,11,SafariTicketType.entries.first())
        val ticket=flow.tick(frame(room).copy(menu=menu),6100)!!
        room.receive(7,"536870912,0,536870912,0")
        assertFalse(CjsEntryDispatch.send(flow,ticket,{frame(room).copy(menu=menu)},6101,{it()},{error("manager twice")},{_,_->inputs.add("ticket")}))
        assertEquals(listOf("manager"),inputs);assertTrue(flow.suppressesAutomaticMenus)
        flow.tick(frame(room).copy(enabled=false,menu=menu),6200)
        assertTrue(flow.suppressesAutomaticMenus)
    }
    @Test fun `welcome and historical local reports are insufficient without current atomic data`() {
        val room=PartyRoomDiscoveries();room.welcome(7,true)
        val flow=CjsEntryFlow()
        assertNull(flow.tick(frame(room),6000))
        // Neither a normal legacy detector/report nor a welcome appears in the positive input API.
        room.receive(7,"536870912,0,0,0")
        assertNotNull(flow.tick(frame(room),6100))
        room.welcome(8,true)
        assertNull(flow.tick(frame(room,8),6200))
        room.receive(8,"536870912,0,0,0")
        assertNull(flow.tick(frame(room,8),6300))
        assertTrue(flow.suppressesAutomaticMenus)
    }
}
