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
            "warpList.player.testplayer.alwaysInvite" to "true",
            "banner.warp.text" to "Warp time",
            "hud.warp.scale" to "1.3",
            "appearance.warp.borderEnabled" to "true",
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
            "esp.cavern.onlyInBiome" to "false",
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
        assertFalse(SafariEspConfig.standardCritterBoxes)
        assertTrue(SafariEspConfig.groups.getValue("cavern").onlyInBiome)
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
        assertTrue(catalog.search("Sparkling invite list").isEmpty())
        assertTrue(catalog.search("Warping now").isEmpty())
        assertTrue(catalog.search("Warping HUD").isEmpty())
        assertNull(catalog.locate(SettingsTarget("ESP General", listOf("biomes"))))
        assertTrue(catalog.search("Forest critters only in Forest").isEmpty())
        assertFalse(net.johnceo.sparklingmutuals.hud.SafariHud.WARP in net.johnceo.sparklingmutuals.hud.SafariHud.available)
        assertFalse(SafariSettings::class.java.declaredClasses.any { it.simpleName == "Warp" })
    }

    @Test fun `Standard kick banner recognizes kick reports and stays in the run and party group`() {
        ConfigManager.init(root)
        ContestConfig.init(root)
        val settings = SafariSettings()
        val catalog = SettingsCatalog(settings)
        assertTrue(catalog.locate(SettingsTarget("Banner", listOf("safari", "partyKicked", "enabled"))) != null)
        assertTrue(catalog.locate(SettingsTarget("Safari helpers", listOf("capture", "hideyhoKey"))) != null)
        val guard = net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard
        assertTrue(guard.isKickReport("kick!"))
        assertTrue(guard.isKickReport("I got kicked"))
        assertFalse(guard.isKickReport("kickstart the run"))
        assertFalse(guard.isKickReport("can someone kick Steve?"))
        settings.banner.safari.partyKicked.enabled = false
        settings.banner.apply()
        ConfigManager.saveSettings()
        ConfigManager.init(root)
        assertFalse(guard.tracksKickReports())
    }
}
