package net.johnceo.sparklingmutuals.safari

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariEntryCooldownTest {
    @Test fun `server denial starts thirty seconds and expires exactly`() {
        val timer=SafariEntryCountdown()
        assertTrue(timer.trigger("§cYou cannot join this instance!",0,true))
        assertEquals(30,timer.remaining(0))
        assertEquals(30,timer.remaining(999))
        assertEquals(29,timer.remaining(1000))
        assertEquals(1,timer.remaining(29999))
        assertNull(timer.remaining(30000))
    }
    @Test fun `repeat denials do not extend a running timer and expiry allows rearming`() {
        val timer=SafariEntryCountdown()
        timer.trigger("You cannot join this instance!",0,true)
        assertFalse(timer.trigger("You cannot join this instance!",10000,true))
        assertEquals(20,timer.remaining(10000))
        assertTrue(timer.trigger("You cannot join this instance!",30000,true))
        assertEquals(30,timer.remaining(30000))
    }
    @Test fun `unrelated messages player chat overlays and non safari context do not arm`() {
        for(raw in listOf("You cannot join this instance", "Other error", "Party > JohnCEO: You cannot join this instance!", "[MVP+] JohnCEO: You cannot join this instance!"))
            assertFalse(SafariEntryCountdown().trigger(raw,0,true),raw)
        assertFalse(SafariEntryCountdown().trigger("You cannot join this instance!",0,false))
        assertFalse(SafariEntryCountdown().trigger("You cannot join this instance!",0,true,overlay=true))
    }
    @Test fun `clear cancels and permits a fresh timer`() {
        val timer=SafariEntryCountdown()
        timer.trigger("You cannot join this instance!",0,true)
        timer.clear()
        assertNull(timer.remaining(1))
        assertTrue(timer.trigger("You cannot join this instance!",1,true))
    }
}
