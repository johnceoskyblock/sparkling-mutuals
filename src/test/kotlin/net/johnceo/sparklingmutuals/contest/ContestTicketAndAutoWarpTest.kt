package net.johnceo.sparklingmutuals.contest

import net.johnceo.sparklingmutuals.alerts.AlertManager
import net.johnceo.sparklingmutuals.alerts.CommandSpacing
import net.johnceo.sparklingmutuals.alerts.SparkleWarpPolicy
import net.johnceo.sparklingmutuals.alerts.WarpAlertTimer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContestTicketAndAutoWarpTest {
    @Test fun `First-Class waits for legendary and switching targets cannot repeat an earned notice`() {
        val tickets = ContestTicketState()
        assertFalse(tickets.claim("Me", 10, "Uncommon", 30, true, 1000))
        assertFalse(tickets.claim("Me", 10, "Epic", 40, true, 4999))
        assertFalse(tickets.claim("Me", 10, "Legendary", 9, true, 5000))
        assertTrue(tickets.claim("Me", 10, "Uncommon", 30))
        assertTrue(tickets.claim("Me", 10, "Legendary", 60, true, 5000))
        assertFalse(tickets.claim("me", 10, "Mythic", 70, true, 6000))
        assertFalse(tickets.claim("Me", 10, "Legendary", 80))
        assertTrue(tickets.claim("Me", 11, "Legendary", 60, true, 5000))
        assertEquals("Ticket earned!", ContestTicketState.message(false))
        assertEquals("First-Class Ticket earned!", ContestTicketState.message(true))
    }
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
        assertFalse(tickets.claim("Me", 10, "Uncommon", 10) { false })
        assertTrue(tickets.claim("Me", 10, "Uncommon", 10) { true })
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
    @Test fun `warp waits for the latest travel with room for the Safari entry deadline`() {
        val timer = WarpAlertTimer()
        val startedAt = 1_000L
        val normalDue = SparkleWarpPolicy.warpDeadline(startedAt, 27)
        val token = timer.arm()
        assertEquals(28_000L, timer.dueAt(normalDue, startedAt))
        timer.traveled(token, 24_000L)
        assertEquals(28_000L, timer.dueAt(normalDue, startedAt))
        timer.traveled(token, 27_000L)
        assertEquals(29_000L, timer.dueAt(normalDue, startedAt))
        timer.traveled(token, 28_000L)
        assertEquals(30_000L, timer.dueAt(normalDue, startedAt))
        timer.traveled(token, 30_000L)
        assertEquals(30_750L, timer.dueAt(normalDue, startedAt))
        assertEquals(31_000L, timer.dueAt(SparkleWarpPolicy.warpDeadline(startedAt, 30), startedAt))
        timer.arm()
        timer.traveled(token, 30_000L)
        assertEquals(28_000L, timer.dueAt(normalDue, startedAt))
    }
    @Test fun `travel delay only recognizes Hypixel server notices`() {
        val notice = "» _cj is traveling to Torrhus Canyon FOLLOW"
        assertEquals("_cj", SparkleWarpPolicy.travelingPlayer(" §9§l» §b_cj §eis traveling to §aTorrhus Canyon §e§lFOLLOW"))
        assertEquals("Stuffy", SparkleWarpPolicy.travelingPlayer("» [MVP++] Stuffy is traveling to Moonglade Marsh FOLLOW"))
        assertNull(SparkleWarpPolicy.travelingPlayer("Party > _cj: $notice"))
        assertNull(SparkleWarpPolicy.travelingPlayer("From [MVP+] _cj: $notice"))
        assertNull(SparkleWarpPolicy.travelingPlayer(notice.removeSuffix(" FOLLOW")))
    }
    @Test fun `warp can only send immediately when the shared command slot is free`() {
        val commands = CommandSpacing<String>()
        commands.add("invite")
        assertFalse(commands.reserveIfEmpty(0L))
        assertEquals("invite", commands.next(0L))
        assertFalse(commands.reserveIfEmpty(499L))
        assertTrue(commands.reserveIfEmpty(500L))
        commands.add("chat")
        assertNull(commands.next(999L))
        assertEquals("chat", commands.next(1_000L))
    }
    @Test fun `warp and reparty use the HUD clock even when the run is discovered late`() {
        val ledger = net.johnceo.sparklingmutuals.safari.SafariLedger()
        ledger.worldChanged(1_000)
        ledger.confirmEntry(10_000)
        val run = ledger.current!!
        val policy = net.johnceo.sparklingmutuals.alerts.SparkleWarpPolicy
        assertNull(run.entry.hotspot)
        assertEquals(24_000L, run.elapsed(25_000))
        assertTrue(policy.returningPhase(run.startedAt, 25_000))
        assertEquals(28_000L, policy.warpDeadline(run.startedAt, policy.DEFAULT_DELAY_SECONDS))
    }
}
