package net.johnceo.sparklingmutuals.safari.integration

import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariChatFilter
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.SafariPersonalBests
import net.johnceo.sparklingmutuals.safari.esp.CritterCapsuleRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

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
        assertTrue(ConfigManager.hideCaptureChat)
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
    @Test fun `routine critter interactions hide only system lines when enabled`() {
        val messages = listOf(
        "The Shyworm saw a player and fled!",
        "The Shyworm surfaced nearby! Don't let it see you!",
        "The Shyworm hid back into the ground.",
        "The Shyworm fled because it was startled!",
        "You ran headfirst into the wall. Some more cracks appeared in it...",
        "Keep running into the wall! More cracks are forming!",
        "You destroyed the wall! Maybe something was hiding behind it...",
        "The mound fell apart, revealing a Rockmite hidden inside!",
        "The mound falls apart, but nothing is inside...",
        "The Chuckwalla dodged your critter capsule and started running away!",
        "The Chuckwalla is cornered! Catch it!",
        "Your Critter Capsule can't get through Scrappy's armor!",
        "This particular Scrappy doesn't seem to care for this type of food...",
        "Looks like the hive is empty now...",
        "You found a Duplico that was disguised as a block!",
        "You used your Icebreaker to shatter the ice and free the Troodon trapped within!",
        "The Scrappy cautiously started eating the Flavor-packed Fish.",
        "The Scrappy reaches out its paw for more Flavor-packed Fish.",
        "The Scrappy ate the Flavor-packed Fish and came out of its shell!",
        "The Scrappy cautiously started eating the Wholesale Wheat.",
        "The Scrappy reaches out its paw for more Wholesale Wheat.",
        "The Scrappy ate the Wholesale Wheat and came out of its shell!",
        "The Scrappy cautiously started eating the Lush Lily Pad.",
        "The Scrappy reaches out its paw for more Lush Lily Pad.",
        "The Scrappy ate the Lush Lily Pad and came out of its shell!")
        messages.forEach { message ->
            assertTrue(SafariChatFilter.hidden("\u00a7a$message", true), message)
            assertFalse(SafariChatFilter.hidden(message, false), message)
            assertFalse(SafariChatFilter.hidden("Party > Test: $message", true), message)
        }
        assertFalse(SafariChatFilter.hidden("SPARKLING Scrappy!", true))
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
        assertEquals(61000L, bests.time("Wumpa", duo = true))
        assertEquals(61000L, bests.time("Doomspiral", duo = true))
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
