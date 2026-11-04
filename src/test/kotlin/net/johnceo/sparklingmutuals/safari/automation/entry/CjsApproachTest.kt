package net.johnceo.sparklingmutuals.safari.automation.entry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class CjsApproachTest {
    @Test fun `room discovery starts after five seconds and changed socket cancels`() {
        val a=CjsApproach()
        assertEquals(CjsApproachOutcome.MOVING,a.tick(frame(),6000).outcome)
        assertTrue(a.tick(frame(),6050).forward)
        assertEquals(CjsApproachOutcome.CANCELLED,a.tick(frame().copy(signal=sight.copy(generation=8)),6100).outcome)
        assertFalse(a.tick(frame(),6150).forward)
    }
    @Test fun `prime native crouch before allowing the first forward tick`() {
        val a=CjsApproach()
        val first=a.tick(frame().copy(crouched=false),0)
        assertTrue(first.crouch);assertFalse(first.forward)
        assertFalse(a.tick(frame().copy(crouched=false),50).forward)
        assertTrue(a.tick(frame().copy(crouched=true),100).forward)
    }
    @Test fun `native speed point two and ordinary friction coast within one block`() {
        val a=CjsApproach();var position=0.0;var velocity=0.0;var previousSneak=false;var crouched=false
        repeat(60) { tick ->
            val action=a.tick(frame(z=position).copy(crouched=crouched,velocity=velocity,acceleration=.05880001,drag=.5460001),tick*50L)
            crouched=previousSneak
            previousSneak=action.crouch || action.forward
            val acceleration=if(action.forward) { if(crouched) .0588 else .196 } else 0.0
            val step=velocity+acceleration
            position+=step;velocity=step*.546
        }
        assertTrue(position<=1.0,"Native walk and coast travelled $position")
        assertTrue(position>.5,"Test must exercise a real approach, not permanently refuse")
    }
    @Test fun `sideways and backwards corrections cancel rather than count as progress`() {
        for (drift in listOf(frame(x=.1),frame(z=-.1))) {
            val a=CjsApproach();a.tick(frame(),0);a.tick(frame(),50)
            assertFalse(a.tick(drift,100).forward)
            assertFalse(a.tick(frame(),150).forward)
        }
    }
    private val c = EntryFixtures.context()
    private val target = UUID.fromString("00000000-0000-0000-0000-000000000099")
    private val sight = CjsRoomSignal(c, 7, setOf("Bluebird"))
    private fun frame(x: Double = 0.0, z: Double = 0.0) = CjsApproachFrame(c, true, sight, target,
        x, 68.0, z, 0.0, 3.6, 0f, 0f, true)
    @Test fun `face the Manager before walking rather than following original yaw`() {
        val a = CjsApproach()
        val first = a.tick(frame().copy(managerX = -3.6, managerZ = 0.0), 0)
        assertFalse(first.forward); assertEquals(90f, first.yaw!!, .01f)
        assertTrue(a.tick(frame().copy(managerX = -3.6, managerZ = 0.0, yaw = 90f), 50).forward)
    }
    @Test fun `stop short of one block and never rearm in same arrival`() {
        val a=CjsApproach(); a.tick(frame(),0)
        assertTrue(a.tick(frame(),50).forward)
        assertTrue(a.tick(frame(z=.2),100).forward)
        assertTrue(a.tick(frame(z=.5),150).forward)
        assertTrue(a.tick(frame(z=.7),175).forward)
        assertFalse(a.tick(frame(z=.86),200).forward)
        assertFalse(a.tick(frame(),250).forward)
        assertFalse(a.tick(frame().copy(context=c.copy(party=EntryParty(c.party.leader,c.party.members,3))),300).forward)
        assertFalse(a.tick(frame().copy(enabled=false),350).forward)
        assertFalse(a.tick(frame(),400).forward)
        assertFalse(a.tick(frame().copy(context=c.copy(world=2,arrival=2),signal=sight.copy(context=c.copy(world=2,arrival=2))),450).forward)
    }
    @Test fun `safety interruption or changed target context cancels rather than waiting to resume`() {
        val changes=listOf<(CjsApproachFrame)->CjsApproachFrame>(
            {it.copy(safe=false)}, {it.copy(enabled=false)}, {it.copy(signal=null)},
            {it.copy(target=null)}, {it.copy(target=UUID.randomUUID())}, {it.copy(context=null)},
            {it.copy(context=c.copy(server="miniOther"))}, {it.copy(y=68.3)},
            {it.copy(yaw=45f)}, {it.copy(x=2.0)}, {it.copy(x=Double.NaN)})
        for (change in changes) {
            val a=CjsApproach(); a.tick(frame(),0); assertTrue(a.tick(frame(),50).forward)
            assertFalse(a.tick(change(frame()),100).forward)
            assertFalse(a.tick(frame(),150).forward)
        }
    }
    @Test fun `expiry rollback timeout and stalled movement stop owned input`() {
        for (time in listOf(5000L,1500L,-1L)) {
            val a=CjsApproach(); a.tick(frame(),0); a.tick(frame(),50)
            assertFalse(a.tick(frame(),time).forward)
            assertFalse(a.tick(frame(),time.coerceAtLeast(0)+100).forward)
        }
        val a=CjsApproach(); a.tick(frame(),0); a.tick(frame(),50)
        assertFalse(a.tick(frame(),450).forward)
    }
    @Test fun `unsafe start consumes no movement but does not spend a never started attempt`() {
        val a=CjsApproach()
        assertFalse(a.tick(frame().copy(safe=false),0).forward)
        assertFalse(a.tick(frame(),50).forward) // face first
        assertTrue(a.tick(frame(),100).forward)
    }
}
