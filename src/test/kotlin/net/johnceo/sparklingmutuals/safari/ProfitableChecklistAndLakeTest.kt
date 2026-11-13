package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.safari.ui.ProfitableChecklist
import net.johnceo.sparklingmutuals.safari.completion.SharedCritterProgress
import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.hud.SafariHud
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class ProfitableChecklistAndLakeTest {
    @Test fun `profitable ESP species and parent toggles immediately filter checklist without backfill`() {
        val profitable = SafariEspConfig.profitableToggles.toMap()
        val modWide = SafariEspConfig.modWideToggles.toMap()
        try {
            SafariEspConfig.profitableToggles.keys.forEach { SafariEspConfig.profitableToggles[it] = true }
            SafariEspConfig.modWideToggles.keys.forEach { SafariEspConfig.modWideToggles[it] = true }
            val ranked = listOf("Foxtrot", "Bluebird", "Honeybug", "Treefrog", "Woodchucker", "Fluffling", "Hideonfloor", "Parakeet", "Tepid", "Gazer", "Macaw")
            assertTrue(ProfitableChecklist.targets(SafariBiome.FOREST, ranked).any { it.name == "Foxtrot" })
            SafariEspConfig.profitableToggles["Foxtrot"] = false
            val filtered = ProfitableChecklist.targets(SafariBiome.FOREST, ranked).map { it.name }
            assertFalse("Foxtrot" in filtered); assertFalse("Macaw" in filtered)
            assertTrue("Bluebird" in filtered)
            assertFalse(ProfitableChecklist.panel(SafariRun(0), SafariBiome.FOREST, ranked).rows.any { it.label == "Foxtrot" })
            SafariEspConfig.profitableToggles["Foxtrot"] = true
            assertTrue(ProfitableChecklist.targets(SafariBiome.FOREST, ranked).any { it.name == "Foxtrot" })
            SafariEspConfig.modWideToggles["Foxtrot"] = false
            assertFalse(ProfitableChecklist.targets(SafariBiome.FOREST, ranked).any { it.name == "Foxtrot" })
            SafariEspConfig.profitableToggles.keys.forEach { SafariEspConfig.profitableToggles[it] = false }
            assertTrue(ProfitableChecklist.panel(SafariRun(0), SafariBiome.FOREST, ranked).rows.single().label.contains("No enabled"))
        } finally {
            SafariEspConfig.profitableToggles.putAll(profitable)
            SafariEspConfig.modWideToggles.putAll(modWide)
        }
    }
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
    @Test fun `walls never become Snoozle counts and a cleared empty survey can prove zero`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.CAVERN)
        run.observeCavernSources(false, true, false)
        fun value() = ProfitableChecklist.panel(run, SafariBiome.CAVERN, listOf("Snoozle")).rows.single().value
        assertNull(run.profitableCapturesLeft("Snoozle")); assertEquals("Check walls", value())
        run.observePopulation(List(2) { "Snoozle" to java.util.UUID.randomUUID() })
        assertEquals(2, run.profitableCapturesLeft("Snoozle")); assertEquals("2+ left", value())
        run.observeCavernSources(false, false, true)
        assertEquals("2 left", value(), "remaining opportunities did not add five presumed Snoozles")
        val empty = SafariRun(0)
        empty.observeCavernSources(false, false, true)
        empty.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        assertEquals(0, empty.profitableCapturesLeft("Snoozle"), "walls may contain no Snoozles")
        assertEquals("All caught", ProfitableChecklist.panel(empty, SafariBiome.CAVERN, listOf("Snoozle")).rows.single().value)
    }
    @Test fun `pending food shows an action and only confirmed bird spawns add counts`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        fun value(name: String) = ProfitableChecklist.panel(run, SafariBiome.FOREST, listOf(name)).rows.single().value
        for (name in listOf("Bluebird", "Parakeet", "Macaw")) assertEquals("Use bird food", value(name))
        run.birds.inventory(listOf("Bag of Seeds" to 12))
        assertNull(run.profitableCapturesLeft("Bluebird"), "food quantity is not a bird population")
        run.birds.spawn("A Bluebird was attracted to the Birdfeeder!")
        assertEquals("1+ left", value("Bluebird"))
        run.record(SafariCatch(SafariRoster.named("Bluebird")!!))
        assertEquals("Use bird food", value("Bluebird"), "one caught bird cannot finish remaining feeding opportunities")
    }
    @Test fun `profit checklist counts every confirmed Macaw instead of using Full Clear waiver`() {
        val run = SafariRun(0)
        run.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        run.sharedCritters.replace("peer", 1, mapOf("Macaw" to SharedCritterProgress.Evidence(true, true, true, 2, 1)))
        assertEquals(1, run.profitableCapturesLeft("Macaw"), "a shared mode-complete flag cannot erase an uncaught profitable Macaw")
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
