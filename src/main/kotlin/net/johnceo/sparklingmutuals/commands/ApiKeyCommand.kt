package net.johnceo.sparklingmutuals.commands

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager

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
                                    it.source.sendFeedback(LocalChat.component("[SM] API key updated."))
                                    1
                                }
                        )
                )
            }
        }
    }
}