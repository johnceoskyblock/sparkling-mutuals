package net.johnceo.sparklingmutuals.mixin

import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.DisconnectionDetails
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(DisconnectedScreen::class)
interface DisconnectedScreenAccessor {
    @Accessor("parent") fun recoveryParent(): Screen
    @Accessor("details") fun recoveryDetails(): DisconnectionDetails
}
