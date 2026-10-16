package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotByteBudgetTest {
    @Test fun `combined byte budget refuses one byte beyond limit and releases exact bytes`() {
        val budget=ShotByteBudget();assertTrue(budget.tryReserve(4*1024*1024));assertFalse(budget.tryReserve(1))
        budget.release(1);assertTrue(budget.tryReserve(1));assertEquals(4*1024*1024,budget.usedBytes)
    }
    @Test fun `negative reservation or releasing bytes without ownership cannot corrupt accounting`() {
        val budget=ShotByteBudget()
        assertThrows(IllegalArgumentException::class.java) { budget.tryReserve(-1) }
        assertThrows(IllegalArgumentException::class.java) { budget.release(1) }
        assertEquals(0,budget.usedBytes)
    }
}
