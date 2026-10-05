package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.HideyhoPromptWindow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HideyhoPromptTest {
    @Test fun `world or connection changes clear dialogue before options arrive`() {
        val window = HideyhoPromptWindow()
        val world = Any()
        val connection = Any()
        window.observe("[MOB] Hideyho: How about it?", 100, world, connection)
        window.synchronize(Any(), connection)
        assertFalse(window.acceptsOptions(200))
        window.observe("[MOB] Hideyho: How about it?", 300, world, connection)
        window.synchronize(world, Any())
        assertFalse(window.acceptsOptions(400))
    }
    @Test fun `real MOB Hideyho dialogue arms options and another speaker cancels them`() {
        val window = HideyhoPromptWindow()
        window.observe("[MOB] Hideyho: §fHow about it?", 100)
        assertTrue(window.acceptsOptions(200))
        window.observe("[MOB] Other Critter: Hello!", 300)
        assertFalse(window.acceptsOptions(300))
    }
    @Test fun `options require recent Hideyho dialogue`() {
        val window = HideyhoPromptWindow()
        assertFalse(window.acceptsOptions(100))
        window.observe("Party > Farmer: Hideyho", 100)
        assertFalse(window.acceptsOptions(100))
        window.observe("[NPC] Hideyho: Want to play again?", 100)
        assertTrue(window.acceptsOptions(200))
        assertFalse(window.acceptsOptions(15101))
    }
    @Test fun `another NPC cancels the Hideyho window`() {
        val window = HideyhoPromptWindow()
        window.observe("[NPC] Hideyho: Play again?", 100)
        window.observe("[NPC] Trader: Buy a shard?", 200)
        assertFalse(window.acceptsOptions(200))
        window.observe("[NPC] Hideyho: Play again?", 300)
        window.reset()
        assertFalse(window.acceptsOptions(300))
    }
}
