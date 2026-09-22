package net.johnceo.sparklingmutuals

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object ApiKeyCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommand(dispatcher, "apiKey")
            registerCommand(dispatcher, "apikey")
            registerCommand(dispatcher, "APIKEY")
        }
    }

    private fun registerCommand(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        commandName: String
    ) {
        dispatcher.register(
            ClientCommands.literal(commandName)
                .then(
                    ClientCommands.argument(
                        "key",
                        StringArgumentType.word()
                    )
                        .executes { context ->

                            val key =
                                StringArgumentType.getString(
                                    context,
                                    "key"
                                )

                            ConfigManager.apiKey = key
                            ConfigManager.save()

                            context.source.sendFeedback(
                                Component.literal(
                                    "Hypixel API key updated."
                                )
                            )

                            1
                        }
                )
        )
    }
}
