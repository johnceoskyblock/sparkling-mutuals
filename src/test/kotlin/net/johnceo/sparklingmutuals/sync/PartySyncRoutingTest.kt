package net.johnceo.sparklingmutuals.sync

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncRoutingTest {
    @Test fun `only validated usable room resets continuous failure and backup is visit sticky`() {
        val p=PartySyncRoutePolicy("nonce")
        p.observe(false,0);assertEquals(29999,p.unusableMillis(29999));assertEquals(30000,p.unusableMillis(30000))
        p.observe(true,30001);assertEquals(0,p.unusableMillis(30001));p.observe(false,40000);assertEquals(1000,p.unusableMillis(41000))
        assertTrue(p.accept("generation","backup","nonce",true));assertEquals("backup",p.provider)
        assertFalse(p.accept("generation","primary","nonce",true));assertEquals("backup",p.provider)
        assertFalse(p.accept("other","backup","nonce",true));assertFalse(p.accept("generation","backup","stale",true))
    }
    @Test fun `pending or unrecognized routing never changes provider or enables backup`() {
        val p=PartySyncRoutePolicy("nonce")
        assertFalse(p.accept("old","backup","nonce",false));assertEquals("primary",p.provider);assertTrue(p.pending)
        assertFalse(p.accept("generation","unknown","nonce",true));assertEquals("primary",p.provider)
        assertTrue(p.accept("new","primary","nonce",true));assertFalse(p.pending);assertTrue(p.accept("new","backup","nonce",true))
        assertEquals("primary",PartySyncRoutePolicy("new nonce").provider)
    }
}
