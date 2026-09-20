package net.johnceo.sparklingmutuals

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object ApiKeyCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommand(dispatcher)
        }
    }

    private fun registerCommand(
        dispatcher: CommandDispatcher<FabricClientCommandSource>
    ) {
        dispatcher.register(
            ClientCommands.literal("apiKey")
                .then(
                    ClientCommands.argument(
                        "key",
                        com.mojang.brigadier.arguments.StringArgumentType.word()
                    )
                        .executes { context ->
                            val key = com.mojang.brigadier.arguments.StringArgumentType.getString(
                                context,
                                "key"
                            )

                            ConfigManager.apiKey = key
                            ConfigManager.save()

                            context.source.sendFeedback(
                                Component.literal("Hypixel API key updated.")
                            )

                            1
                        }
                )
        )
    }
}