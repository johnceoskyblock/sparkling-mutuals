package net.johnceo.sparklingmutuals.safari.visibility

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VisibleCritterHighlightPolicyTest {
    @Test fun `partly obstructed glow has a box fallback`() {
        assertTrue(VisibleCritterHighlightPolicy.box(false, true, true, false))
        assertFalse(VisibleCritterHighlightPolicy.box(false, true, true, true))
    }
    @Test fun `models without glow support still have a highlight`() {
        assertTrue(VisibleCritterHighlightPolicy.box(false, true, false, true))
    }
    @Test fun `disabled highlights stay disabled and explicit boxes remain available`() {
        assertFalse(VisibleCritterHighlightPolicy.box(false, false, true, false))
        assertTrue(VisibleCritterHighlightPolicy.box(true, false, true, true))
    }
}
