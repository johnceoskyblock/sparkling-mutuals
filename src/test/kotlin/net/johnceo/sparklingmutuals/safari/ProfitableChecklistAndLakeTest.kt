package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.safari.ui.ProfitableChecklist
import net.johnceo.sparklingmutuals.safari.completion.SharedCritterProgress
import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.hud.SafariHud
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class ProfitableChecklistAndLakeTest {
    @Test fun `global top ten is filtered to each biome without backfilling`() {
        val ranked = listOf("Tepid", "Foxtrot", "Gazer", "Gemzie", "Nozzlenose", "Treefrog", "Scrappy", "Honeybug", "Bloodbat", "Mantis Shrimp", "Macaw")
        for (biome in SafariBiome.entries) {
            val targets = ProfitableChecklist.targets(biome, ranked)
            assertEquals(ranked.take(10).mapNotNull(SafariRoster::named).filter { it.biome == biome }, targets)
            assertTrue(targets.all { it.biome == biome })
        }
        assertFalse(ProfitableChecklist.targets(SafariBiome.FOREST, ranked).any { it.name == "Macaw" })
    }
    @Test fun `remaining captures decrease independently of mode and never below zero`() {
        val run = SafariRun(0)
        assertEquals(6, run.profitableCapturesLeft("Tepid"))
        repeat(2) { run.record(SafariCatch(SafariRoster.named("Tepid")!!)) }
        assertEquals(4, run.profitableCapturesLeft("Tepid"))
        run.sharedCritters.replace("peer", 1, mapOf("Tepid" to SharedCritterProgress.Evidence(true, false, false, 0, 4)))
        assertEquals(2, run.profitableCapturesLeft("Tepid"))
        assertEquals("2 left", ProfitableChecklist.panel(run, SafariBiome.ICY, listOf("Tepid", "Foxtrot")).rows.single().value)
        repeat(8) { run.record(SafariCatch(SafariRoster.named("Tepid")!!)) }
        val zone = net.johnceo.sparklingmutuals.safari.completion.SafariCompletionZones.forBiome(SafariBiome.ICY)["Tepid"]
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), true, playerX = zone?.x ?: -112.0, playerZ = zone?.z ?: -54.0)
        assertEquals(0, run.profitableCapturesLeft("Tepid"))
    }
    @Test fun `qualified rendering is not a profitable capture and unknown sources are not false zeroes`() {
        val run = SafariRun(0)
        run.sharedCritters.replace("peer", 1, mapOf("Tepid" to SharedCritterProgress.Evidence(false, true, false, 6, 0)))
        assertEquals(6, run.profitableCapturesLeft("Tepid"))
        assertNull(run.profitableCapturesLeft("Rockmite"))
        assertEquals("? left", ProfitableChecklist.panel(run, SafariBiome.CAVERN, listOf("Rockmite")).rows.single().value)
        assertTrue(ProfitableChecklist.panel(run, SafariBiome.ICY, emptyList()).rows.single().label.contains("loading"))
    }
    @Test fun `lake requires all three personal catches once per run`() {
        val run = SafariRun(0)
        fun consume() = run.lakeCatchMilestone.consume(run::personalCount)
        for (name in LakeCatchMilestone.species) run.record(SafariCatch(SafariRoster.named(name)!!, personal = false, catcherName = "Peer"))
        assertFalse(consume(), "loot shares do not qualify")
        for (name in listOf("Tepid", "Nozzlenose")) run.record(SafariCatch(SafariRoster.named(name)!!))
        assertFalse(consume())
        run.record(SafariCatch(SafariRoster.named("Mantis Shrimp")!!))
        assertTrue(consume()); assertFalse(consume())
        assertFalse(SafariRun(1).lakeCatchMilestone.consume { 0 })
        assertEquals("Lake Cleared", LakeClearAlert.preview().title)
    }
    @Test fun `both new HUDs have persisted independent size and direct settings links`() {
        val saved = Properties()
        val old = SafariHud.LAKE_CLEARED.layout.scale
        try {
            SafariHud.LAKE_CLEARED.layout.scale = 1.75f
            SafariHud.LAKE_CLEARED.layout.save(saved, "lake_cleared")
            SafariHud.LAKE_CLEARED.layout.scale = .5f
            SafariHud.LAKE_CLEARED.layout.load(saved, "lake_cleared")
            assertEquals(1.75f, SafariHud.LAKE_CLEARED.layout.scale)
            assertNotSame(AppearanceConfig.panels.getValue("profitable"), AppearanceConfig.panels.getValue("lake_cleared"))
            assertEquals(listOf("lakeClearedHud", "enabled"), HudConfigLinks.target(SafariHud.LAKE_CLEARED).fields)
            assertEquals(listOf("profitableHud", "enabled"), HudConfigLinks.target(SafariHud.PROFITABLE).fields)
            val settings = SafariSettings()
            assertTrue(settings.tracking.profitableHud.enabled)
            assertTrue(settings.tracking.lakeClearedHud.enabled)
        } finally { SafariHud.LAKE_CLEARED.layout.scale = old }
    }
}
