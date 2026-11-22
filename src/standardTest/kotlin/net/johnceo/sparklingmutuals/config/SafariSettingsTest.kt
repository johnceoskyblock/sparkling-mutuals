package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.safari.SafariAreaMap
import net.johnceo.sparklingmutuals.safari.completion.BossPbCategory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariSettingsTest {
    @TempDir lateinit var dir: Path
    @Test fun `each local PB category has an independent switch that survives reload`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        val settings = SafariSettings()
        val catalog = SettingsCatalog(settings)
        val bossCategories = BossPbCategory.entries.zip(listOf("solo", "duo", "observedSolo", "observedDuo"))
        val toggles = buildMap<List<String>, () -> Boolean> {
            for ((bossField, species) in listOf("doom" to "Doomspiral", "wumpa" to "Wumpa")) {
                for ((category, field) in bossCategories) {
                    put(listOf(bossField, field)) { ConfigManager.showBossPbChat(species, category) }
                }
            }
            put(listOf("fullClear")) { ConfigManager.fullClearPbChat }
            put(listOf("unique")) { ConfigManager.uniquePbChat }
            put(listOf("partyUnique")) { ConfigManager.partyUniquePbChat }
        }
        assertTrue(toggles.values.all { it() })
        for (path in toggles.keys) {
            val target = SettingsTarget("Party Interactions", listOf("pbChat") + path)
            val row = catalog.locate(target)!!.second.last()
            assertTrue(catalog.location(row).startsWith("Party → Interactions → Local PB messages"))
            row.write(false)
            settings.apply()
            ConfigManager.init(dir)
            assertEquals(setOf(path), toggles.filterValues { !it() }.keys)
            row.write(true)
        }
        settings.apply()
        assertTrue(toggles.values.all { it() })
    }

    @Test fun `HUD contest visibility saves independently from contest tracking`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val settings=SafariSettings()
        settings.tracking.contestTimer=false
        settings.apply()
        ContestConfig.init(dir)
        assertFalse(ContestConfig.showContestTimer)
        assertTrue(ContestConfig.trackContest)
        assertEquals("HUDs",SettingsCatalog(SafariSettings()).pages.first { it.name == "HUDs" }.name)
        val next=SafariSettings();next.tracking.contestTimer=true;next.apply()
        ContestConfig.init(dir);assertTrue(ContestConfig.showContestTimer)
    }
    @Test fun `Native view applies and persists toggles and independent warnings`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        assertEquals(0, ContestConfig.ticketMode)
        val settings = SafariSettings()
        assertFalse(settings.party.startingItems)
        settings.party.startingItems = true
        settings.safari.teammates.color = "0:255:100:80:200"
        ConfigManager.hotspotAnnouncer = true
        settings.miria.ticketMode = 1
        settings.miria.three = false
        settings.safari.paintings = false
        settings.safari.shiny = false
        settings.party.enabled = false
        settings.apply()
        assertFalse(net.johnceo.sparklingmutuals.alerts.SparkleWarping.preparingParty,
            "Enabling must not prepare or disband the party; /start does that")
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        assertTrue(ContestConfig.firstClassTicket)
        assertTrue(ConfigManager.announceStartingItems)
        assertEquals("0:255:100:80:200", SafariEspConfig.teammateColor)
        assertEquals(0xFF6450C8.toInt(), net.johnceo.sparklingmutuals.safari.esp.TeammateEsp.COLOR)
        assertTrue(ConfigManager.hotspotAnnouncer)
        assertFalse(ConfigManager.hideHauntedPaintings)
        assertFalse(ConfigManager.shinyDetection)
        assertFalse(ConfigManager.partyCommandsEnabled)
        assertFalse(ConfigManager.warpAlertsEnabled)
        assertFalse(ConfigManager.leechMode)
        assertEquals(listOf(1), ContestConfig.warningMinutes())
        settings.miria.five = false
        settings.miria.ticketMode = 0
        settings.miria.one = false
        settings.apply()
        ContestConfig.init(dir)
        assertFalse(ContestConfig.firstClassTicket)
        assertTrue(ContestConfig.warningMinutes().isEmpty())
    }
    @Test fun `bundled 0_9 area table identifies Haunted and rejects outside coordinates`() {
        assertEquals(4, SafariAreaMap.biomeAt(-4.0, 71.0, -79.0))
        assertNull(SafariAreaMap.biomeAt(10000.0, 71.0, 10000.0))
    }
}
