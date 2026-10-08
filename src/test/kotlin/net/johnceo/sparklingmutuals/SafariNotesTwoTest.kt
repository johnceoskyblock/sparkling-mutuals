package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.*
import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariNotesTwoTest {
    @TempDir lateinit var dir: Path
    @Test fun `mode selection sets lookup scope and allows only the specified PB kinds`() {
        ConfigManager.init(dir)
        for (mode in SafariMode.entries) {
            SafariFullClear.select(mode)
            assertEquals(mode != SafariMode.FULL_CLEAR, ConfigManager.timesaveOnly)
            for (kind in PartyCommandKind.entries) {
                val command = PartyCommand(kind)
                val allowed = when (kind) {
                    PartyCommandKind.PB_DOOM, PartyCommandKind.PB_WUMPA -> mode != SafariMode.FULL_CLEAR
                    PartyCommandKind.PB_FOREST, PartyCommandKind.PB_CAVERN, PartyCommandKind.PB_ICY, PartyCommandKind.PB_HAUNTED -> mode != SafariMode.SPARKLING
                    else -> true
                }
                assertEquals(allowed, command.allowed(mode), "$mode / $kind")
            }
        }
    }
    @Test fun `party help lists actual active PB commands and fits one server message`() {
        for (mode in SafariMode.entries) {
            val help = CommandHelp.partyReply(mode)
            assertTrue(help.length + 4 <= 256)
            assertFalse(help.contains("doom/wumpa"))
            assertFalse(help.contains("[gui]"))
            assertTrue(help.contains("/sm gui"))
            for (kind in PartyCommandKind.entries.filter { PartyCommand(it).pbName != null }) {
                val command = PartyCommand(kind)
                assertEquals(command.allowed(mode), help.contains(command.text), "$mode / ${command.text}")
            }
            assertTrue(PartyCommand(PartyCommandKind.HELP).matchesResponse(help))
        }
    }
    @Test fun `clicker requires a physical held click on a mound and stops immediately on release or target change`() {
        assertFalse(ClickConditions().allowed())
        assertTrue(ClickConditions(rockmiteMound = true).allowed())
        assertFalse(ClickConditions(rockmiteMound = true, mouseHeld = false).allowed())
        val clock = SafariClickClock()
        assertTrue(clock.tick(ClickConditions(rockmiteMound = true).allowed()))
        assertFalse(clock.tick(ClickConditions(rockmiteMound = true, mouseHeld = false).allowed()))
        assertFalse(clock.tick(ClickConditions(mouseHeld = true, rockmiteMound = false).allowed()))
        assertTrue(clock.tick(ClickConditions(rockmiteMound = true).allowed()))
    }
    @Test fun `candle selection is confined to the Haunted client world while holding incense`() {
        assertTrue(SafariCandleRules.enabled(true, true, true, "Soothing Incense"))
        assertTrue(SafariCandleRules.enabled(true, true, true, "§aSoothing Incense"))
        assertFalse(SafariCandleRules.enabled(false, true, true, "Soothing Incense"))
        assertFalse(SafariCandleRules.enabled(true, false, true, "Soothing Incense"))
        assertFalse(SafariCandleRules.enabled(true, true, false, "Soothing Incense"))
        assertFalse(SafariCandleRules.enabled(true, true, true, "Shining Coin"))
    }
    @Test fun `candle toggle saves independently and descriptions remain brief`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        settings.safari.candleHitbox = false
        settings.apply(); ConfigManager.init(dir)
        assertFalse(ConfigManager.candleHitbox)
        assertTrue(SafariSettings.Warp::class.java.fields.mapNotNull {
            it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.desc
        }.any { it.contains("before entry closes") })
    }
}
