package net.johnceo.sparklingmutuals.safari.automation.entry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariEntryControllerTest {
    private val c = EntryFixtures.context()
    private fun controller() = SafariEntryController { EntryFixtures.token }.also { assertTrue(it.beginArrival(c, 1000, true)) }
    private fun frame() = EntryFrame(c, enabled = true, ticketHelperEnabled = true, atEntry = true,
        destinationAssociated = true, partyKnown = true, guardAllows = true, managerLoaded = true, managerReachable = true)
    private fun sighting(expires: Long = 6000) = EntrySighting(c, "Bluebird", EntryFixtures.event, 1000, expires)
    private fun managerIssued(): SafariEntryController = controller().also {
        it.observe(sighting()); assertTrue(it.tick(frame(), 1100).single() is EntryEffect.Manager)
    }
    private fun ticketFrame() = frame().copy(menu = EntryMenu(10, exact = true, ready = true, availableSlot = 12))

    @Test fun `guarded flow proposes each action once and admission needs explicit personal evidence`() {
        val ctl = managerIssued()
        assertTrue(ctl.tick(ticketFrame(), 1200).single() is EntryEffect.Ticket)
        assertTrue(ctl.allowsAutomaticTicket(c, 10, 1200))
        assertTrue(ctl.consumeTicketPermit(ticketFrame(), 1200))
        assertFalse(ctl.consumeTicketPermit(ticketFrame(), 1200))
        assertTrue(ctl.tick(ticketFrame(), 1300).isEmpty())
        assertEquals(EntryPhase.WAITING_ADMISSION, ctl.phase)
        ctl.tick(frame().copy(personalAdmission = true, atEntry = false), 1400)
        assertEquals(EntryPhase.DONE, ctl.phase)
        assertTrue(ctl.tick(frame(), 1500).isEmpty())
    }

    @Test fun `no real evidence means no automatic interaction or permissive defaults`() {
        val variants = listOf(frame().copy(ticketHelperEnabled = false), frame().copy(atEntry = false),
            frame().copy(destinationAssociated = false), frame().copy(partyKnown = false),
            frame().copy(guardAllows = false), frame().copy(managerLoaded = false), frame().copy(managerReachable = false))
        variants.forEach { input ->
            val ctl = controller(); ctl.observe(sighting())
            assertTrue(ctl.tick(input, 1100).none { it is EntryEffect.Manager || it is EntryEffect.Ticket }, input.toString())
        }
        assertTrue(controller().tick(EntryFrame(c), 1100).isEmpty())
    }

    @Test fun `fresh query and reply are proposals with no original announcement prerequisite`() {
        val ctl = controller()
        val query = ctl.tick(frame(), 1100).single() as EntryEffect.Query
        assertEquals(EntryFixtures.token, query.message.nonce)
        assertTrue(ctl.tick(frame(), 1200).isEmpty())
        assertTrue(ctl.receiveReply(c, EntryPeer(EntryFixtures.leader, "Leader"), EntryFixtures.live(), 1300))
        assertTrue(ctl.tick(frame(), 1400).single() is EntryEffect.Manager)
    }

    @Test fun `expired sighting cannot spend a ticket after the menu opens`() {
        val ctl = managerIssued()
        assertTrue(ctl.tick(ticketFrame(), 6000).isEmpty())
        assertTrue(ctl.suppressesAutomaticMenus)
        assertFalse(ctl.allowsAutomaticTicket(c, 10, 6000))
        assertFalse(ctl.consumeTicketPermit(ticketFrame(), 6000))
    }

    @Test fun `cancelled manager attempt suppresses late menu despite rearm toggle or reopen`() {
        val ctl = managerIssued(); ctl.cancel()
        assertFalse(ctl.rearm())
        assertTrue(ctl.suppressesAutomaticMenus)
        assertTrue(ctl.tick(ticketFrame().copy(enabled = false), 1200).isEmpty())
        assertTrue(ctl.tick(ticketFrame(), 1300).isEmpty())
        assertFalse(ctl.beginArrival(c.copy(world = 2), 1400, true), "world replacement alone cannot create a new arrival")
        assertFalse(ctl.allowsAutomaticTicket(c, 10, 1500))
    }

    @Test fun `ticket permit rechecks current guard context and menu immediately before consumption`() {
        listOf(ticketFrame().copy(guardAllows = false), ticketFrame().copy(partyKnown = false),
            ticketFrame().copy(context = c.copy(world = 2)), ticketFrame().copy(menu = EntryMenu(11, true, true, 12)),
            ticketFrame().copy(menu = EntryMenu(10, true, true, 13)),
            ticketFrame().copy(manualCancel = true)).forEach { changed ->
            val ctl = managerIssued(); ctl.tick(ticketFrame(), 1200)
            assertFalse(ctl.consumeTicketPermit(changed, 1300))
            assertFalse(ctl.consumeTicketPermit(ticketFrame(), 1400), "rejected ownership cannot revive")
        }
    }

    @Test fun `uncertain ticket remains spent across rearm and passive late admission is recorded`() {
        val ctl = managerIssued(); ctl.tick(ticketFrame(), 1200)
        assertTrue(ctl.consumeTicketPermit(ticketFrame(), 1200))
        ctl.cancel(); assertFalse(ctl.rearm())
        assertTrue(ctl.tick(ticketFrame(), 1400).isEmpty())
        assertFalse(ctl.consumeTicketPermit(ticketFrame(), 1500))
        ctl.tick(frame().copy(enabled = false, personalAdmission = true), 1600)
        assertEquals(EntryPhase.DONE, ctl.phase)
        assertTrue(ctl.ticketIssued)
    }

    @Test fun `loading waits unavailable menus reveal without selection and replacement cancels`() {
        val ctl = managerIssued()
        assertTrue(ctl.tick(frame().copy(menu = EntryMenu(10, true, false, null)), 1200).isEmpty())
        assertTrue(ctl.tick(frame().copy(menu = EntryMenu(10, true, true, null)), 1300).isEmpty())
        assertTrue(ctl.suppressesAutomaticMenus)
        val replaced = managerIssued()
        replaced.tick(frame().copy(menu = EntryMenu(10, true, false, null)), 1200)
        assertTrue(replaced.tick(ticketFrame().copy(menu = EntryMenu(11, true, true, 12)), 1300).isEmpty())
        assertTrue(replaced.suppressesAutomaticMenus)
    }

    @Test fun `context cancellation rollback deadline and capture never replenish actions`() {
        val moved = managerIssued()
        moved.tick(frame().copy(context = null), 1200)
        assertTrue(moved.tick(ticketFrame(), 1300).isEmpty())
        val back = managerIssued(); back.tick(frame(), 1099)
        assertTrue(back.tick(ticketFrame(), 1200).isEmpty())
        val ended = controller(); ended.observe(sighting(Long.MAX_VALUE))
        assertTrue(ended.tick(frame(), 13000).isEmpty())
        val caught = managerIssued(); caught.captured(c, "Bluebird", 1150)
        assertTrue(caught.tick(ticketFrame(), 1200).isEmpty())
        assertTrue(caught.suppressesAutomaticMenus)
    }

    @Test fun `only explicit validated different arrival can reset uncertainty latches`() {
        val ctl = managerIssued(); ctl.cancel()
        val next = c.copy(world = 2, arrival = 2)
        assertFalse(ctl.beginArrival(next, 1500, false))
        assertTrue(ctl.beginArrival(next, 1500, true))
        assertFalse(ctl.suppressesAutomaticMenus)
        ctl.observe(EntrySighting(next, "Bluebird", EntryFixtures.event, 1500, 6500))
        assertTrue(ctl.tick(frame().copy(context = next), 1600).single() is EntryEffect.Manager)
    }

    @Test fun `older same-event evidence cannot crash or extend an active attempt`() {
        val ctl = controller()
        ctl.observe(EntrySighting(c, "Bluebird", EntryFixtures.event, 2000, 6000))
        assertDoesNotThrow { ctl.observe(EntrySighting(c, "Bluebird", EntryFixtures.event, 1000, 1500)) }
        assertTrue(ctl.tick(frame(), 2100).none { it is EntryEffect.Manager || it is EntryEffect.Ticket })
    }

    @Test fun `verification lapse retires an outstanding query but initially loading context may settle`() {
        listOf(frame().copy(partyKnown = false), frame().copy(destinationAssociated = false)).forEach { lost ->
            val ctl = controller()
            assertTrue(ctl.tick(frame(), 1100).single() is EntryEffect.Query)
            ctl.tick(lost, 1200)
            assertFalse(ctl.receiveReply(c, EntryPeer(EntryFixtures.leader, "Leader"), EntryFixtures.live(), 1300))
            assertTrue(ctl.tick(frame(), 1400).isEmpty())
        }
        val settling = controller(); settling.tick(frame().copy(partyKnown = false), 1100)
        assertTrue(settling.tick(frame(), 1200).single() is EntryEffect.Query)
        val held = controller(); held.tick(frame(), 1100)
        held.tick(frame().copy(guardAllows = false), 1200)
        assertTrue(held.receiveReply(c, EntryPeer(EntryFixtures.leader, "Leader"), EntryFixtures.live(), 1300))
        assertTrue(held.tick(frame(), 1400).single() is EntryEffect.Manager)
    }

    @Test fun `captured events and pre-capture replies cannot be restored while a new identity may qualify`() {
        val ctl = controller(); val old = sighting()
        ctl.observe(old); ctl.captured(c, "Bluebird", 1050); ctl.observe(old)
        assertTrue(ctl.tick(frame(), 1100).none { it is EntryEffect.Manager })
        ctl.observe(old.copy(event = "c".repeat(32), receivedAt = 1200, expiresAt = 6200))
        assertTrue(ctl.tick(frame(), 1300).single() is EntryEffect.Manager)
        val waiting = controller(); waiting.tick(frame(), 1100); waiting.captured(c, "Bluebird", 1200)
        assertFalse(waiting.receiveReply(c, EntryPeer(EntryFixtures.leader, "Leader"), EntryFixtures.live(age = 100), 1500))
        assertTrue(waiting.tick(frame(), 1600).isEmpty())
        val delayedLocal = controller(); delayedLocal.captured(c, "Bluebird", 1050); delayedLocal.observe(old)
        assertTrue(delayedLocal.tick(frame(), 1100).none { it is EntryEffect.Manager })
        delayedLocal.observe(old.copy(event = "d".repeat(32), receivedAt = 1200, expiresAt = 6200))
        assertTrue(delayedLocal.tick(frame(), 1300).single() is EntryEffect.Manager)
    }
}
