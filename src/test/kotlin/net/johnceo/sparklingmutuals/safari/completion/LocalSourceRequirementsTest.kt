package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.structures.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalSourceRequirementsTest {
    private fun party()=PartySparklingState().apply {
        select(setOf("me"));accept(setOf("me"),mapOf("me" to emptySet()))
    }
    @Test fun `late Forest entry trusts fd for nest work while early clearer keeps render requirements`() {
        for(late in listOf(false,true)) {
            val run=SafariRun(0)
            run.visitBiome(SafariBiome.FOREST,if(late) 40000 else 1000)
            run.updateNestEvidence(false,0,3,0)
            run.partyFullClear("Party > Friend: fd")
            run.updateNestEvidence(false,0,3,0)
            assertEquals(late,run.trustLateForestNestReport)
            assertEquals(!late,run.nestCounterNeeded(SafariMode.SPARKLING,party()))
            assertEquals(!late,run.nestCounterNeeded(SafariMode.FULL_CLEAR,party()))
            assertEquals(0,run.count("Honeybug"))
        }
        val survey=BeeNestSurvey()
        survey.observe(BeeNestSurvey.positions.first(),NestBlock.NEST)
        survey.acceptPartyClear()
        survey.observe(BeeNestSurvey.positions.last(),NestBlock.NEST)
        assertTrue(survey.allChecked);assertTrue(survey.remaining.isEmpty());assertEquals(2,survey.punchedCount)
    }
    @Test fun `coin counter reaches zero from floor and own inventory despite reported party coins`() {
        val check=GimmiegoldSparklingCheck()
        check.floor(1,true);check.inventory(1)
        check.contributions(0,"Me",mapOf("Friend" to 9))
        assertEquals(2,check.leftToUse)
        check.floor(1,false);check.inventory(0)
        assertEquals(0,check.leftToUse)
        assertFalse(check.complete,"capture and Sparkling population checks are still independent")
    }
}
