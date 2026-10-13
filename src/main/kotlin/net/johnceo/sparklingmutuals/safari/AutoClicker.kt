package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.mixin.SafariAttackKeyAccessor
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import org.lwjgl.glfw.GLFW

/** Poll physical Mouse 0 and use ordinary attacks only while aiming at a Rockmite mound. */
object AutoClicker {
    private val clock = SafariClickClock()
    private fun allowed(client: Minecraft): Boolean {
        val block = client.hitResult as? BlockHitResult
        val entity = (client.hitResult as? EntityHitResult)?.entity
        val held = client.mouseHandler.isLeftPressed && GLFW.glfwGetMouseButton(client.window.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
        return ClickConditions(ConfigManager.autoClicker, SafariAssist.biome == SafariBiome.CAVERN, held,
            client.level != null && client.player != null && client.gameMode != null && client.screen == null &&
                client.overlay == null && client.mouseHandler.isMouseGrabbed && client.isWindowActive,
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
