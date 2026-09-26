package net.johnceo.sparklingmutuals

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object AlertCommand {

    @Volatile
    private var registered = false

    fun register() {
        if (registered) return
        registered = true
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerToggleCommand(dispatcher)
            registerSetDelayCommand(dispatcher)
        }
    }

    private fun registerToggleCommand(
        dispatcher: CommandDispatcher<FabricClientCommandSource>
    ) {
        dispatcher.register(
            ClientCommands.literal("alert")
                .executes { context ->
                    val newState = AlertManager.toggleAlert()
                    context.source.sendFeedback(
                        Component.literal(
                            "Alerts ${if (newState) "enabled" else "disabled"}"
                        )
                    )
                    1
                }
        )
    }

    private fun registerSetDelayCommand(
        dispatcher: CommandDispatcher<FabricClientCommandSource>
    ) {
        dispatcher.register(
            ClientCommands.literal("alertdelay")
                .then(
                    ClientCommands.argument(
                        "seconds",
                        IntegerArgumentType.integer(1, 86400)
                    )
                        .executes { context ->
                            val seconds = context.getArgument("seconds", Int::class.java)
                            AlertManager.setDelay(seconds)
                            context.source.sendFeedback(
                                Component.literal(
                                    "Alert delay set to ${seconds}s"
                                )
                            )
                            1
                        }
                )
        )
    }
}