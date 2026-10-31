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
            dispatcher.register(ClientCommands.literal("start").executes {
                val client=net.minecraft.client.Minecraft.getInstance()
                when {
                    !net.johnceo.sparklingmutuals.JoinWelcomeState.hypixel(client.currentServer?.ip) -> {
                        it.source.sendError(LocalChat.component("[SM] Start sparkle warping while connected to Hypixel."));0
                    }
                    net.johnceo.sparklingmutuals.config.ConfigManager.leechMode -> {
                        it.source.sendError(LocalChat.component("[SM] Turn /leech off before starting a leader cycle."));0
                    }
                    net.johnceo.sparklingmutuals.party.PartyManager.hasInfo && !net.johnceo.sparklingmutuals.alerts.SparkleWarping.canLead(client) -> {
                        it.source.sendError(LocalChat.component("[SM] Only the party leader can start the cycle."));0
                    }
                    else -> {
                        AlertManager.startCycle()
                        it.source.sendFeedback(LocalChat.component("[SM] Starting sparkle warping: saving your party and preparing it for the next run."));1
                    }
                }
            })
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
