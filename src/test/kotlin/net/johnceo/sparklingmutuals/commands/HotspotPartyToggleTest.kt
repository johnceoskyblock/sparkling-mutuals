package net.johnceo.sparklingmutuals.commands

import java.nio.file.Files
import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HotspotPartyToggleTest {
    @TempDir lateinit var dir: Path
    @Test fun `party hotspot controls ignore the removed opt-out and only emit local feedback`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "acceptPartyHotspotCommands=false\n")
        ConfigManager.init(dir)
        ConfigManager.partyCommandsEnabled=false
        val local=mutableListOf<String>()
        assertTrue(HotspotToggle.fromParty("Party > [MVP+] Alice: !hotspot on") {local.add(it.string)})
        assertTrue(ConfigManager.hotspotAnnouncer)
        assertEquals(listOf("[SM] Hotspot assigning on."),local)
        assertTrue(HotspotToggle.fromParty("Party > Bob: !HOTSPOT OFF") {local.add(it.string)})
        assertFalse(ConfigManager.hotspotAnnouncer)
        assertEquals("[SM] Hotspot assigning off.",local.last())
        ConfigManager.init(dir)
        assertFalse(ConfigManager.hotspotAnnouncer)
        assertFalse(ConfigManager.partyCommandsEnabled)
        for(line in listOf("Guild > Alice: !hotspot on","Alice: !hotspot on","Party > Alice: please !hotspot on","Party > Alice: !hotspot on please"))
            assertFalse(HotspotToggle.fromParty(line) {fail("Unrelated chat must not change the setting")})
    }
}
