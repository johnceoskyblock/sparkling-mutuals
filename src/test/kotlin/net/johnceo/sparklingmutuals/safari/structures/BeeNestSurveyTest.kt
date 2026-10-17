package net.johnceo.sparklingmutuals.safari.structures

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BeeNestSurveyTest {
    private val low = Triple(-17, 68, 41)
    private val other = Triple(-22, 68, 39)
    private val high = Triple(16, 114, 87)
    private fun observe(survey: BeeNestSurvey, nests: Set<Triple<Int, Int, Int>>) =
        BeeNestSurvey.positions.forEach { survey.observe(it, if (it in nests) NestBlock.NEST else NestBlock.HIVE) }

    @Test fun `unloaded final location prevents completion after all known nests are clicked`() {
        val survey = BeeNestSurvey()
        observe(survey, setOf(low, other, high))
        survey.observe(high, NestBlock.UNKNOWN)
        assertEquals(3, survey.nestCount, "unloading cannot erase an initial nest")
        survey.punch(low); survey.punch(other)
        assertFalse(survey.allChecked)
        survey.observe(high, NestBlock.NEST)
        assertEquals(setOf(high), survey.remaining)
        survey.punch(high)
        assertTrue(survey.allChecked)
    }
    @Test fun `six actual nests remain required while beehives are ignored`() {
        val survey = BeeNestSurvey()
        val nests = setOf(low, other, high, Triple(25, 70, 41), Triple(-1, 81, 60), Triple(-1, 84, 9))
        observe(survey, nests)
        assertEquals(6, survey.remaining.size)
        assertFalse(survey.punch(Triple(20, 88, 33)))
        nests.toList().take(5).forEach { survey.punch(it) }
        assertFalse(survey.allChecked)
        survey.punch(nests.toList().last())
        assertTrue(survey.allChecked)
        assertEquals(6, survey.punchedCount)
    }
    @Test fun `repeated clicks count once and later scans cannot restore a punched marker`() {
        val survey = BeeNestSurvey()
        observe(survey, setOf(low, other, high))
        assertTrue(survey.punch(low))
        assertFalse(survey.punch(low))
        survey.observe(low, NestBlock.NEST)
        assertEquals(2, survey.remaining.size)
        assertEquals(1, survey.punchedCount)
        survey.observe(low, NestBlock.UNKNOWN)
        assertEquals(2, survey.remaining.size)
        survey.punch(other); survey.punch(high)
        assertTrue(survey.allChecked)
    }
    @Test fun `a survey retains validated locations across unloading and resets between runs`() {
        val survey = BeeNestSurvey()
        observe(survey, setOf(low, other, high))
        survey.punch(low); survey.punch(other); survey.punch(high)
        // No observation is delivered for unloaded blocks.
        assertTrue(survey.allChecked)
        survey.reset()
        assertEquals(0, survey.punchedCount)
        assertTrue(survey.remaining.isEmpty())
        assertFalse(survey.allChecked)
        assertFalse(survey.punch(low))
        assertFalse(survey.punch(Triple(0, 0, 0)))
    }
    @Test fun `all beehives or invalid blocks cannot prove nest completion`() {
        val survey = BeeNestSurvey()
        observe(survey, emptySet())
        assertFalse(survey.allChecked)
        observe(survey, setOf(low, other, high))
        survey.punch(low); survey.punch(other); survey.punch(high)
        survey.observe(Triple(20, 88, 33), NestBlock.UNKNOWN)
        assertTrue(survey.allChecked, "unloading cannot erase an already classified hive")
    }
}
