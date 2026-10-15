package net.johnceo.sparklingmutuals.safari.structures

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ForestNestHeightTest {
    @Test fun `survey reads hive blocks below the reported oak fences and highlights actual nests`() {
        // John's live diagnostics found fences at each old position; actual site blocks are Y - 1.
        val sites = listOf(Triple(-17, 68, 41), Triple(-22, 68, 39), Triple(25, 70, 41),
            Triple(23, 70, 46), Triple(-1, 81, 60), Triple(-9, 81, 58), Triple(-13, 103, 21),
            Triple(-11, 103, 13), Triple(-1, 84, 9), Triple(20, 88, 33), Triple(15, 88, 31),
            Triple(18, 114, 79), Triple(16, 114, 87))
        val nests = setOf(Triple(-17, 68, 41), Triple(-13, 103, 21), Triple(16, 114, 87))
        val loadedBlocks = sites.associateWith { if (it in nests) NestBlock.NEST else NestBlock.HIVE }
        val survey = BeeNestSurvey()
        survey.scan { loadedBlocks[it] ?: NestBlock.UNKNOWN }
        assertEquals(0, survey.unverified.size)
        assertEquals(nests, survey.remaining)
        assertEquals(3, survey.waypoints(true).count { it.nest })
        assertFalse(survey.punch(Triple(-17, 69, 41)))
        assertTrue(survey.punch(Triple(-17, 68, 41)))
        assertEquals(2, survey.waypoints(true).size)
        survey.punch(Triple(-13, 103, 21)); survey.punch(Triple(16, 114, 87))
        assertTrue(survey.allChecked)
        assertTrue(survey.waypoints(true).isEmpty())
    }
}
