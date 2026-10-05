package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.mixin.SafariAttackKeyAccessor
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.BlockHitResult

/** Fixed Mouse 0 activation; queues the normal attack action rather than sending custom packets. */
object AutoClicker {
    private val clock = SafariClickClock()
    private fun allowed(client: Minecraft): Boolean {
        val block = client.hitResult as? BlockHitResult
        return ClickConditions(ConfigManager.autoClicker, SafariAssist.inSafari, client.mouseHandler.isLeftPressed,
            client.level != null && client.player != null && client.gameMode != null && client.screen == null &&
                client.overlay == null && client.mouseHandler.isMouseGrabbed && client.isWindowActive,
            client.player?.isUsingItem == true, client.gameMode?.isDestroying == true,
            block != null && client.level?.getBlockState(block.blockPos)?.isAir == false).allowed()
    }
    fun tick(client: Minecraft) { if (!allowed(client)) clock.tick(false) }
    @JvmStatic fun prepare(client: Minecraft) {
        val active = allowed(client)
        val click = clock.tick(active)
        if (!active) return
        // Consume physical presses so they cannot add an extra attack to the fixed cadence.
        while (client.options.keyAttack.consumeClick()) { }
        if (click) (client.options.keyAttack as SafariAttackKeyAccessor).sparklingQueueAttack(1)
    }
    fun reset() { clock.tick(false) }
}
