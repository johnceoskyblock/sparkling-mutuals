package net.johnceo.sparklingmutuals.contest

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContestPartyTicketsTest {
    @Test fun `ready needs the local ticket and a report from each teammate in this contest`() {
        val party = ContestPartyTickets()
        val teammates = setOf("alex", "jo")
        party.update("me", 10, teammates)
        party.confirm("alex", 10, teammates)
        party.confirm("jo", 9, teammates)
        party.confirm("stranger", 10, teammates)
        assertFalse(party.ready(10, teammates, true))

        party.confirm("jo", 10, teammates)
        assertFalse(party.ready(10, teammates, false))
        assertTrue(party.ready(10, teammates, true))
        assertTrue(party.shouldAnnounce())
        party.markShown()
        party.update("me", 10, teammates)
        assertFalse(party.shouldAnnounce())

        party.update("me", 11, teammates)
        assertFalse(party.ready(11, teammates, true))
        assertTrue(party.shouldAnnounce())
    }

    @Test fun `departed teammates lose their confirmations and solo does not count`() {
        val party = ContestPartyTickets()
        val teammates = setOf("alex", "jo")
        party.update("me", 10, teammates)
        party.confirm("alex", 10, teammates)
        party.confirm("jo", 10, teammates)
        party.update("me", 10, setOf("alex"))
        party.update("me", 10, teammates)
        assertFalse(party.ready(10, teammates, true))
        assertFalse(party.ready(10, emptySet(), true))

        party.update("different-account", 10, teammates)
        assertFalse(party.ready(10, setOf("alex"), true))
    }

    @Test fun `ticket reports accept earned messages and reject guesses reminders and generic readiness`() {
        for (body in listOf("Ticket earned!", "First-Class Ticket earned!", "ticket", "I got the basic ticket!"))
            assertTrue(ContestPartyTickets.reportsTicket(body), body)
        for (body in listOf("Do you have a ticket?", "I need a ticket", "[Reminder] Everyone get your ticket!", "!ticket", "r"))
            assertFalse(ContestPartyTickets.reportsTicket(body), body)
    }
}
