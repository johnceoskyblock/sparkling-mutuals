package net.johnceo.sparklingmutuals.api

import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.history.AchievementTier
import net.johnceo.sparklingmutuals.history.Achievements
import net.johnceo.sparklingmutuals.history.Measure
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AchievementApiTest {
    private fun achievement(id: String) = Achievements.definitions.first { it.id == id }

    @Test fun `runs come from the once-a-run bestiary counts and catches from all 37`() {
        val member = JsonParser.parseString("""{"safari":{"discovered_sparkling_critters":["GAZER"]},
            "bestiary":{"kills":{"doomspiral_1":379,"wumpa_1":372,"hideyho_1":368,"cavernfish_1":1000,"mantis_shrimp_1":20}}}""").asJsonObject
        val profile = SafariProfile.from(member)
        assertEquals(379L, profile.runs)
        assertEquals(379L + 372 + 368 + 1000 + 20, profile.catches)
        assertEquals(listOf("Gazer"), profile.caught)
    }
    @Test fun `a profile without bestiary data leaves runs and catches unknown`() {
        val profile = SafariProfile.from(JsonParser.parseString("""{"safari":{"discovered_sparkling_critters":[]}}""").asJsonObject)
        assertNull(profile.runs)
        assertNull(profile.catches)
    }
    @Test fun `sparkle runs are re-tiered and a lone hat trick is a novelty`() {
        assertEquals(AchievementTier.RARE, achievement("party_double").tier)
        assertEquals(AchievementTier.LEGENDARY, achievement("party_triple").tier)
        assertEquals(AchievementTier.SPECIAL, achievement("own_triple").tier)
        assertFalse(AchievementTier.SPECIAL.fanfare)
    }
    @Test fun `raw unique streak criteria are archived and observed Macaw absence stops at 8`() {
        assertTrue(Achievements.definitions.none { it.id.startsWith("no_unique_runs_") })
        assertEquals(3, Achievements.definitions.count { it.measure == Measure.MODELED_UNIQUE })
        assertEquals(listOf(3L, 5L, 8L), Achievements.definitions.filter { it.measure == Measure.RUNS_SINCE_MACAW }.map { it.goal })
        assertTrue(Achievements.definitions.none { it.id.startsWith("emblem") })
        assertEquals(Achievements.definitions, Achievements.all())
    }
}
