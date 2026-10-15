package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.mixin.SafariAttackKeyAccessor
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult

/** Poll physical Mouse 0 and use ordinary attacks only while aiming at a Rockmite mound. */
object AutoClicker {
    private val clock = SafariClickClock()
    private fun allowed(client: Minecraft): Boolean {
        val block = client.hitResult as? BlockHitResult
        val entity = (client.hitResult as? EntityHitResult)?.entity
        val held = client.mouseHandler.isLeftPressed && physicalLeftMouseDown(client)
        return ClickConditions(ConfigManager.autoClicker, SafariAssist.biome == SafariBiome.CAVERN, held,
            client.level != null && client.player != null && client.gameMode != null && client.smScreen == null &&
                client.smOverlay == null && client.mouseHandler.isMouseGrabbed && client.isWindowActive,
            client.player?.isUsingItem == true, client.gameMode?.isDestroying == true,
            block != null && client.level?.getBlockState(block.blockPos)?.isAir == false,
            entity != null && SafariEsp.isRockmiteMound(entity, client)).allowed()
    }
    fun tick(client: Minecraft) { if (!allowed(client)) clock.tick(false) }
    @JvmStatic fun prepare(client: Minecraft) {
        val active = allowed(client)
        val click = clock.tick(active)
        if (!active) return
        // Consume physical presses so they cannot add an extra attack to the fixed cadence.
        while (client.options.keyAttack.consumeClick()) { }
        if (click) (client as SafariAttackKeyAccessor).sparklingAttack()
    }
    fun reset() { clock.tick(false) }
}
