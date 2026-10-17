package net.johnceo.sparklingmutuals.safari.structures

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HoneybugModeEvidenceTest {
    private val honeybug = SafariRoster.named("Honeybug")!!
    private fun party(needs: Boolean) = PartySparklingState().apply {
        select(setOf("me"))
        accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() -
            if (needs) setOf("Honeybug") else emptySet())))
    }
    private fun catch(run: SafariRun, amount: Int) = repeat(amount) { run.record(SafariCatch(honeybug, false)) }

    @Test fun `actual nest totals complete local Full Clear without a report or historical bee sightings`() {
        for (nests in 1..6) {
            val run = SafariRun(0)
            run.visitBiome(SafariBiome.CAVERN)
            run.updateNestEvidence(false, 0, nests, 0)
            run.updateNestEvidence(false, 0, 0, 13) // A later unloaded/empty survey cannot erase the initial total.
            assertEquals(nests, run.honeybugCaptureTarget)
            catch(run, nests - 1)
            assertFalse(run.fullClearMissingComplete("Honeybug"))
            assertFalse(run.fullClearComplete("Honeybug"))
            catch(run, 1)
            assertTrue(run.fullClearMissingComplete("Honeybug"))
            assertTrue(run.fullClearComplete("Honeybug"))
            for (mode in SafariMode.entries) assertFalse(SafariHelperRules.showNests(mode, run, party(true)))
        }
    }

    @Test fun `needed nest highlights outlive the Unique catch until captures plus live bees resolve sources`() {
        for (needs in listOf(false, true)) for (mode in SafariMode.entries) {
            val run = SafariRun(0)
            run.visitBiome(SafariBiome.FOREST)
            run.updateNestEvidence(false, 0, 4, 0)
            catch(run, 1)
            assertTrue(run.uniqueComplete("Honeybug", party(needs)))
            assertEquals(mode == SafariMode.FULL_CLEAR || needs, SafariHelperRules.showNests(mode, run, party(needs)))
            run.liveHoneybugs(2)
            assertEquals(mode == SafariMode.FULL_CLEAR || needs, SafariHelperRules.showNests(mode, run, party(needs)))
            run.liveHoneybugs(3)
            assertEquals(mode == SafariMode.FULL_CLEAR, SafariHelperRules.showNests(mode, run, party(needs)))
            assertFalse(run.fullClearMissingComplete("Honeybug"), "spawned bees are not all captured")
            catch(run, 3)
            run.liveHoneybugs(0)
            run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true, nestsPunched = 4, nestsFound = 4)
            assertTrue(run.fullClearMissingComplete("Honeybug"))
        }
    }

    @Test fun `Forest report survives entry but never supplies Unique or Full Clear captures`() {
        for (mode in SafariMode.entries) {
            val run = SafariRun(0)
            run.visitBiome(SafariBiome.CAVERN)
            run.updateNestEvidence(false, 0, 4, 0)
            run.partyFullClear("Party > Friend: fd")
            assertEquals(if (mode == SafariMode.SPARKLING) 1 else 0, run.completed(mode, listOf(honeybug), party(true)))
            assertEquals(0, run.count("Honeybug"))
            catch(run, 1)
            val expected = if (mode == SafariMode.FULL_CLEAR) 0 else 1
            assertEquals(expected, run.completed(mode, listOf(honeybug), party(true)))
            run.visitBiome(SafariBiome.FOREST)
            run.updateNestEvidence(false, 0, 0, 13)
            assertEquals(expected, run.completed(mode, listOf(honeybug), party(true)), "entering Forest cannot retire trusted checking")
            run.visitBiome(SafariBiome.CAVERN)
            assertEquals(expected, run.completed(mode, listOf(honeybug), party(true)))
            catch(run, 3)
            assertEquals(1, run.completed(mode, listOf(honeybug), party(true)))
            run.observeSparkling(UUID.randomUUID(), "Honeybug")
            assertEquals(0, run.completed(mode, listOf(honeybug), party(true)), "actual Sparkling obligations still block reports")
        }
    }

    @Test fun `own completed nest survey resolves helpers without having to render released bees`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.updateNestEvidence(false, 2, 3, 0)
        catch(run, 1)
        assertTrue(SafariHelperRules.showNests(SafariMode.UNIQUE, run, party(true)))
        run.updateNestEvidence(true, 3, 3, 0)
        for (mode in SafariMode.entries) assertFalse(SafariHelperRules.showNests(mode, run, party(true)))
    }
    @Test fun `fd hides checking helpers but Full Clear keeps unpunched nests until shared capture quota`() {
        val run = SafariRun(0)
        val survey = BeeNestSurvey()
        val sites = BeeNestSurvey.positions.take(4).toSet()
        survey.scan { if (it in sites) NestBlock.NEST else NestBlock.HIVE }
        fun sync() = run.updateNestEvidence(survey.allChecked, survey.punchedCount, survey.nestCount, survey.unverified.size)
        sync()
        run.visitBiome(SafariBiome.CAVERN)
        run.partyFullClear("Party > Friend: fd")
        run.visitBiome(SafariBiome.FOREST)
        run.liveHoneybugs(4)
        assertTrue(SafariHelperRules.showNests(SafariMode.FULL_CLEAR, run, party(true)))
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING))
            assertFalse(SafariHelperRules.showNests(mode, run, party(true)))
        val punched = sites.first()
        survey.punch(punched); sync()
        assertEquals(3, survey.waypoints(SafariHelperRules.showNests(SafariMode.FULL_CLEAR, run, party(true))).size)
        assertFalse(survey.waypoints(true).any { it.position == punched })
        catch(run, 3)
        assertTrue(SafariHelperRules.showNests(SafariMode.FULL_CLEAR, run, party(true)))
        catch(run, 1)
        assertFalse(SafariHelperRules.showNests(SafariMode.FULL_CLEAR, run, party(true)))
        assertTrue(survey.waypoints(SafariHelperRules.showNests(SafariMode.FULL_CLEAR, run, party(true))).isEmpty())
    }

    @Test fun `Unique Honeybug missing row clears on one catch while needed Sparkling progress waits for checking`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.CAVERN)
        run.updateNestEvidence(false, 0, 4, 0)
        catch(run, 1)
        assertTrue(run.uniqueComplete("Honeybug", party(true)), "the missing row uses its first catch")
        assertEquals(0, run.completed(SafariMode.UNIQUE, listOf(honeybug), party(true)))
        assertEquals(1, run.completed(SafariMode.UNIQUE, listOf(honeybug), party(false)))
        run.partyFullClear("Party > Friend: fd")
        assertEquals(1, run.completed(SafariMode.UNIQUE, listOf(honeybug), party(true)))
        val local = SafariRun(0)
        local.visitBiome(SafariBiome.FOREST)
        local.updateNestEvidence(false, 0, 4, 0)
        catch(local, 1)
        assertEquals(0, local.completed(SafariMode.SPARKLING, listOf(honeybug), party(true)))
        local.updateNestEvidence(true, 4, 4, 0)
        assertEquals(1, local.completed(SafariMode.UNIQUE, listOf(honeybug), party(true)))
        assertEquals(1, local.completed(SafariMode.SPARKLING, listOf(honeybug), party(true)))
    }

}
