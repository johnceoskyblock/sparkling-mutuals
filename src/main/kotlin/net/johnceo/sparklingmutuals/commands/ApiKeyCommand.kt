package net.johnceo.sparklingmutuals.commands

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.network.chat.Component

object ApiKeyCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            listOf("apiKey", "apikey", "APIKEY").forEach { commandName ->
                dispatcher.register(
                    ClientCommands.literal(commandName)
                        .then(
                            ClientCommands.argument("key", StringArgumentType.word())
                                .executes {
                                    ConfigManager.apiKey = StringArgumentType.getString(it, "key")
                                    ConfigManager.save()
                                    it.source.sendFeedback(Component.literal("[SM] API key updated."))
                                    1
                                }
                        )
                )
            }
        }
    }
}