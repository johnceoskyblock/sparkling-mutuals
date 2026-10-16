package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.safari.SafariIsland
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariManagerGuardTest {
    @Test fun `only server entry nametags in the Safari entry menu are guarded`() {
        assertTrue(SafariManagerGuard.entryItem("Critter Safari Entry", true, false))
        assertTrue(SafariManagerGuard.entryItem("§aCritter Safari Entry", true, false))
        assertFalse(SafariManagerGuard.entryItem("Critter Safari Entry", true, true))
        assertFalse(SafariManagerGuard.entryItem("Critter Safari Entry", false, false))
        assertFalse(SafariManagerGuard.entryItem("Storage", true, false))
    }
    @Test fun `party kick substring matches only the body`() {
        assertTrue(SafariManagerGuard.callsKick("Party > Farmer: KICK"))
        assertTrue(SafariManagerGuard.callsKick("Party > [VIP] Farmer: can someone kick Steve?"))
        assertTrue(SafariManagerGuard.callsKick("Party > Farmer: kickstart the run"))
        assertTrue(SafariManagerGuard.callsKick("Party > Farmer: I was kicked!"))
        assertFalse(SafariManagerGuard.callsKick("Party > KickPlayer: ready"))
        assertFalse(SafariManagerGuard.callsKick("Guild > Farmer: kick him"))
        assertFalse(SafariManagerGuard.callsKick("Farmer has been kicked from the party"))
    }
    @Test fun `only the Safari island counts, not Torrhus Canyon's Critter Safari zone`() {
        assertTrue(SafariIsland.isSafari("safari"))
        assertFalse(SafariIsland.isSafari("foraging_3"))
        assertFalse(SafariIsland.isSafari(null))
    }
}
