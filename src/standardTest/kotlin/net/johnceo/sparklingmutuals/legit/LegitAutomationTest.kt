package net.johnceo.sparklingmutuals.legit

import java.nio.file.Files
import java.nio.file.Path
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
    @Test fun `legacy values and settings edits cannot enable gameplay automation`() {
        Files.writeString(root.resolve("sparkling-mutuals.properties"),
            "autoClicker=true\nautoParty=true\nwarpAlertsEnabled=true\nautoSelectSafariTicket=true\nsafariEntryAutoEnter=true\ngemzieGemAutoSelect=true\nhideyhoAutoAccept=true\nleechMode=true\nautoReconnectHypixel=true\nautoJoinSkyBlock=true\ncandleHitbox=true\nclearWater=true\n")
        ConfigManager.init(root); ContestConfig.init(root)
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
        ConfigManager.autoClicker = true
        ConfigManager.safariEntryAutoEnter = true
        assertFalse(ConfigManager.autoClicker)
        assertFalse(ConfigManager.safariEntryAutoEnter)
        ConfigManager.save()
        val saved = java.util.Properties().apply { Files.newInputStream(root.resolve("sparkling-mutuals.properties")).use(::load) }
        assertNull(saved.getProperty("autoClicker"))
        assertNull(saved.getProperty("leechMode"))
        val catalog = SettingsCatalog(SafariSettings())
        assertTrue(catalog.search("auto-click").isEmpty())
        assertTrue(catalog.search("auto-entry").isEmpty())
        assertTrue(catalog.search("auto-reconnect").isEmpty())
        assertFalse(catalog.pages.any { it.name == "Sparkle Warping" })
        assertFalse(SafariSettings::class.java.declaredClasses.any { it.simpleName == "Warp" })
    }
}
