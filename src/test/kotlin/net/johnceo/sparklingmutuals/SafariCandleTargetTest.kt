package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.SafariCandleRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariCandleTargetTest {
    @Test fun `red candles qualify regardless of candle count or pedestal`() {
        assertTrue(SafariCandleRules.target(true), "Red candles qualify without consulting count or pedestal")
        assertFalse(SafariCandleRules.target(false), "White and other decorative candles retain their normal shape")
    }
}
