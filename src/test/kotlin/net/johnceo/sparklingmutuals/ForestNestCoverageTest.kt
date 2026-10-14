package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ForestNestCoverageTest {
    private val nests = setOf(Triple(-17, 68, 41), Triple(-13, 103, 21), Triple(16, 114, 87))
    private fun blocks() = BeeNestSurvey.positions.associateWith { if (it in nests) NestBlock.NEST else NestBlock.HIVE }
    @Test fun `loaded blocks at every elevation are read without a player proximity condition`() {
        val survey = BeeNestSurvey()
        val data = blocks()
        survey.scan { data[it] }
        assertEquals(0, survey.unverified.size)
        assertEquals(nests, survey.remaining)
        assertEquals(3, survey.waypoints(true).count { it.nest })
        assertFalse(survey.allChecked)
        nests.forEach { survey.punch(it) }
        assertTrue(survey.allChecked)
        assertTrue(survey.waypoints(true).isEmpty())
    }
    @Test fun `unavailable sites have guidance but never gain survey coverage by a visit`() {
        val survey = BeeNestSurvey()
        repeat(10) { survey.scan { null } }
        assertEquals(13, survey.unverified.size)
        assertEquals(13, survey.waypoints(true).size)
        assertTrue(survey.waypoints(true).none { it.nest })
        assertFalse(survey.allChecked)
        val data = blocks()
        val upper = Triple(16, 114, 87)
        survey.scan { if (it == upper) null else data[it] }
        survey.punch(Triple(-17, 68, 41)); survey.punch(Triple(-13, 103, 21))
        assertEquals(setOf(upper), survey.unverified)
        assertEquals(listOf(NestWaypoint(upper, false)), survey.waypoints(true))
        survey.scan { data[it] }
        assertEquals(listOf(NestWaypoint(upper, true)), survey.waypoints(true))
        assertFalse(survey.allChecked)
        survey.punch(upper)
        assertTrue(survey.allChecked)
    }
    @Test fun `unloading retains survey and remaining markers while visibility never changes tracking`() {
        val survey = BeeNestSurvey()
        val data = blocks()
        survey.scan { data[it] }
        assertTrue(survey.waypoints(false).isEmpty())
        survey.scan { null }
        assertEquals(nests, survey.remaining)
        assertEquals(0, survey.unverified.size)
        nests.forEach { survey.punch(it) }
        survey.scan { if (it in nests) NestBlock.UNKNOWN else data[it] }
        assertTrue(survey.allChecked)
        survey.reset()
        assertEquals(13, survey.unverified.size)
        assertEquals(0, survey.punchedCount)
        assertFalse(survey.allChecked)
    }
    @Test fun `invalid block data revokes coverage instead of proving an empty site`() {
        val survey = BeeNestSurvey()
        survey.scan { blocks()[it] }
        nests.forEach { survey.punch(it) }
        assertTrue(survey.allChecked)
        survey.scan { if (it == Triple(20, 88, 33)) NestBlock.UNKNOWN else null }
        assertEquals(setOf(Triple(20, 88, 33)), survey.unverified)
        assertFalse(survey.allChecked)
    }
}
