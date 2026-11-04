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
    @Test fun `shared FIFO finishes each banner once without interrupting its presentation`() {
        val events=mutableListOf<String>();val queue=AlertSequence()
        queue.submit(BannerKind.GEMS,2000,"gems",finish={events.add("gems done")}) {events.add("gems")}
        queue.tick(0)
        queue.submit(BannerKind.WARP,2000,"warp",finish={events.add("warp done")}) {events.add("warp")}
        queue.tick(500);assertEquals(listOf("gems"),events)
        queue.tick(2000);assertEquals(listOf("gems","gems done","warp"),events)
        queue.tick(4000);assertEquals(listOf("gems","gems done","warp","warp done"),events)
        assertFalse(queue.busy)
    }
    @Test fun `clearing one owner preserves other queued alerts and duplicate keys do not repeat`() {
        val started=mutableListOf<String>();val queue=AlertSequence()
        queue.submit(BannerKind.GEMS,2000,"gems") {started.add("gems")};queue.tick(0)
        queue.submit(BannerKind.GEMS,2000,"gems") {started.add("duplicate")}
        queue.submit(BannerKind.WARP,2000,"warp") {started.add("warp")}
        queue.submit(BannerKind.BIRDS,2000,"bird") {started.add("bird")}
        queue.clear(BannerKind.BIRDS);queue.tick(2000)
        assertEquals(listOf("gems","warp"),started)
        queue.reset();queue.tick(9999);assertFalse(queue.busy)
        assertEquals(listOf("gems","warp"),started)
    }
    @Test fun `ongoing audio and invalid requests do not overlap a subsequent alert`() {
        var audio=true;var started=0;val queue=AlertSequence {audio}
        queue.submit("invalid",100,valid={false}) {fail<Unit>("Invalid alert started")}
        queue.submit("next",100) {started++}
        queue.tick(0);assertEquals(0,started)
        audio=false;queue.tick(1);assertEquals(1,started)
        queue.tick(101);assertFalse(queue.busy)
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
