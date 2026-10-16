package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotFeedbackTest {
    @Test fun `escape is separate feedback and quoted party lines are rejected`() {
        assertEquals(ShotFeedbackKind.ESCAPE,ShotFeedback.parse("The Gemzie escaped your Critter Capsule!")!!.kind)
        assertNull(ShotFeedback.parse("Party > Farmer: You threw a Critter Capsule at the Gemzie!"))
        assertNull(ShotFeedback.parse("unrelated private message"))
    }
    @Test fun `known variant personal shared and startle remain distinct without raw text`() {
        assertEquals(ShotVariant.MASTERFUL,ShotFeedback.parse("You threw a Masterful Critter Capsule at the Gemzie!")!!.variant)
        assertEquals(ShotFeedbackKind.PERSONAL_CAPTURE,ShotFeedback.parse("CAPTURE! You caught a Gemzie!")!!.kind)
        assertEquals(ShotFeedbackKind.SHARED_CAPTURE,ShotFeedback.parse("LOOT SHARE! You received loot from PrivateName catching a Gemzie!")!!.kind)
        assertEquals(ShotFeedbackKind.STARTLE,ShotFeedback.parse("The Shyworm fled because it was startled!")!!.kind)
        assertNull(ShotFeedback.parse("CAPTURE! You caught a fictional monster!"))
    }
}
