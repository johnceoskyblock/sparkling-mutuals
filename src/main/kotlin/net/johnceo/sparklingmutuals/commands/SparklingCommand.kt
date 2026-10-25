package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.SafariConfigScreen
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import net.johnceo.sparklingmutuals.history.ui.HistoryScreen
import net.johnceo.sparklingmutuals.honeyhives.HoneyHives
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.structures.BeeNests
import net.johnceo.sparklingmutuals.safari.ui.CatchCountScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

object SparklingCommand {
    private fun mode(source: FabricClientCommandSource, selected: SafariMode): Int {
        SafariFullClear.select(selected)
        SafariFullClear.modeLines().forEach { source.sendFeedback(LocalChat.component(it)) }
        return 1
    }
    private fun open(screen: () -> Screen): Int {
        val client = Minecraft.getInstance()
        client.execute { client.setScreenAndShow(screen()) }
        return 1
    }

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("sm")
                    .apply { ShotCommands.register(this) }
                    .executes { open(::SafariConfigScreen) }
                    .then(ClientCommands.literal("config").executes { open(::SafariConfigScreen) })
                    .then(ClientCommands.literal("gui").executes { open { ContestGui() } })
                    .then(ClientCommands.literal("history")
                        .then(ClientCommands.literal("import")
                            .then(ClientCommands.argument("folder", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes {
                                LogImporter.runImport(java.nio.file.Path.of(
                                    com.mojang.brigadier.arguments.StringArgumentType.getString(it, "folder").replace("\"", ""))); 1 })
                            .executes { LogImporter.runImport(LogImporter.defaultLogs()); 1 })
                        .executes { open { HistoryScreen(null) } })
                    .then(ClientCommands.literal("achievements").executes {
                        open { HistoryScreen(null, HistoryScreen.Tab.ACHIEVEMENTS) } })
                    .then(ClientCommands.literal("stats").executes {
                        open { HistoryScreen(null, HistoryScreen.Tab.STATS) } })
                    .then(ClientCommands.literal("debug").executes { context ->
                        SafariEsp.debug(Minecraft.getInstance())
                        net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard.debug().forEach {
                            org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/entry-guard").info(it)
                        }
                        BeeNests.debug(Minecraft.getInstance())
                        HoneyHives.debug(Minecraft.getInstance())
                        context.source.sendFeedback(LocalChat.component("[SM] Entity, nest and honeyhive diagnostics saved to latest.log."))
                        1
                    })
            )
            dispatcher.register(ClientCommands.literal("rates").executes { SparklingRates.show(it.source) })
            dispatcher.register(ClientCommands.literal("profitablecritters").executes { ProfitableCritters.show(it.source) })
            dispatcher.register(ClientCommands.literal("captures").executes { open { CatchCountScreen() } })
            dispatcher.register(ClientCommands.literal("unique").executes { mode(it.source, SafariMode.UNIQUE) })
            dispatcher.register(ClientCommands.literal("sparkle").executes { mode(it.source, SafariMode.SPARKLING) })
            dispatcher.register(ClientCommands.literal("fc").executes { mode(it.source, SafariMode.FULL_CLEAR) })
        }
    }
}
