package net.johnceo.sparklingmutuals.skyblock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UbiksCubeTimerTest {
    @Test fun `reads Hypixel waits with omitted time units`() {
        val waits = mapOf("1h 23m 45s" to 5025L, "1h 23m" to 4980L, "2h" to 7200L,
            "12m 34s" to 754L, "12m" to 720L, "12s" to 12L, "0s" to 0L)
        waits.forEach { (text, seconds) ->
            assertEquals(seconds * 1000, UbiksCubeRules.reportedWait("§cSPLIT! You need to wait §e$text §cbefore you can play again."))
        }
        assertNull(UbiksCubeRules.reportedWait("SPLIT! You need to wait before you can play again."))
        assertNull(UbiksCubeRules.reportedWait("Party > Player: SPLIT! You need to wait 12s before you can play again."))
    }

    @Test fun `saved deadlines use wall time and stay active until the final millisecond`() {
        val deadline = 5_000L + UbiksCubeRules.COOLDOWN
        assertEquals(7200L, UbiksCubeRules.remaining(deadline, 5_000L))
        assertEquals(1L, UbiksCubeRules.remaining(deadline, deadline - 1))
        assertEquals(0L, UbiksCubeRules.remaining(deadline, deadline))
        assertEquals(0L, UbiksCubeRules.remaining(deadline, deadline + 30_000))
        assertEquals("1h 23m 45s", UbiksCubeRules.format(5025))
        assertEquals("2h", UbiksCubeRules.format(7200))
    }

    @Test fun `recognizes Hypixel profile changes independently of the profile API`() {
        assertEquals("pomegranate", UbiksCubeRules.profile("§aYou are playing on profile: §ePomegranate"))
        assertEquals("kiwi", UbiksCubeRules.profile("Your profile was changed to: Kiwi (Co-op)"))
        assertNull(UbiksCubeRules.profile("Party > Runner: You are playing on profile: Banana"))
    }
}
