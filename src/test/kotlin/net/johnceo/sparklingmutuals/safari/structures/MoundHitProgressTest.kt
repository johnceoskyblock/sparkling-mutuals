package net.johnceo.sparklingmutuals.safari.structures

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MoundHitProgressTest {
    @Test fun serverConfirmedStagesAdvanceOnlyTheAttackedMoundUntilItBreaks() {
        val survey = MoundSurvey()
        val mound = Triple(-80, 57, 61)
        val other = Triple(-95, 43, 24)
        survey.scan(setOf(mound, other)) { true }
        assertEquals("(0/5)", survey.hitLabel(mound))
        val messages = listOf(
            "Small cracks begin to form in the mound...",
            "The cracks seem to be getting larger, keep hitting it!",
            "Chunks of the mound begin falling away...",
            "The mound is about to fall to pieces! Keep going!",
            "The mound fell apart, revealing a Rockmite hidden inside!"
        )
        messages.forEachIndexed { index, message ->
            val time = 1000L + index * 200
            survey.attack(mound, time)
            val stage = SafariMounds.hitStage(message)!!
            assertEquals(mound, survey.confirmHit(stage, time + 50))
            if (index < 4) {
                assertEquals(mound, survey.confirmHit(stage, time + 51))
                assertEquals("(${index + 1}/5)", survey.hitLabel(mound))
            }
            assertEquals("(0/5)", survey.hitLabel(other))
        }
        assertTrue(survey.confirmedBroken(mound))
        assertNull(survey.hitLabel(mound))
    }

    @Test fun ambiguousOrStaleAttacksCannotAssignProgress() {
        val survey = MoundSurvey()
        val first = Triple(0, 0, 0)
        val second = Triple(1, 1, 1)
        survey.attack(first, 1000)
        survey.attack(second, 1100)
        assertNull(survey.confirmHit(1, 1200))
        assertNull(survey.confirmHit(2, 4000))
        assertEquals("(0/5)", survey.hitLabel(first))
        assertEquals("(0/5)", survey.hitLabel(second))
    }
}
