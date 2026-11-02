package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Properties

class SafariBannerTest {
    @TempDir lateinit var dir: Path
    private fun notice(kind: BannerKind)=BannerNotice(kind,kind.text,-1,2000,9,1f,1f)
    @Test fun `priority queue preserves remaining time without repeating interrupted sound`() {
        val queue=BannerQueue()
        queue.submit(notice(BannerKind.GEMS),0)
        assertEquals(1,queue.sounds().size)
        queue.submit(notice(BannerKind.GEMS),100)
        assertTrue(queue.sounds().isEmpty())
        queue.submit(notice(BannerKind.WARP),500)
        assertEquals(BannerKind.WARP,queue.current!!.notice.kind)
        assertEquals(1,queue.sounds().size)
        queue.advance(2500)
        assertEquals(BannerKind.GEMS,queue.current!!.notice.kind)
        assertEquals(1500,queue.current!!.notice.duration)
        assertEquals(0f,queue.sounds().single().volume)
        queue.advance(4000);assertNull(queue.current)
    }
    @Test fun `sound only leaves the active banner alone and clearing removes queued events`() {
        val queue=BannerQueue();queue.submit(notice(BannerKind.GEMS),0);queue.sounds()
        queue.submit(notice(BannerKind.WUMPA_DONE).copy(soundOnly=true),50)
        assertEquals(BannerKind.GEMS,queue.current!!.notice.kind)
        assertTrue(queue.sounds().single().soundOnly)
        queue.submit(notice(BannerKind.BIRDS),100)
        queue.clear(BannerKind.BIRDS);queue.advance(2000)
        assertNull(queue.current)
        queue.submit(notice(BannerKind.GEMS),3000);queue.clear();assertTrue(queue.sounds().isEmpty())
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
        assertTrue(banner.any { it.name=="Bottom timer bar" })
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
}
