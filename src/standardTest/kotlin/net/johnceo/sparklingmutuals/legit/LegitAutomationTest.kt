package net.johnceo.sparklingmutuals.legit

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import net.johnceo.sparklingmutuals.config.*

class LegitAutomationTest {
    @Test fun `dispatch rejects gameplay and party management commands`() {
        val commands = net.johnceo.sparklingmutuals.alerts.AutomatedCommands
        for (command in listOf("p warp", "p disband", "p accept JohnCEO", "p leave", "p invite Friend", "play sb", "lobby prototype", "accept", "pc hello\np warp"))
            assertFalse(commands.allowed(command), command)
        assertTrue(commands.allowed("pc run complete"))
        assertTrue(commands.allowed("p list"))
        assertTrue(commands.allowed("profileid"))
    }
    @TempDir lateinit var root: Path
    @Test fun `Standard ignores Full values and preserves them when saving or resetting settings`() {
        val file = root.resolve("sparkling-mutuals.properties")
        val fullSettings = listOf("autoClicker", "autoParty", "warpAlertsEnabled", "autoSelectSafariTicket",
            "safariEntryAutoEnter", "gemzieGemAutoSelect", "hideyhoAutoAccept", "leechMode", "autoReconnectHypixel",
            "autoJoinSkyBlock", "candleHitbox", "clearWater").associateWith { "true" } + mapOf(
            "warpList.players" to "TestPlayer",
            "warpList.Foxtrot.priority.testplayer" to "3",
            "warpDelaySeconds" to "25",
            "repartyDelaySeconds" to "20",
            "waterOpacity.forest" to "25",
            "highlightTeammates" to "false",
            "showTeammateNames" to "true",
            "esp.teammate.color" to "0:255:12:34:56",
            "esp.nameplates.distance" to "true",
            "esp.critterBoxes" to "true",
            "esp.floor.coin.textScale" to "4.0",
            "esp.floor.coin.color" to "0:255:65:53:32",
            "esp.floor.lime.alwaysOn" to "false",
            "esp.floor.incense.onlyInBiome" to "true",
            "esp.floor.rewards.enabled" to "true",
            "esp.scrappy.textScale" to "3.0",
            "safariTicketPreference" to "1",
            "migration.sparkleWarpTimers22_27" to "false"
        )
        Files.newOutputStream(file).use { output ->
            Properties().apply { putAll(fullSettings) }.store(output, null)
        }
        fun assertPreserved() {
            val saved = Properties().apply { Files.newInputStream(file).use(::load) }
            assertEquals(fullSettings, fullSettings.keys.associateWith(saved::getProperty))
        }

        ConfigManager.init(root, "4.4.3")
        ContestConfig.init(root)
        assertPreserved()
        assertFalse(ConfigManager.autoClicker)
        assertFalse(ConfigManager.autoParty)
        assertFalse(ConfigManager.warpAlertsEnabled)
        assertFalse(ConfigManager.autoSelectSafariTicket)
        assertFalse(ConfigManager.safariEntryAutoEnter)
        assertFalse(ConfigManager.gemzieGemAutoSelect)
        assertFalse(ConfigManager.hideyhoAutoAccept)
        assertFalse(ConfigManager.leechMode)
        assertFalse(ConfigManager.autoReconnectHypixel)
        assertFalse(ConfigManager.autoJoinSkyBlock)
        assertFalse(ConfigManager.candleHitbox)
        assertFalse(ConfigManager.clearWater)
        assertEquals(27, ConfigManager.warpDelaySeconds)
        assertEquals(0, ConfigManager.safariTicketPreference)
        ConfigManager.autoClicker = true
        ConfigManager.safariEntryAutoEnter = true
        assertFalse(ConfigManager.autoClicker)
        assertFalse(ConfigManager.safariEntryAutoEnter)
        ConfigManager.progressHud = false
        ConfigManager.save()
        assertPreserved()
        ConfigManager.init(root)
        assertFalse(ConfigManager.progressHud)
        ConfigManager.resetSettings()
        assertPreserved()
        assertTrue(ConfigManager.progressHud)
        val catalog = SettingsCatalog(SafariSettings())
        assertTrue(catalog.search("auto-click").isEmpty())
        assertTrue(catalog.search("auto-entry").isEmpty())
        assertTrue(catalog.search("auto-reconnect").isEmpty())
        assertFalse(catalog.pages.any { it.name == "Sparkle Warping" })
        assertFalse(SafariSettings::class.java.declaredClasses.any { it.simpleName == "Warp" })
    }
}
