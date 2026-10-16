package net.johnceo.sparklingmutuals.safari.esp

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.party.PartyManager
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/** Noamm-style outlines and distance-scaled names, using SkyBlock party UUIDs rather than dungeon classes. */
object TeammateEsp {
    const val COLOR = 0xFF55FF55.toInt()
    fun isTeammate(entity: Entity): Boolean {
        val client = Minecraft.getInstance()
        return entity is Player && !entity.isRemoved && client.level?.getEntity(entity.id) === entity &&
            TeammateEspRules.eligible(SkyblockSidebar.inSkyblock(client), PartyManager.hasInfo,
                entity.uuid, client.player?.uuid, PartyManager.getMembers())
    }
    fun register() {
        WorldRenderCompat.register { context ->
            if (!ConfigManager.showTeammateNames) return@register
            val client = Minecraft.getInstance()
            val world = client.level ?: return@register
            if (!PartyManager.hasInfo || !SkyblockSidebar.inSkyblock(client)) return@register
            val camera = client.gameRenderer.smCamera.position()
                val delta = client.deltaTracker.getGameTimeDeltaPartialTick(false)
            for (player in world.players()) {
                if (!isTeammate(player)) continue
                val pos = player.getPosition(delta)
                val distance = pos.distanceTo(camera)
                val scale = (SafariEspConfig.labelWorldScale * TeammateEspRules.nameScale(distance)).toFloat()
                val text = player.gameProfile.name
                WorldRenderCompat.label(context, text, COLOR, pos.x, pos.y + player.bbHeight + .7 + distance * .015, pos.z, scale)
            }
        }
    }
}
