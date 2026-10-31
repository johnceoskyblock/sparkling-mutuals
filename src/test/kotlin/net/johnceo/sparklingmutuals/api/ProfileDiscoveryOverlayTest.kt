package net.johnceo.sparklingmutuals.api

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProfileDiscoveryOverlayTest {
    @Test fun `observed catches survive old and refreshed profiles without mutating cache`() {
        val body=JsonParser.parseString("""{"profiles":[{"selected":true,"profile_id":"p","members":{"abc":{"safari":{"discovered_sparkling_critters":["GAZER"],"total_captured_sparkling_critters":5}}}}]}""").asJsonObject
        val overlay=ProfileDiscoveryOverlay<String>()
        overlay.caught("abc","Mantis Shrimp",5)
        overlay.caught("abc","Mantis Shrimp",5)
        val member=CloudProfiles.member(overlay.apply("abc","abc",body),"abc","p")!!
        assertEquals(setOf("GAZER","MANTIS_SHRIMP"),SafariProfile.discoveriesFrom(member))
        assertEquals(7L,SafariProfile.from(member).totalCaptured)
        assertEquals(5L,SafariProfile.from(CloudProfiles.member(body,"abc","p")!!).totalCaptured)
        assertSame(body,overlay.apply("absent","abc",body))
        overlay.clear();assertSame(body,overlay.apply("abc","abc",body))
    }
}
