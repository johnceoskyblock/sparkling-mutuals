package net.johnceo.sparklingmutuals.safari

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.minecraft.client.gui.screens.InBedChatScreen

internal object GazerBedShortcut {
    fun register() {
        ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
            if (screen is InBedChatScreen) ScreenMouseEvents.allowMouseClick(screen).register click@{ _, event ->
                if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !ConfigManager.leftClickGazerBed ||
                    client.smScreen !== screen || !SafariAssist.visualsActive(client)) return@click true
                val player = client.player?.takeIf { it.isSleeping } ?: return@click true
                val bed = player.sleepingPos.orElseGet(player::blockPosition)
                if (SafariEspRules.biomeAt(bed.x.toDouble(), bed.z.toDouble()) != SafariBiome.HAUNTED) return@click true

                screen.onClose()
                false
            }
        }
    }
}
