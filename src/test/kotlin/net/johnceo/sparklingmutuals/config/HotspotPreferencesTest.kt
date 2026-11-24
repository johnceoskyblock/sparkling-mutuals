package net.johnceo.sparklingmutuals.config

import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.sync.PartySyncProtocol
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class HotspotPreferencesTest {
    @Test fun `shared settings save order and restore defaults for invalid data`() {
        val props = Properties()
        try {
            HotspotPreferences.load(props)
            assertEquals(listOf(SafariBiome.ICY, SafariBiome.FOREST, SafariBiome.HAUNTED, SafariBiome.CAVERN), HotspotPreferences.order)
            props.setProperty("hotspotBiomePreferences", "CAVERN,ICY,HAUNTED,FOREST")
            HotspotPreferences.load(props)
            val saved = Properties()
            HotspotPreferences.save(saved)
            assertEquals("CAVERN,ICY,HAUNTED,FOREST", saved.getProperty("hotspotBiomePreferences"))
            props.setProperty("hotspotBiomePreferences", "ICY,ICY,HAUNTED,FOREST")
            HotspotPreferences.load(props)
            assertEquals(HotspotPreferences.defaults, HotspotPreferences.order)
        } finally {
            HotspotPreferences.load(Properties())
        }
    }

    @Test fun `cached server orders must be complete and canonical`() {
        fun decode(raw: String) = PartySyncProtocol.hotspotPreference(JsonParser.parseString(raw))
        assertEquals(HotspotPreferences.defaults, decode("""["ICY","FOREST","HAUNTED","CAVERN"]"""))
        for (invalid in listOf("null", "[]", """["ICY","FOREST","HAUNTED","ICY"]""", """["ICY","FOREST","HAUNTED",3]"""))
            assertNull(decode(invalid), invalid)
    }
}
