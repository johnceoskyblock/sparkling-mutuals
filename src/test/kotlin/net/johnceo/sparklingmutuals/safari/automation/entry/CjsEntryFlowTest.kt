package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariTicketType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjsEntryFlowTest {
    private val c=EntryFixtures.context()
    private val target=UUID(0,99)
    private val menu=Any()
    private fun frame()=CjsEntryFrame(c,CjsRoomSignal(c,7,setOf("Bluebird")),1,true,0,true,true,false,
        true,true,false,false,target,false,false,CjsApproachOutcome.WAITING,null)
    private fun completed()=frame().copy(approach=CjsApproachOutcome.COMPLETE,settled=true,reachable=true)
    private fun ticket(identity: Any=menu,slot: Int=11)=CjsTicketMenu(identity,2,true,true,slot,SafariTicketType.entries.first())
    private fun waitingMenu(): CjsEntryFlow {
        val flow=CjsEntryFlow()
        assertTrue(flow.tick(frame(),6000) is CjsEntryEffect.Approach)
        assertTrue(flow.tick(completed(),6100) is CjsEntryEffect.Manager)
        assertTrue(flow.consumeManager(completed(),6100))
        assertFalse(flow.consumeManager(completed(),6101))
        return flow
    }
    @Test fun `new verified warp ends old suppression while full entry remains disabled`() {
        val flow=CjsEntryFlow();flow.tick(frame(),6000)
        flow.tick(frame().copy(enabled=false),6100)
        assertTrue(flow.suppressesAutomaticMenus)
        assertNull(flow.tick(frame().copy(enabled=false,context=c.copy(world=2),warpAllowed=false),6200))
        assertTrue(flow.suppressesAutomaticMenus)
        assertNull(flow.tick(frame().copy(enabled=false,warpGeneration=2,arrivedAt=6200),6300))
        assertFalse(flow.suppressesAutomaticMenus)
        assertFalse(flow.consumeManager(completed().copy(enabled=false,warpGeneration=2),6301))
    }
    @Test fun `gameplay press and release between samples latches cancellation`() {
        val flow=CjsEntryFlow();flow.tick(frame(),6000)
        assertTrue(flow.inputEvent(true))
        assertFalse(flow.inputEvent(false))
        assertNull(flow.tick(completed(),6100))
        assertFalse(flow.consumeManager(completed(),6100))
        assertTrue(flow.suppressesAutomaticMenus)
        val idle=CjsEntryFlow();assertFalse(idle.inputEvent(true))
        assertNotNull(idle.tick(frame(),6000))
    }
    @Test fun `complete sequence has one manager one ticket and personal confirmation`() {
        val flow=waitingMenu();val f=completed().copy(menu=ticket())
        assertTrue(flow.tick(f,6200) is CjsEntryEffect.Ticket)
        assertTrue(flow.consumeTicket(f,6200));assertFalse(flow.consumeTicket(f,6201))
        assertNull(flow.tick(f,6300));assertEquals(CjsEntryPhase.WAITING_ADMISSION,flow.phase)
        flow.tick(f.copy(admitted=true),6400)
        assertEquals(CjsEntryPhase.DONE,flow.phase);assertNull(flow.tick(f,6500))
    }
    @Test fun `default and approach only frames cannot spend tickets`() {
        val flow=CjsEntryFlow()
        assertNull(flow.tick(CjsEntryFrame(),0))
        assertNull(flow.tick(frame().copy(enabled=false),6000))
        assertFalse(flow.consumeManager(completed(),6100))
    }
    @Test fun `loss after approach latches until a genuinely new warp`() {
        val changes=listOf<(CjsEntryFrame)->CjsEntryFrame>(
            {it.copy(signal=null)}, {it.copy(signal=CjsRoomSignal(c,8,setOf("Bluebird")))},
            {it.copy(signal=CjsRoomSignal(c,7,emptySet()))}, {it.copy(context=c.copy(world=2))},
            {it.copy(safe=false)}, {it.copy(guardAllows=false)}, {it.copy(manualCancel=true)},
            {it.copy(unexpectedScreen=true)}, {it.copy(enabled=false)}, {it.copy(manager=UUID(0,98))})
        for(change in changes) {
            val flow=CjsEntryFlow();flow.tick(frame(),6000);flow.tick(change(completed()),6100)
            assertTrue(flow.suppressesAutomaticMenus)
            assertNull(flow.tick(frame(),6200));assertFalse(flow.consumeManager(completed(),6200))
            assertTrue(flow.tick(frame().copy(warpGeneration=2,arrivedAt=6200),6300) is CjsEntryEffect.Approach)
        }
    }
    @Test fun `replaced late or closed menu cannot consume reserved ticket`() {
        val flow=waitingMenu();val f=completed().copy(menu=ticket())
        assertTrue(flow.tick(f,6200) is CjsEntryEffect.Ticket)
        assertFalse(flow.consumeTicket(f.copy(menu=ticket(Any())),6201))
        assertTrue(flow.suppressesAutomaticMenus);assertFalse(flow.consumeTicket(f,6202))
        val closed=waitingMenu();closed.tick(f.copy(menu=ticket().copy(ready=false)),6200)
        closed.tick(completed(),6300)
        assertNull(closed.tick(f,6400));assertTrue(closed.suppressesAutomaticMenus)
    }
    @Test fun `slot tier guard and room changes are checked at consumption`() {
        val changes=listOf<(CjsEntryFrame)->CjsEntryFrame>(
            {it.copy(menu=ticket(slot=12))}, {it.copy(menu=ticket().copy(tier=SafariTicketType.entries.last()))},
            {it.copy(menu=ticket().copy(ready=false))}, {it.copy(guardAllows=false)}, {it.copy(signal=null)})
        for(change in changes) {
            val flow=waitingMenu();val f=completed().copy(menu=ticket())
            flow.tick(f,6200);assertFalse(flow.consumeTicket(change(f),6201));assertFalse(flow.consumeTicket(f,6202))
        }
    }
    @Test fun `unknown menu waits no available ticket cancels and deadline never rearms`() {
        val flow=waitingMenu()
        assertNull(flow.tick(completed().copy(menu=ticket().copy(ready=false)),6200))
        assertNull(flow.tick(completed().copy(menu=ticket().copy(slot=null,tier=null)),6300))
        assertEquals(CjsEntryPhase.CANCELLED,flow.phase)
        assertNull(flow.tick(frame(),6400))
        val expired=CjsEntryFlow();expired.tick(frame(),6000);expired.tick(completed(),12000)
        assertEquals(CjsEntryPhase.EXPIRED,expired.phase);assertNull(expired.tick(frame(),12001))
        expired.tick(completed().copy(admitted=true),12002);assertEquals(CjsEntryPhase.DONE,expired.phase)
    }
    @Test fun `must settle and be in real reach after bounded approach`() {
        val flow=CjsEntryFlow();flow.tick(frame(),6000)
        assertNull(flow.tick(completed().copy(settled=false),6100))
        assertTrue(flow.tick(completed(),6200) is CjsEntryEffect.Manager)
        val far=CjsEntryFlow();far.tick(frame(),6000);far.tick(completed().copy(reachable=false),6200)
        assertEquals(CjsEntryPhase.CANCELLED,far.phase)
    }
}
