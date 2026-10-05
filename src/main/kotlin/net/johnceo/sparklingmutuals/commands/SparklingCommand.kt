package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.config.SafariConfigScreen
import net.minecraft.client.Minecraft

object SparklingCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("sparkling")
                    .executes {
                        Minecraft.getInstance().execute {
                            Minecraft.getInstance().setScreen(SafariConfigScreen())
                        }
                        1
                    }
                    .then(ClientCommands.literal("config").executes {
                        Minecraft.getInstance().execute {
                            Minecraft.getInstance().setScreen(SafariConfigScreen())
                        }
                        1
                    })
                    .then(
                        ClientCommands.literal("gui")
                            .executes {
                                val client = Minecraft.getInstance()

                                client.execute {
                                    client.setScreenAndShow(ContestGui())
                                }

                                1
                            }
                    )
            )
        }
    }
}
