/*
 * Adapted from SkyHanni PhantomleafSolver.kt, commit b3543887dfb80d95ff9880b3f610167ac2c104f2.
 * Copyright SkyHanni contributors. Licensed under LGPL-2.1; see licenses/SkyHanni-LGPL-2.1.txt.
 * Changes: standalone state, Fabric lifecycle/sound hook, native settings and world rendering.
 */
package net.johnceo.sparklingmutuals.misc.phantomleaf

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.world.phys.AABB

/** Garden-only Phantomleaf helper. No clicking, crop interaction or party messages. */
object PhantomleafSolver {
    private val state = PhantomleafState()
    private var world: Any? = null
    private val fill by lazy { WorldRenderCompat.renderType("phantomleaf_fill", filled = true) }
    private val lines by lazy { WorldRenderCompat.renderType("phantomleaf_lines") }
    private var beamsBroken = false
    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) {
                val client = Minecraft.getInstance()
                client.execute { if (active(client)) state.chat(SafariRules.strip(message.string)) }
            }
        }
        WorldRenderCompat.register(::render)
        LevelRenderEvents.COLLECT_SUBMITS.register(::beams)
    }
    fun tick(client: Minecraft) { active(client) }
    private fun active(client: Minecraft): Boolean {
        if (world !== client.level) { state.reset(); world = client.level; beamsBroken = false }
        val enabled = ConfigManager.phantomleafSolver && client.player != null && client.level != null &&
            SafariIsland.currentMode.equals("garden", true)
        if (!enabled) state.reset()
        return enabled
    }
    fun sound(name: String, pitch: Float, volume: Float) {
        if (!ConfigManager.phantomleafSolver || !SafariIsland.currentMode.equals("garden", true)) return
        val client = Minecraft.getInstance()
        val sourceWorld = client.level
        client.execute {
            if (client.level !== sourceWorld || !active(client)) return@execute
            val player = client.player ?: return@execute
            val hint = state.sound(name, pitch, volume, player.x, player.z)
            val text = when (hint) {
                PhantomleafState.Hint.NONE_FOUND -> "No solutions found. Try moving a little."
                PhantomleafState.Hint.MULTIPLE -> "Multiple candidates found. Move a little to recalculate."
                null -> return@execute
            }
            player.sendSystemMessage(LocalChat.component("[SM] Phantomleaf: $text"))
        }
    }
    private fun color(size: Int) = if (size == 1) 0xFF00FF00.toInt() else 0xFFFFFF00.toInt()
    private fun render(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        if (!active(client) || !state.searching || client.options.smHideGui) return
        val candidates = state.candidates
        if (candidates.isEmpty()) return
        val color = color(candidates.size)
        for (pos in candidates) {
            WorldRenderCompat.tile(context, fill, pos.x.toDouble(), pos.y + .01, pos.z.toDouble(),
                (color and 0xFFFFFF) or 0x60000000)
            WorldRenderCompat.box(context, lines,
                AABB(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 1.0, pos.z + 1.0), color)
            if (beamsBroken) WorldRenderCompat.box(context, lines,
                AABB(pos.x + .4, pos.y + 1.0, pos.z + .4, pos.x + .6, pos.y + 64.0, pos.z + .6), color)
        }
        WorldRenderCompat.flush(context, fill)
        WorldRenderCompat.flush(context, lines)
    }
    private fun beams(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        if (!active(client) || !state.searching || client.options.smHideGui || beamsBroken) return
        val candidates = state.candidates
        if (candidates.isEmpty()) return
        val level = client.level ?: return
        val camera = client.gameRenderer.smCamera.position()
        val animation = Math.floorMod(level.gameTime, 40L) + client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val poses = context.poseStack()
        try {
            for (pos in candidates) {
                poses.pushPose()
                try {
                    poses.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)
                    BeaconRenderer.submitBeaconBeam(poses, context.submitNodeCollector(), BeaconRenderer.BEAM_LOCATION, 1f,
                        animation, 0, BeaconRenderer.MAX_RENDER_Y, color(candidates.size) and 0xFFFFFF,
                        BeaconRenderer.SOLID_BEAM_RADIUS, BeaconRenderer.BEAM_GLOW_RADIUS)
                } finally { poses.popPose() }
            }
        } catch (error: LinkageError) {
            beamsBroken = true
        }
    }
    fun reset() { state.reset(); world = null; beamsBroken = false }
}
