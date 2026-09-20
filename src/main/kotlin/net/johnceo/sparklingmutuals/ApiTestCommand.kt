package net.johnceo.sparklingmutuals

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component

object ApiTestCommand {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommand(dispatcher)
        }
    }

    private fun registerCommand(
        dispatcher: CommandDispatcher<FabricClientCommandSource>
    ) {
        dispatcher.register(
            ClientCommands.literal("apitest")
                .executes { context ->

                    val uuid = "980b4a33-8f13-495f-822d-0b2e59ebcbd6"

                    val profileUuid = HypixelApi.getCurrentProfileUuid(uuid)

                    if (profileUuid == null) {
                        context.source.sendFeedback(
                            Component.literal("Failed to find current profile.")
                        )
                        return@executes 0
                    }

                    context.source.sendFeedback(
                        Component.literal(
                            "Current profile UUID: $profileUuid"
                        )
                    )

                    val sparklingCritters =
                        HypixelApi.getSparklingCritters(uuid, profileUuid)

                    if (sparklingCritters == null) {
                        context.source.sendFeedback(
                            Component.literal("Failed to retrieve sparkling critters.")
                        )
                        return@executes 0
                    }

                    context.source.sendFeedback(
                        Component.literal(
                            "Found ${sparklingCritters.size} sparkling critters."
                        )
                    )

                    1
                }
        )
    }
}