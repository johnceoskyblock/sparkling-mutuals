package net.johnceo.sparklingmutuals.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.alerts.AlertManager

object AlertCommand {

    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("warping")
                    .executes {
                        val enabled = AlertManager.toggleAlert()
                        it.source.sendFeedback(
                            LocalChat.component("[SM] Sparkle warping ${if (enabled) "on" else "off"}.")
                        )
                        1
                    }
            )

            dispatcher.register(
                ClientCommands.literal("warpingdelay")
                    .then(
                        ClientCommands.argument(
                            "seconds",
                            IntegerArgumentType.integer(1, 86400)
                        ).executes {
                            val seconds = it.getArgument("seconds", Int::class.java)
                            AlertManager.setDelay(seconds)
                            it.source.sendFeedback(
                                LocalChat.component("[SM] Warping delay: ${seconds}s.")
                            )
                            1
                        }
                    )
            )
        }
    }
}