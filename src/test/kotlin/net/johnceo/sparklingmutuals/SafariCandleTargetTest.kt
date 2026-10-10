package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.SafariCandleRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariCandleTargetTest {
    @Test fun `only single red candles on chiseled stone brick pedestals qualify`() {
        assertTrue(SafariCandleRules.target(true, 1, true))
        assertFalse(SafariCandleRules.target(false, 1, true), "Other candle colors retain their normal shape")
        for (count in 2..4) assertFalse(SafariCandleRules.target(true, count, true), "Grouped red candles retain their normal shape")
        assertFalse(SafariCandleRules.target(true, 1, false), "Single red decorative candles off pedestals retain their normal shape")
    }
}
