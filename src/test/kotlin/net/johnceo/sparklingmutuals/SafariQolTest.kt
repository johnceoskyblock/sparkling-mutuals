package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Properties

class SafariQolTest {
    @TempDir lateinit var dir: Path
    @Test fun `new installations use the unique mode ESP preset with biome restrictions`() {
        ConfigManager.init(dir)
        assertTrue(SafariEspConfig.groups.values.all { it.enabled && it.onlyInBiome })
        assertEquals(SafariRoster.all.map { it.name }.toSet(), SafariEspConfig.mobs.filterValues { it.enabled }.keys)
    }
    @Test fun `ESP upgrades retain explicit user preferences`() {
        SafariEspConfig.load(Properties().apply {
            setProperty("esp.forest.enabled", "false")
            setProperty("esp.cavern.onlyInBiome", "false")
            setProperty("esp.wumpa.enabled", "true")
            setProperty("esp.rockmite.enabled", "false")
        })
        assertFalse(SafariEspConfig.groups.getValue("forest").enabled)
        assertFalse(SafariEspConfig.groups.getValue("cavern").onlyInBiome)
        assertTrue(SafariEspConfig.mobs.getValue("Wumpa").enabled)
        assertFalse(SafariEspConfig.mobs.getValue("Rockmite").enabled)
    }
    @Test fun `capsule and chat settings apply and survive a restart with safe distance bounds`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        assertFalse(ConfigManager.hideCaptureChat)
        assertTrue(ConfigManager.hideGroundCapsules)
        assertTrue(ConfigManager.hideFlyingCapsules)
        assertEquals(2f, ConfigManager.capsuleHideDistance)
        val settings = SafariSettings()
        settings.safari.hideCaptureChat = true
        settings.safari.hideGroundCapsules = false
        settings.safari.hideFlyingCapsules = false
        settings.safari.capsuleHideDistance = 4.5f
        settings.apply()
        ConfigManager.init(dir)
        assertTrue(ConfigManager.hideCaptureChat)
        assertFalse(ConfigManager.hideGroundCapsules)
        assertFalse(ConfigManager.hideFlyingCapsules)
        assertEquals(4.5f, ConfigManager.capsuleHideDistance)
        assertEquals(2f, ConfigManager.validCapsuleDistance(Float.NaN))
        assertEquals(2f, ConfigManager.validCapsuleDistance(Float.POSITIVE_INFINITY))
        assertEquals(.5f, ConfigManager.validCapsuleDistance(-1f))
        assertEquals(6f, ConfigManager.validCapsuleDistance(100f))
    }
    @Test fun `capture spam filtering handles formatting and every species without hiding player chat or PB notices`() {
        val spam = listOf("§aYou threw a Critter Capsule!", "§aCAPTURE! You caught a Wumpa!",
            "LOOT SHARE! You received a Doomspiral Shard from Other catching a Doomspiral!") +
            SafariRoster.all.map { "The ${it.name} escaped!" }
        spam.forEach {
            assertTrue(SafariChatFilter.hidden(it, true), it)
            assertFalse(SafariChatFilter.hidden(it, false), it)
        }
        listOf("Party > [VIP] Test: CAPTURE! You caught a Wumpa!",
            "Guild > Test: You threw a capsule", "[SM] New Doomspiral PB: 1:00.000!",
            "Party > Test: !pb doom", "The unknown creature escaped!",
            "Party > Test: quoted\nCAPTURE! You caught a Wumpa!").forEach {
            assertFalse(SafariChatFilter.hidden(it, true), it)
        }
    }
    @Test fun `hidden captures still feed raw counts and only own catches feed personal bests`() {
        val run = SafariRun(1000)
        val bests = SafariPersonalBests()
        val messages = listOf("CAPTURE! You caught a Wumpa!",
            "LOOT SHARE! You received a Doomspiral Shard from Other catching a Doomspiral!",
            "CAPTURE! You caught a Doomspiral!")
        val notices = messages.flatMap { raw ->
            // The display decision never changes the message that trackers receive.
            assertTrue(SafariChatFilter.hidden(raw, true))
            SafariMessages.lines(raw).mapNotNull { line ->
                SafariCatch.parse(line)?.let(run::record)
                bests.newBest(line, run, 62000)
            }
        }
        assertEquals(1, run.count("Wumpa"))
        assertEquals(2, run.count("Doomspiral"))
        assertEquals(61000L, bests.time("Wumpa"))
        assertEquals(61000L, bests.time("Doomspiral"))
        assertEquals(2, notices.size)
        notices.forEach { assertFalse(SafariChatFilter.hidden(it, true)) }
    }
    @Test fun `capsules hide only in Safari with independent toggles and an inclusive camera distance`() {
        fun hide(id: String?, flying: Boolean = true, distanceSquared: Double = 4.0,
            ground: Boolean = true, air: Boolean = true, safari: Boolean = true) =
            CritterCapsuleRules.hidden(safari, flying, id, distanceSquared, ground, air, 2f)
        for (id in listOf("CRITTER_CAPSULE", "MASTERFUL_CRITTER_CAPSULE")) {
            assertTrue(hide(id))
            assertTrue(hide(id, distanceSquared = 0.0))
            assertFalse(hide(id, distanceSquared = 4.0001))
            assertFalse(hide(id, air = false))
            assertFalse(hide(id, safari = false))
        }
        assertTrue(hide("CRITTER_CAPSULE", flying = false, distanceSquared = 10000.0, air = false))
        assertFalse(hide("CRITTER_CAPSULE", flying = false, ground = false))
        assertFalse(hide("MASTERFUL_CRITTER_CAPSULE", flying = false))
        assertFalse(hide("STRING"))
        assertFalse(hide(null))
    }
}
