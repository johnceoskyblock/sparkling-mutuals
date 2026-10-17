package net.johnceo.sparklingmutuals.safari.integration

import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoHiding
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoLocationWatch
import net.johnceo.sparklingmutuals.safari.resources.FloorDropState
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.johnceo.sparklingmutuals.safari.resources.SafariResources
import net.johnceo.sparklingmutuals.safari.resources.StartingResourceReminder
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariFeatureIntegrationTest {
    @TempDir lateinit var dir: Path
    private fun party(vararg missing: String) = PartySparklingState().apply {
        select(setOf("local"))
        accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - missing.toSet()))
    }

    @Test fun `fresh GUI independently switchable features are on except the two requested exceptions`() {
        ConfigManager.init(dir); ContestConfig.init(dir)
        val switches = linkedMapOf<String, Boolean>()
        fun inspect(view: Any, path: String = "") {
            view.javaClass.fields.forEach { field ->
                val name = if (path.isEmpty()) field.name else "$path.${field.name}"
                when {
                    field.getAnnotation(ConfigEditorBoolean::class.java) != null -> switches[name] = field.getBoolean(view)
                    field.getAnnotation(Category::class.java) != null || field.getAnnotation(Accordion::class.java) != null -> inspect(field.get(view), name)
                }
            }
        }
        inspect(SafariSettings())
        assertTrue(switches.size >= 40, "Inspect nested GUI options, including panel borders")
        val exceptions = setOf("history.instaSell", "history.toParty", "warp.enabled")
        val modes = setOf("modes.unique.enabled", "modes.fullClear.enabled", "modes.sparkling.enabled")
        switches.filterKeys { it !in modes }.forEach { (name, value) -> assertEquals(name !in exceptions, value, name) }
        assertEquals(1, modes.count { switches[it] == true })
        assertTrue(ConfigManager.achievements)
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
    }

    @Test fun `saved independent GUI choices and explicit exception opt-ins survive restart`() {
        ConfigManager.init(dir)
        ConfigManager.warpAlertsEnabled = false
        ConfigManager.hideCaptureChat = false
        ConfigManager.shinyDetection = false
        ConfigManager.runCoinsInstaSell = true
        ConfigManager.achievementsToParty = true
        AppearanceConfig.panels.values.forEach { it.borderEnabled = false }
        ConfigManager.save(); ConfigManager.init(dir)
        assertFalse(ConfigManager.warpAlertsEnabled)
        assertFalse(ConfigManager.hideCaptureChat)
        assertFalse(ConfigManager.shinyDetection)
        assertTrue(ConfigManager.runCoinsInstaSell)
        assertTrue(ConfigManager.achievementsToParty)
        assertTrue(AppearanceConfig.panels.values.none { it.borderEnabled })
    }

    @Test fun `missing settings use new defaults without replacing stored values or selections`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "shinyDetection=false\nwarpDelaySeconds=42\nshowWhere=2\n")
        ConfigManager.init(dir)
        assertFalse(ConfigManager.shinyDetection)
        assertFalse(ConfigManager.warpAlertsEnabled)
        assertTrue(ConfigManager.hideCaptureChat)
        assertTrue(AppearanceConfig.panels.values.all { it.borderEnabled })
        assertEquals(42, ConfigManager.warpDelaySeconds)
        assertEquals(2, ConfigManager.showWhere)
        assertFalse(ConfigManager.runCoinsInstaSell)
        assertFalse(ConfigManager.achievementsToParty)
    }

    @Test fun `fixed counters explicitly distinguish caught totals from currently checked population`() {
        for ((name, total) in mapOf("Scrappy" to 3, "Gemzie" to 3, "Troodon" to 3, "Gazer" to 4)) {
            val run = SafariRun(0)
            val critter = SafariRoster.named(name)!!
            run.record(SafariCatch(critter, false))
            val (x, z) = if (name == "Gemzie") -141.0 to 51.0 else 0.0 to 0.0
            run.sparklingChecks.scan(critter.biome, x, z, List(total - 1) { name to UUID.randomUUID() })
            val missing = party(name)
            assertEquals("1/$total captured", SafariPanels.fixedCounter(name, run, SafariMode.FULL_CLEAR, missing))
            assertEquals("$total/$total checked", SafariPanels.fixedCounter(name, run, SafariMode.SPARKLING, missing))
            assertEquals("$total/$total checked", SafariPanels.fixedCounter(name, run, SafariMode.UNIQUE, missing))
            assertNull(SafariPanels.fixedCounter(name, run, SafariMode.UNIQUE, party()))
            repeat(total + 2) { run.record(SafariCatch(critter, false)) }
            assertEquals("$total/$total captured", SafariPanels.fixedCounter(name, run, SafariMode.FULL_CLEAR, missing))
        }
        assertNull(SafariPanels.fixedCounter("Foxtrot", SafariRun(0), SafariMode.FULL_CLEAR, party()))
    }

    @Test fun `Unique Gemzie catch completes independently of the three-spawn Sparkling check`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        run.record(SafariCatch(SafariRoster.named("Gemzie")!!, false))
        run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, listOf("Gemzie" to UUID.randomUUID()))
        val panel = SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, party = party("Gemzie"))
        assertEquals(0, panel.rows.count { it.label == "Gemzie" })
        assertEquals(2, run.sparklingChecks.checkCount("Gemzie"), "ordinary checking evidence stays independent of Unique completion")
        assertTrue(run.uniqueComplete("Gemzie", party("Gemzie")))
        assertTrue(run.uniqueComplete("Gemzie", party()))
    }

    @Test fun `Sparkling incense is controlled by Doomspiral discovery and floor pickup cutoff`() {
        val state = FloorDropState()
        state.startingGrant(FloorDropType.INCENSE)
        state.inventory(listOf("Soothing Incense" to 2))
        assertTrue(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral")))
        assertFalse(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Gimmiegold")))
        assertTrue(state.needed(FloorDropType.COIN, SafariMode.SPARKLING, party("Gimmiegold")))
        state.record("FLOOR DROP! You found 3x Soothing Incense on the ground!", SafariBiome.HAUNTED)
        assertTrue(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral", "Gimmiegold")))
        state.record("Party > Other: FLOOR DROP! Soothing Incense", SafariBiome.HAUNTED)
        state.record("FLOOR DROP! Soothing Incense Shard", SafariBiome.HAUNTED)
        assertTrue(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral")))
        state.record("FLOOR DROP! Soothing Incense", SafariBiome.HAUNTED)
        assertFalse(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral", "Gimmiegold")))
        assertTrue(state.needed(FloorDropType.COIN, SafariMode.SPARKLING, party("Doomspiral", "Gimmiegold")))
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.FULL_CLEAR))
            assertFalse(state.needed(FloorDropType.INCENSE, mode, party("Gimmiegold")), "Four floor pickups hide incense in every mode")
        state.reset()
        assertTrue(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral")))
    }

    @Test fun `Sparkling hides unneeded coins and latches four incense including two starting items`() {
        val state = FloorDropState()
        assertFalse(state.needed(FloorDropType.COIN, SafariMode.SPARKLING, party()))
        assertFalse(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.SPARKLING, true, null, party()))
        assertTrue(state.needed(FloorDropType.COIN, SafariMode.SPARKLING, party("Gimmiegold")))
        assertFalse(state.needed(FloorDropType.COIN, SafariMode.SPARKLING, party("Doomspiral")))
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.FULL_CLEAR))
            assertEquals(mode == SafariMode.FULL_CLEAR, state.needed(FloorDropType.COIN, mode, party()))
        state.inventory(listOf("Soothing Incense" to 2))
        repeat(2) { state.record("FLOOR DROP! You found Soothing Incense on the ground!", SafariBiome.HAUNTED) }
        state.inventory(listOf("§fSoothing Incense" to 2, "Soothing Incense" to 2), initial = false)
        assertFalse(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral")))
        state.inventory(emptyList(), initial = false)
        state.visit(SafariBiome.FOREST); state.visit(SafariBiome.HAUNTED)
        assertFalse(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral", "Gimmiegold")))
        assertFalse(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.SPARKLING, true, null, party("Doomspiral")))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.SPARKLING, true, null, party("Doomspiral", "Gimmiegold")))
        state.reset()
        assertTrue(state.needed(FloorDropType.INCENSE, SafariMode.SPARKLING, party("Doomspiral")))
    }

    @Test fun `remaining Haunted drops block Gimmiegold checks despite the minimum captured population`() {
        val run = SafariRun(0)
        (1..3).forEach { run.gimmiegoldCheck.floor(it, true) }
        run.inventoryEvidence(emptyList(), 10000)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        run.sparklingChecks.inventory(emptyList())
        run.scanGimmiegolds(emptyList(), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = 1)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        assertTrue(run.uniqueComplete("Gimmiegold", party("Gimmiegold")))
        run.scanGimmiegolds(emptyList(), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = -1)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.scanGimmiegolds(emptyList(), true)
        (1..3).forEach { run.gimmiegoldCheck.floor(it, false) }
        run.scanGimmiegolds(emptyList(), true)
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, emptyList(), hauntedDropsRemaining = 0)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
    }

    @Test fun `Hideyho watch is Haunted-only and leaving cancels pending dialogue`() {
        val watch = HideyhoLocationWatch()
        val before = HideyhoHiding.Spot(-4, 68, -64)
        val after = HideyhoHiding.Spot(37, 70, -14)
        for (biome in listOf(null, SafariBiome.FOREST, SafariBiome.CAVERN, SafariBiome.ICY)) {
            assertFalse(watch.context(true, biome))
            watch.chat("[MOB] Hideyho: No peeking!", 1000, before)
            assertFalse(watch.armed)
            assertNull(watch.settled(after, 2000))
        }
        assertTrue(watch.context(true, SafariBiome.HAUNTED))
        watch.chat("[MOB] Hideyho: No peeking!", 3000, before)
        assertTrue(watch.armed)
        assertNull(watch.settled(before, 4000))
        assertFalse(watch.context(true, SafariBiome.FOREST))
        assertFalse(watch.armed)
        assertNull(watch.settled(after, 5000))
        assertTrue(watch.context(true, SafariBiome.HAUNTED))
        assertEquals(after, watch.settled(after, 6000)) // Current idle sighting in Haunted is allowed.
        assertNull(watch.settled(after, 7000))
        assertFalse(watch.context(false, SafariBiome.HAUNTED))
        assertNull(watch.settled(before, 8000))
    }

    @Test fun `automatic resource title and party notice share discovery destination and use gates`() {
        val resources = SafariResources()
        resources.inventory(listOf("Shining Coin" to 1, "Bag of Seeds" to 1), true)
        val missing = party("Gimmiegold", "Macaw")
        assertTrue(resources.notices("Local", SafariBiome.CAVERN, party()).isEmpty())
        val notices = resources.notices("Local", SafariBiome.CAVERN, missing)
        assertEquals(2, notices.size)
        val reminders = notices.map { StartingResourceReminder.parse(it, missing)!! }
        assertEquals(setOf("USE YOUR SHINING COIN!", "USE YOUR BIRD FOOD!"), reminders.map { it.title }.toSet())
        reminders.forEach { reminder ->
            assertTrue(reminder.detail.contains("1x ${reminder.type.label}"))
            assertTrue(reminder.detail.contains(reminder.destination.label))
            assertTrue(reminder.detail.contains("party needs Sparkling"))
            assertEquals(1, reminder.missing.size)
        }
        notices.forEach { notice ->
            assertTrue(notice.startsWith("[SM] Use: "))
            assertFalse(notice.contains("party needs"))
            assertTrue(resources.noticeStillRelevant(notice, SafariBiome.CAVERN, missing))
            assertFalse(resources.noticeStillRelevant(notice, SafariBiome.CAVERN, party()))
            val reminder = StartingResourceReminder.parse(notice, missing)!!
            assertFalse(resources.noticeStillRelevant(notice, reminder.destination, missing))
            assertTrue(SafariResources().partyNotice("Party > Local: $notice"))
        }
        notices.forEach { resources.noticeSent("Local", it) }
        assertTrue(resources.notices("Local", SafariBiome.CAVERN, missing).isEmpty())
        resources.inventory(emptyList(), false)
        notices.forEach { assertFalse(resources.noticeStillRelevant(it, SafariBiome.CAVERN, missing)) }
    }
    @Test fun `Hideyho inside outside watch is disabled in Sparkling and mode switches cancel pending alerts`() {
        val before = HideyhoHiding.Spot(-4, 68, -64)
        val after = HideyhoHiding.Spot(37, 70, -14)
        val watch = HideyhoLocationWatch()
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.FULL_CLEAR)) {
            assertTrue(watch.context(true, SafariBiome.HAUNTED, mode))
            watch.chat("[MOB] Hideyho: No peeking!", 1000, before)
            assertTrue(watch.armed)
            assertFalse(watch.context(true, SafariBiome.HAUNTED, SafariMode.SPARKLING))
            assertFalse(watch.armed)
            watch.chat("[MOB] Hideyho: No peeking!", 2000, before)
            assertNull(watch.settled(after, 3000))
            assertTrue(watch.context(true, SafariBiome.HAUNTED, mode))
            assertEquals(after, watch.settled(after, 4000))
            watch.reset()
        }
    }

}
