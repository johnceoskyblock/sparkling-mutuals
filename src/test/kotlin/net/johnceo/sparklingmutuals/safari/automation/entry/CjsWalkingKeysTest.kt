package net.johnceo.sparklingmutuals.safari.automation.entry

import net.minecraft.client.KeyMapping
import net.minecraft.client.ToggleKeyMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjsWalkingKeysTest {
    @Test fun `actual toggle mapping remains crouched across odd and even ticks and releases`() {
        val forward=KeyMapping("cj-test-forward",87,KeyMapping.Category.MOVEMENT)
        val sneak=ToggleKeyMapping("cj-test-sneak",340,KeyMapping.Category.MOVEMENT,{true},false)
        val keys=CjsWalkingKeys(forward,sneak,{true},{false},{false})
        repeat(4) { keys.drive(true,true);assertTrue(forward.isDown);assertTrue(sneak.isDown) }
        keys.drive(false,false);assertFalse(forward.isDown);assertFalse(sneak.isDown)
    }
    @Test fun `release restores the initial latch and preserves physical forward takeover`() {
        val forward=KeyMapping("cj-test-forward2",87,KeyMapping.Category.MOVEMENT)
        val sneak=ToggleKeyMapping("cj-test-sneak2",340,KeyMapping.Category.MOVEMENT,{true},false)
        sneak.setDown(true)
        val keys=CjsWalkingKeys(forward,sneak,{true},{true},{false})
        keys.drive(true,true);keys.drive(false,false)
        assertTrue(forward.isDown);assertTrue(sneak.isDown)
    }
    @Test fun `hold mapping primes sneak without forward and releases on cancellation`() {
        val forward=KeyMapping("cj-test-forward3",87,KeyMapping.Category.MOVEMENT)
        val sneak=ToggleKeyMapping("cj-test-sneak3",340,KeyMapping.Category.MOVEMENT,{false},false)
        val keys=CjsWalkingKeys(forward,sneak,{false},{false},{false})
        keys.drive(false,true);assertFalse(forward.isDown);assertTrue(sneak.isDown)
        keys.drive(false,false);assertFalse(forward.isDown);assertFalse(sneak.isDown)
    }
}
