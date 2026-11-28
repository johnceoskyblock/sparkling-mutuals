package net.johnceo.sparklingmutuals.commands

import java.nio.file.Files
import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HotspotPartyToggleTest {
    @TempDir lateinit var dir: Path
    @Test fun `party hotspot controls ignore the removed opt-out and only emit local feedback`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "acceptPartyHotspotCommands=false\n")
        ConfigManager.init(dir)
        ConfigManager.partyCommandsEnabled=false
        val local = mutableListOf<Component>()
        assertTrue(HotspotToggle.fromParty("Party > [MVP+] Alice: !hotspot on") { local.add(it) })
        assertTrue(ConfigManager.hotspotAnnouncer)
        assertEquals(listOf("[SM] Hotspot assigning on.", "[SM] Want to set your biome preferences? [Preferences]"),
            local.map { it.string })
        val link = local.last().siblings.last().siblings.single()
        assertEquals(ClickEvent.RunCommand("/hotspot preferences"), link.style.clickEvent)
        local.clear()
        assertTrue(HotspotToggle.fromParty("Party > Alice: !hotspot on") { local.add(it) })
        assertEquals(listOf("[SM] Hotspot assigning on."), local.map { it.string })
        local.clear()
        assertTrue(HotspotToggle.fromParty("Party > Bob: !HOTSPOT OFF") { local.add(it) })
        assertFalse(ConfigManager.hotspotAnnouncer)
        assertEquals(listOf("[SM] Hotspot assigning off."), local.map { it.string })
        ConfigManager.init(dir)
        assertFalse(ConfigManager.hotspotAnnouncer)
        assertFalse(ConfigManager.partyCommandsEnabled)
        for(line in listOf("Guild > Alice: !hotspot on","Alice: !hotspot on","Party > Alice: please !hotspot on","Party > Alice: !hotspot on please"))
            assertFalse(HotspotToggle.fromParty(line) {fail("Unrelated chat must not change the setting")})
    }
}
