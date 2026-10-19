package net.johnceo.sparklingmutuals.misc.phantomleaf

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PhantomleafStateTest {
    @Test fun `two stationary notes find one block at mutation height including negative coordinates`() {
        val state = PhantomleafState()
        state.chat("[CROP] Phantomleaf: Poof! Try and find me!")
        assertTrue(state.searching)
        state.sound("minecraft:block.note_block.basedrum", .615f, 1f, -2.5, 10.5)
        assertTrue(state.candidates.isEmpty())
        state.sound("minecraft:block.note_block.basedrum", .615f, 1f, -2.5, 10.5)
        assertEquals(listOf(PhantomleafState.Candidate(-3, 74, 10)), state.candidates)
    }
    @Test fun `ambiguous distance requires moving and two fresh notes before recalculation`() {
        val state = PhantomleafState()
        state.chat("Phantomleaf: Poof! Try and find me!")
        val sound = "minecraft:block.note_block.basedrum"
        state.sound(sound, .615f, 29f / 30f, .5, .5)
        assertEquals(PhantomleafState.Hint.MULTIPLE, state.sound(sound, .615f, 29f / 30f, .5, .5))
        assertEquals(setOf(PhantomleafState.Candidate(-1, z = 0), PhantomleafState.Candidate(1, z = 0),
            PhantomleafState.Candidate(0, z = -1), PhantomleafState.Candidate(0, z = 1)), state.candidates.toSet())
        state.sound(sound, .615f, 1f, 1.5, .5)
        assertEquals(4, state.candidates.size)
        state.sound(sound, .615f, 1f, 1.5, .5)
        assertEquals(listOf(PhantomleafState.Candidate(1, z = 0)), state.candidates)
    }
    @Test fun `unrelated sounds are ignored and ending or world reset clears the stationary sample`() {
        val state = PhantomleafState()
        val sound = "minecraft:block.note_block.basedrum"
        repeat(2) { state.sound(sound, .615f, 1f, .5, .5) }
        assertTrue(state.candidates.isEmpty())
        for (ending in listOf("Phantomleaf: You found me!", "Phantomleaf: That's not me! Better luck next time!")) {
            state.chat("Phantomleaf: Poof! Try and find me!")
            repeat(2) {
                state.sound("minecraft:block.note_block.harp", .615f, 1f, .5, .5)
                state.sound(sound, .8f, 1f, .5, .5)
                state.sound(sound, .615f, Float.NaN, .5, .5)
            }
            assertTrue(state.candidates.isEmpty())
            repeat(2) { state.sound(sound, .615f, 1f, .5, .5) }
            assertEquals(1, state.candidates.size)
            state.chat(ending)
            assertFalse(state.searching)
            assertTrue(state.candidates.isEmpty())
        }
        state.chat("Phantomleaf: Poof! Try and find me!")
        repeat(2) { state.sound(sound, .615f, 1f, .5, .5) }
        state.reset()
        state.chat("Phantomleaf: Poof! Try and find me!")
        state.sound(sound, .615f, 1f, .5, .5)
        assertTrue(state.candidates.isEmpty())
    }
    @Test fun `impossible distance yields a hint without a false waypoint`() {
        val state = PhantomleafState()
        state.chat("Phantomleaf: Poof! Try and find me!")
        repeat(2) {
            val hint = state.sound("minecraft:block.note_block.basedrum", .615f, .5f, .7, .8)
            if (it == 1) assertEquals(PhantomleafState.Hint.NONE_FOUND, hint)
        }
        assertTrue(state.candidates.isEmpty())
    }
    @Test fun `solver defaults on and its new category toggle persists`(@TempDir dir: Path) {
        val old = ConfigManager.phantomleafSolver
        try {
            ConfigManager.init(dir)
            assertTrue(ConfigManager.phantomleafSolver)
            val settings = SafariSettings()
            assertTrue(settings.miscEndgame.phantomleafSolver)
            settings.miscEndgame.phantomleafSolver = false
            settings.apply()
            ConfigManager.init(dir)
            assertFalse(ConfigManager.phantomleafSolver)
            assertFalse(SafariSettings().miscEndgame.phantomleafSolver)
        } finally { ConfigManager.phantomleafSolver = old }
    }
}
