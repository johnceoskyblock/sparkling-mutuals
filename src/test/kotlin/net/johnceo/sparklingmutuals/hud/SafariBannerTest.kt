package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files
import java.util.Properties

class SafariBannerTest {
    @TempDir lateinit var dir: Path
    private fun notice(kind: BannerKind)=BannerNotice(kind,kind.text,-1,2000,9,1f,1f)
    @Test fun `visual ends on time while audio holds the next alert`() {
        var playing = false
        val queue = AlertSequence { playing }
        val events = mutableListOf<String>()
        queue.submit("run", 500, finish = { events.add("hide") }) { playing = true }
        queue.submit("next", 500) { events.add("next") }
        queue.tick(0)
        queue.tick(500)
        assertEquals(listOf("hide"), events)
        assertTrue(queue.busy)
        playing = false
        queue.tick(750)
        assertEquals(listOf("hide", "next"), events)
    }
    @Test fun `banners and celebrations run FIFO and start sound once`() {
        val queue = AlertSequence()
        val events = mutableListOf<String>()
        queue.submit("banner", 2_000, "gems", finish = { events.add("finish gems") }) { events.add("gems") }
        queue.submit("banner", 2_000, "gems") { fail<Unit>("duplicate banner") }
        queue.submit("catch", 3_000, finish = { events.add("finish catch") }) { events.add("catch") }
        queue.submit("banner", 2_000, "warp") { events.add("warp") }
        queue.tick(0)
        queue.tick(1_999)
        assertEquals(listOf("gems"), events)
        queue.tick(2_000)
        queue.tick(4_999)
        assertEquals(listOf("gems", "finish gems", "catch"), events)
        queue.tick(5_000)
        assertEquals(listOf("gems", "finish gems", "catch", "finish catch", "warp"), events)
    }
    @Test fun `a queued discovery does not start during the lottery lead in or the reels`() {
        val queue = AlertSequence()
        var sounds = 0
        queue.submit("sparkling", 2_000, "Doomspiral") { sounds++ }
        queue.tick(0, paused = true)
        queue.tick(4_000, paused = true)
        queue.tick(8_900, paused = true)
        assertEquals(0, sounds)
        queue.tick(8_950)
        queue.tick(9_000)
        assertEquals(1, sounds)
    }

    @Test fun `disabling features preserves both active and pending coin celebrations`() {
        val queue = AlertSequence()
        val events = mutableListOf<String>()
        val coins = "coins"
        queue.submit(coins, 500, 7, finish = { events.add("finish 7") }) { events.add("start 7") }
        queue.submit("banner", 500) { fail<Unit>("disabled banner") }
        queue.submit(coins, 500, 8, finish = { events.add("finish 8") }) { events.add("start 8") }

        queue.tick(0)
        queue.reset(except = coins)
        assertEquals(listOf("start 7"), events)
        queue.tick(500)
        assertEquals(listOf("start 7", "finish 7", "start 8"), events)

        queue.reset()
        assertEquals(listOf("start 7", "finish 7", "start 8", "finish 8"), events)
        assertFalse(queue.busy)
    }

    @Test fun `sound only waits its turn and stale requests can be skipped`() {
        val queue = AlertSequence()
        val events = mutableListOf<String>()
        queue.submit("banner", 2_000, "visual") { events.add("visual") }
        queue.submit("banner", 1_000, "sound") { events.add("sound") }
        queue.submit("hotspot", 2_000, valid = { false }) { fail<Unit>("stale alert") }
        queue.submit("detection", 2_000) { events.add("detection") }
        queue.tick(0)
        queue.tick(2_000)
        assertEquals(listOf("visual", "sound"), events)
        queue.tick(3_000)
        assertEquals(listOf("visual", "sound", "detection"), events)
    }
    @Test fun `fade and duration bar follow elapsed time`() {
        val active=ActiveBanner(notice(BannerKind.GEMS),100)
        assertEquals(0f,active.alpha(100));assertEquals(1f,active.alpha(500))
        assertEquals(.5f,active.alpha(1600));assertEquals(.5f,active.remaining(1100))
        assertEquals(0f,active.remaining(9999))
    }
    @Test fun `new settings migrate disabled old alerts and keep appearance and per alert options`() {
        val p=Properties().apply { setProperty("allGemsAlert","false");setProperty("runCompleteTitle","Cleared!") }
        BannerConfig.load(p)
        assertFalse(BannerConfig.options.getValue(BannerKind.GEMS).enabled)
        assertEquals("Cleared!",BannerConfig.options.getValue(BannerKind.RUN_COMPLETE).text)
        BannerConfig.options[BannerKind.GEMS]=BannerConfig.options.getValue(BannerKind.GEMS).copy(enabled=true,sound=44,duration=7f)
        BannerConfig.bottomBar=2;BannerConfig.save(p);BannerConfig.load(p)
        assertTrue(BannerConfig.options.getValue(BannerKind.GEMS).enabled)
        assertEquals(44,BannerConfig.options.getValue(BannerKind.GEMS).sound)
        assertEquals(7f,BannerConfig.options.getValue(BannerKind.GEMS).duration)
        assertEquals(2,BannerConfig.bottomBar)
    }
    @Test fun `banner controls and moved customization appear only in their intended categories`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val settings=SafariSettings();val pages=SettingsCatalog(settings).pages
        fun all(rows: List<SettingsCatalog.Row>): List<SettingsCatalog.Row> = rows.flatMap { listOf(it)+all(it.children) }
        val banner=all(pages.single { it.name=="Banner" }.rows)
        assertTrue(banner.any { it.name=="Bottom countdown bar" })
        assertEquals(BannerKind.entries.count { it.supported },banner.count { it.owner is BannerOption && it.name=="Enabled" })
        assertTrue(all(pages.single { it.name=="ESP" }.rows).none { it.name.contains("banner",true) || it.name.endsWith("HUD",true) })
        val appearance=all(pages.single { it.name=="Customization" }.rows)
        assertTrue(appearance.none { it.name in setOf("Alert HUDs","Critter colors","Highlights and waypoints") })
        assertTrue(all(pages.single { it.name=="Safari helpers" }.rows).none { it.name in setOf("All Gems Alert","Run completion sound","Warning") })
        assertSame(settings.customization.critters,settings.esp.critterColors)
        assertSame(settings.customization.alert,settings.safari.alerts.appearance)
        settings.banner.encounters.food.enabled=false;settings.apply();ConfigManager.init(dir)
        assertFalse(BannerConfig.options.getValue(BannerKind.BIRD_FOOD).enabled)
    }
    @Test fun `Hideyho location is restored once in Full and preserved by Standard settings`() {
        val path = dir.resolve("sparkling-mutuals.properties")
        val enabled = "banner.hideyho_location.enabled"
        Files.writeString(path, "$enabled=false\nbanner.hideyho_location.text=Custom <LOCATION>\n")
        ContestConfig.init(dir)
        ConfigManager.init(dir, "4.4.5")
        val settings = SafariSettings()
        val row = SettingsCatalog(settings).locate(SettingsTarget("Banner", listOf("encounters", "hideyho", "enabled")))?.second?.last()
        if (SafariSettings.standardEdition) {
            assertNull(row)
            assertFalse(BannerKind.HIDEYHO_LOCATION.supported)
            ConfigManager.saveSettings()
            ConfigManager.resetSettings()
        } else {
            assertEquals(true, row?.read())
            row!!.write(false)
            assertTrue(settings.banner.apply())
            ConfigManager.saveSettings()
            ConfigManager.init(dir, "4.4.5")
            assertFalse(BannerConfig.options.getValue(BannerKind.HIDEYHO_LOCATION).enabled)
        }
        val saved = Properties().apply { Files.newInputStream(path).use(::load) }
        assertEquals("false", saved.getProperty(enabled))
        assertEquals("Custom <LOCATION>", saved.getProperty("banner.hideyho_location.text"))
        assertEquals(if (SafariSettings.standardEdition) null else "true", saved.getProperty(BannerConfig.HIDEYHO_MIGRATION))
    }
}
