package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.alerts.AlertManager
import net.johnceo.sparklingmutuals.alerts.WarpAlertTimer
import net.johnceo.sparklingmutuals.contest.ContestTicketState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContestTicketAndAutoWarpTest {
    @Test fun `a later active run in the same world cannot inherit an earlier warp`() {
        val timer = WarpAlertTimer()
        val firstRun = net.johnceo.sparklingmutuals.safari.SafariRun(0)
        val nextRun = net.johnceo.sparklingmutuals.safari.SafariRun(1000)
        val first = timer.arm(firstRun)
        assertFalse(timer.consume(first, true, nextRun))
        assertFalse(timer.consume(first, true, firstRun))
        assertTrue(timer.consume(timer.arm(nextRun), true, nextRun))
    }
    @Test fun `uncommon or higher earns one announcement per player per contest`() {
        val tickets = ContestTicketState()
        assertFalse(tickets.claim("Me", 10, "Common", 30))
        assertFalse(tickets.claim("Me", 10, "Uncommon", 9))
        assertTrue(tickets.claim("Me", 10, "Uncommon", 10))
        assertFalse(tickets.claim("me", 10, "Uncommon", 11))
        assertFalse(tickets.claim("Me", 10, "Legendary", 40))
        assertTrue(tickets.claim("Me", 11, "Rare", 10))
        assertTrue(tickets.claim("Other", 11, "Uncommon", 10))
        assertFalse(tickets.claim("", 12, "Uncommon", 10))
    }
    @Test fun `warp timer expires once and a newer hotspot invalidates the older callback`() {
        val timer = WarpAlertTimer()
        val first = timer.arm(); val second = timer.arm()
        assertFalse(timer.consume(first, true))
        assertTrue(timer.consume(second, true))
        assertFalse(timer.consume(second, true))
    }
    @Test fun `cancellation and a stale connection world or ended run never cause a delayed warp`() {
        val timer = WarpAlertTimer()
        val first = timer.arm(); timer.cancel()
        assertFalse(timer.consume(first, true))
        val second = timer.arm()
        assertFalse(timer.consume(second, false))
        assertFalse(timer.consume(second, true))
        assertTrue(timer.consume(timer.arm(), true))
    }
    @Test fun `only a real hotspot line schedules an automatic warp`() {
        val hotspot = "HOTSPOT! Your Hunting Hotspot is the Haunted Biome!"
        assertTrue(AlertManager.isTrigger("§a$hotspot"))
        for (prefix in listOf("Party > Player: ", "Guild > Player: ", "From Player: ", "[SM] "))
            assertFalse(AlertManager.isTrigger(prefix + hotspot))
        assertFalse(AlertManager.isTrigger("HOTSPOT! Your Hunting Hotspot is mentioned here"))
    }
}
