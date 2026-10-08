package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.config.SafariConfigScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.johnceo.sparklingmutuals.safari.CatchCountScreen
import net.johnceo.sparklingmutuals.safari.SafariFullClear
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariEsp
import net.minecraft.network.chat.Component

object SparklingCommand {
    private fun mode(source: FabricClientCommandSource, selected: SafariMode): Int {
        SafariFullClear.select(selected)
        SafariFullClear.modeLines().forEach { source.sendFeedback(Component.literal(it)) }
        return 1
    }
    private fun open(screen: () -> Screen): Int {
        val client = Minecraft.getInstance()
        client.execute { client.setScreenAndShow(screen()) }
        return 1
    }

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("sparkling")
                    .executes { open(::SafariConfigScreen) }
                    .then(ClientCommands.literal("config").executes { open(::SafariConfigScreen) })
                    .then(ClientCommands.literal("gui").executes { open { ContestGui() } })
                    .then(ClientCommands.literal("debug").executes { context ->
                        SafariEsp.debug(Minecraft.getInstance())
                        context.source.sendFeedback(Component.literal("[SM] Nearby critter diagnostics saved to latest.log."))
                        1
                    })
            )
            dispatcher.register(ClientCommands.literal("captures").executes { open { CatchCountScreen() } })
            dispatcher.register(ClientCommands.literal("unique").executes { mode(it.source, SafariMode.UNIQUE) })
            dispatcher.register(ClientCommands.literal("sparkle").executes { mode(it.source, SafariMode.SPARKLING) })
            dispatcher.register(ClientCommands.literal("fc").executes { mode(it.source, SafariMode.FULL_CLEAR) })
        }
    }
}
