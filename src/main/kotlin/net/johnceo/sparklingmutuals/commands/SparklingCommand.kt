package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.config.SafariConfigScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.johnceo.sparklingmutuals.safari.CatchCountScreen
import net.johnceo.sparklingmutuals.safari.SafariFullClear
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.network.chat.Component

object SparklingCommand {
    private fun open(screen: () -> Screen): Int {
        val client = Minecraft.getInstance()
        client.execute { client.setScreenAndShow(screen()) }
        return 1
    }

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommands.literal("pb").then(ClientCommands.literal("toggle").executes { context ->
                val type = ConfigManager.personalBests.toggleBiomeType()
                ConfigManager.save()
                context.source.sendFeedback(Component.literal("[SM] Biome PB replies: ${type.label}. Both PB types are tracked separately."))
                1
            }))
            dispatcher.register(
                ClientCommands.literal("sparkling")
                    .executes { open(::SafariConfigScreen) }
                    .then(ClientCommands.literal("full").then(ClientCommands.literal("clear").executes { context ->
                        val enabled = SafariFullClear.toggle()
                        ConfigManager.save()
                        context.source.sendFeedback(Component.literal("[SM] Full clear mode: ${if (enabled) "on" else "off"}."))
                        1
                    }))
                    .then(ClientCommands.literal("timesave").executes { context ->
                        ConfigManager.timesaveOnly = !ConfigManager.timesaveOnly
                        ConfigManager.save()
                        context.source.sendFeedback(Component.literal("[SM] Timesave mode: ${if (ConfigManager.timesaveOnly) "on" else "off"}."))
                        1
                    })
                    .then(ClientCommands.literal("config").executes { open(::SafariConfigScreen) })
                    .then(ClientCommands.literal("gui").executes { open { ContestGui() } })
                    .then(ClientCommands.literal("catches").executes { open { CatchCountScreen() } })
            )
        }
    }
}
