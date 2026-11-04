package net.johnceo.sparklingmutuals.sync

import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.resources.*
import net.johnceo.sparklingmutuals.safari.structures.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncTrackerTest {
    @Test fun `shared nest resolutions never become local punches or new local exports`() {
        val survey=BeeNestSurvey()
        val first=PartySyncSites.nests.first()
        survey.observe(first,NestBlock.NEST)
        survey.acceptResolved(first)
        assertTrue(survey.remaining.isEmpty())
        assertTrue(survey.locallyResolved.isEmpty())
        assertEquals(0,survey.punchedCount)
        survey.acceptPartyClear()
        assertTrue(survey.locallyResolved.isEmpty())
        survey.reset();assertFalse(survey.allChecked)
    }
    @Test fun `shared objective completion updates source state without catches rewards or personal placement`() {
        val run=SafariRun(0)
        run.gems.shared(7,true)
        assertTrue(run.gems.opened);assertTrue(run.gems.locallyPlaced.isEmpty())
        assertEquals(0,run.count("Gemzie"));assertEquals(0,run.count("Wumpa"))
        val site=PartySyncSites.mounds.first()
        run.moundSurvey.scan(setOf(site)) { true }
        run.moundSurvey.sharedResolved(listOf(site))
        run.moundSurvey.scan(setOf(site)) { true }
        assertEquals(0,run.moundSurvey.remaining)
        assertEquals(0,run.count("Rockmite"))
    }
    @Test fun `shared feeder proof cannot clear local food obligations and is not reexported`() {
        val birds=SafariBirdLedger()
        birds.sharedUsage(intArrayOf(9,1,0,0,0,2))
        assertEquals(9,birds.accountedFeedings)
        assertEquals(0,birds.spawnEvents)
        assertEquals(0,birds.localFeederCount);assertEquals(2,birds.feederCount)
        val check=BirdFoodSparklingCheck()
        check.floor(0);check.inventory(listOf("Bag of Seeds" to 1),1000)
        check.population(9)
        assertFalse(check.complete)
        check.inventory(emptyList(),2000);check.population(9)
        assertTrue(check.complete)
    }
    @Test fun `canonical mask ordering is stable`() {
        assertEquals(Triple(-22,68,39),PartySyncSites.nests.first())
        assertEquals(Triple(-56,61,61),PartySyncSites.mounds.last())
        assertEquals(listOf(PartySyncSites.mounds[31]),PartySyncSites.resolved(Int.MIN_VALUE,PartySyncSites.mounds))
    }
    @Test fun `mound interaction boxes one block above or below share the canonical slot`() {
        val site = PartySyncSites.mounds.first()
        assertEquals(site, PartySyncSites.moundAt(site))
        assertEquals(site, PartySyncSites.moundAt(site.copy(second = site.second + 1)))
        assertEquals(site, PartySyncSites.moundAt(site.copy(second = site.second - 1)))
        assertEquals(site, PartySyncSites.moundAt(site.copy(first = site.first + 1)))
        assertNull(PartySyncSites.moundAt(site.copy(first = site.first - 2)))
    }
    @Test fun `mounds survive unload and only local break evidence is exported`() {
        val survey = MoundSurvey()
        val (local, peer) = PartySyncSites.mounds
        survey.scan(setOf(local, peer)) { false }
        survey.scan(emptySet()) { false }
        assertEquals(2, survey.remaining)
        survey.sharedResolved(listOf(peer))
        survey.scan(emptySet()) { it == local }
        assertTrue(survey.allBroken)
        assertEquals(setOf(local), survey.locallyResolved)
        survey.scan(setOf(local, peer)) { false }
        assertEquals(1, survey.remaining)
        survey.confirmBreak(local)
        survey.scan(setOf(local, peer)) { false }
        assertEquals(0, survey.remaining)
    }
}
