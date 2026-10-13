package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class SafariEspLifecycleTest {
    @Test fun `rockmite forms migrate old toggle and then persist independently`() {
        SafariEspConfig.load(Properties().apply { setProperty("esp.rockmite.enabled", "false") })
        assertFalse(SafariEspConfig.entityEnabled("Rockmite", true))
        assertFalse(SafariEspConfig.entityEnabled("Rockmite"))
        SafariEspConfig.rockmiteMoundEnabled = true
        val saved = Properties().also(SafariEspConfig::save)
        SafariEspConfig.load(saved)
        assertTrue(SafariEspConfig.entityEnabled("Rockmite", true))
        assertFalse(SafariEspConfig.entityEnabled("Rockmite"))
        SafariEspConfig.load(Properties())
        assertTrue(SafariEspConfig.rockmiteMoundEnabled)
    }
    @Test fun `new palette keeps valid saved colors and white mound independent from silverfish`() {
        SafariEspConfig.load(Properties().apply {
            setProperty("esp.gimmiegold.color", "0:255:1:2:3")
            setProperty("esp.foxtrot.color", "invalid")
        })
        assertEquals(0xFF010203.toInt(), SafariEspConfig.rgb(SafariEspConfig.entityColor("Gimmiegold")))
        assertEquals(0xFFF26F14.toInt(), SafariEspConfig.rgb(SafariEspConfig.entityColor("Foxtrot")))
        assertEquals(0xFFFFFFFF.toInt(), SafariEspConfig.rgb(SafariEspConfig.entityColor("Rockmite", true)))
        assertEquals(0xFF525252.toInt(), SafariEspConfig.rgb(SafariEspConfig.entityColor("Rockmite")))
        SafariEspConfig.load(Properties())
    }
    @Test fun `complete new palette matches requested defaults`() {
        SafariEspConfig.load(Properties())
        val expected = "Foxtrot:F26F14 Bluebird:173AE8 Honeybug:F0BE1E Treefrog:39FF57 Woodchucker:F32F13 Fluffling:66DAFA Hideonfloor:0095FF Parakeet:6BEE3B Macaw:FFD200 Cavernfish:F68929 Flitter:55C0EB Shyworm:79F24C Driftling:FFEC5B Chuckwalla:E3C0A2 Rockmite:525252 Scrappy:EA5577 Snoozle:EB3B3B Gemzie:9132FF Strongarm:E47D15 Tepid:FF0000 Polaris:FF0000 Shuddersquid:FF0000 Billygoat:FF0000 Mantis_Shrimp:FF0000 Nozzlenose:FF0000 Troodon:FF0000 Wumpa:FF0000 Areita:00F7FF Bloodbat:F31C12 Duplico:D7D7D7 Gazer:07CFF3 Litterbug:AB00FF Solsnatcher:FF0000 Gimmiegold:F3C900 Hideonwall:FF00F2 Hideyho:FFFFFF Doomspiral:00F0EF"
        expected.split(' ').forEach {
            val (rawName, hex) = it.split(':')
            val name = rawName.replace('_', ' ')
            assertEquals((0xFF000000L or hex.toLong(16)).toInt(), SafariEspConfig.rgb(SafariEspConfig.entityColor(name)), name)
        }
    }
    @Test fun `hidden and transformed gimmiegold displays no longer identify as critters`() {
        val descriptor = EspEntity("item_display", texture = "8b329e108ac28b0bec8d47b7cdce253df1db80b46052b5915d963e1bcbab0db4")
        assertEquals("Gimmiegold", SafariEspRules.identify(descriptor)?.name)
        assertTrue(SafariEspRules.targetCurrent("Gimmiegold", descriptor, false, true, true))
        assertFalse(SafariEspRules.targetCurrent("Gimmiegold", descriptor, true, true, true))
        assertFalse(SafariEspRules.targetCurrent("Gimmiegold", descriptor, false, false, true))
        assertFalse(SafariEspRules.targetCurrent("Gimmiegold", descriptor, false, true, false))
        assertFalse(SafariEspRules.targetCurrent("Gimmiegold", descriptor.copy(invisible = true), false, true, true))
        assertFalse(SafariEspRules.targetCurrent("Gimmiegold", descriptor.copy(texture = null), false, true, true))
        assertNull(SafariEspRules.identify(descriptor.copy(invisible = true)))
        assertNull(SafariEspRules.identify(descriptor.copy(texture = null)))
        assertNull(SafariEspRules.identify(descriptor.copy(texture = "changed")))
    }
}
