package net.johnceo.sparklingmutuals.safari

import java.util.Properties
import net.johnceo.sparklingmutuals.config.SafariChatCategory
import net.johnceo.sparklingmutuals.config.SafariChatConfig
import net.johnceo.sparklingmutuals.config.SafariChatToggleSettings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariChatToggleTest {
    @Test fun `each SafariUtils category is independently selectable without filtering player chat`() {
        val examples = listOf(
            "HEAD START! You started with a Wriggleworm!", "You threw a Critter Capsule!",
            "CAPTURE! You caught a Wumpa!", "LOOT SHARE! You received a shard!",
            "FLOOR DROP! You found Lime Gem on the ground!", "[NPC] Safari Manager: Have fun!",
            "[NPC] Hunter Billy: Hey there!", "You cannot drop items yet!",
            "You placed the Lime Gem on its podium!", "The Chuckwalla is cornered! Catch it!",
            "Small cracks begin to form in the mound...", "The Scrappy happily eats the Wholesale Wheat.",
            "The Shyworm hid back into the ground.", "Keep running into the wall! More cracks are forming!",
            "The cave is collapsing...", "You used your Icebreaker to shatter the ice and free the Troodon trapped within!",
            "BRRR! It's getting really cold! But you've got to keep moving...", "Something stirs in the Haunted Biome...",
            "You startled the Bloodbat!", "You found a Duplico that was disguised as a block!",
            "You wake up suddenly...", "A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!",
            "[MOB] Hideyho: Hello!", "A Bluebird was attracted to the Birdfeeder!", "Looks like the hive is empty now...")
        assertEquals(SafariChatCategory.entries.size, examples.size)
        examples.zip(SafariChatCategory.entries).forEach { (message, category) ->
            assertEquals(category, SafariChatFilter.category(message), message)
            assertTrue(SafariChatFilter.hidden(message, true) { it == category }, message)
            assertFalse(SafariChatFilter.hidden(message, true) { it != category }, message)
            assertFalse(SafariChatFilter.hidden(message, false) { true }, message)
            assertFalse(SafariChatFilter.hidden("Party > Player: $message", true) { true }, message)
        }
        assertFalse(SafariChatFilter.hidden("SPARKLING! Player caught a SPARKLING Macaw!", true) { true })
    }

    @Test fun `chat settings keep existing defaults and persist independent choices`() {
        SafariChatConfig.load(Properties())
        try {
            assertTrue(SafariChatConfig.hidden(SafariChatCategory.CATCH_ATTEMPTS))
            assertFalse(SafariChatConfig.hidden(SafariChatCategory.WUMPA))
            val settings = SafariChatToggleSettings()
            settings.catch_attempts = false; settings.wumpa = true
            assertTrue(settings.apply())
            val saved = Properties().also(SafariChatConfig::save)
            SafariChatConfig.load(saved)
            assertFalse(SafariChatConfig.hidden(SafariChatCategory.CATCH_ATTEMPTS))
            assertTrue(SafariChatConfig.hidden(SafariChatCategory.WUMPA))
        } finally { SafariChatConfig.load(Properties()) }
    }
}
