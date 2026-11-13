package net.johnceo.sparklingmutuals.contest

import net.johnceo.sparklingmutuals.hud.HudThemes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContestHudColorsTest {
    @Test fun `high brackets color text and earned ticket borders without intermediate bracket events`() {
        for((tier,color) in mapOf("UNCOMMON" to 0xFF55FF55,"MYTHIC" to 0xFFFF55FF,
            "DIVINE" to 0xFF55FFFF,"SPECIAL" to 0xFFFF5555)) {
            assertEquals(color.toInt(),HudThemes.tierColor(tier),tier)
            assertEquals(color.toInt(),HudThemes.ticketColor(true,false,tier),tier)
        }
        assertEquals(0xFFFF55FF.toInt(),HudThemes.tierColor(" Mythic "))
    }
}
