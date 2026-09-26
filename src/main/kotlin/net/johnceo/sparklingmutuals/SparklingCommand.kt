package net.johnceo.sparklingmutuals

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft

object SparklingCommand {

    fun register() {

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            registerCommand(dispatcher)

        }
    }

    private fun registerCommand(
        dispatcher: CommandDispatcher<FabricClientCommandSource>
    ) {
        dispatcher.register(
            ClientCommands.literal("sparkling")
                .then(
                    ClientCommands.literal("gui")
                        .executes {

                            val client = Minecraft.getInstance()

                            client.execute {
                                client.setScreenAndShow(
                                    ContestGuiScreen()
                                )
                            }

                            1
                        }
                )
        )
    }
}