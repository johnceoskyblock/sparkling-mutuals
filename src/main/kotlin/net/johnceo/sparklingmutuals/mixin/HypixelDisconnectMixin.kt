package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.alerts.HypixelRecovery
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.network.DisconnectionDetails
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(ClientCommonPacketListenerImpl::class)
abstract class HypixelDisconnectMixin {
    @Shadow @Final @JvmField protected var serverData: ServerData? = null
    @Shadow @JvmField protected var isTransferring: Boolean = false

    @Inject(method = ["createDisconnectScreen"], at = [At("RETURN")])
    private fun scheduleReconnect(details: DisconnectionDetails, callback: CallbackInfoReturnable<Screen>) {
        if (isTransferring) return
        val screen = callback.returnValue as? DisconnectedScreen ?: return
        HypixelRecovery.disconnected(serverData, screen, details.reason())
    }
}
