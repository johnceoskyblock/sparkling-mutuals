package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.config.ConfigManager
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
            if (net.johnceo.sparklingmutuals.debug.SafariDeveloper.enabled) {
                dispatcher.register(ClientCommands.literal("testing").executes {
                    val message = runCatching {
                        net.johnceo.sparklingmutuals.debug.SafariDeveloper.toggleTesting(Minecraft.getInstance())
                    }.getOrElse { error ->
                        org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/testing").warn("Could not start testing log", error)
                        "[SM] Could not start testing log. See latest.log."
                    }
                    it.source.sendFeedback(LocalChat.component(message))
                    1
                })
            }
            val historyTabs = mapOf(
                HistoryScreen.Tab.BESTS to listOf("best", "bests"),
                HistoryScreen.Tab.SPARKLINGS to listOf("sparkling", "sparklings"),
                HistoryScreen.Tab.STATS to listOf("stat", "stats"),
                HistoryScreen.Tab.PROFIT to listOf("profit", "profits"),
                HistoryScreen.Tab.ACHIEVEMENTS to listOf("achievements")
            )
            val historyShortcuts = ClientCommands.literal("sm")
            historyTabs.forEach { (tab, aliases) ->
                aliases.forEach { alias -> historyShortcuts.then(ClientCommands.literal(alias).executes {
                    open { HistoryScreen(null, tab) }
                }) }
            }
            dispatcher.register(ClientCommands.literal("newsession").executes { context ->
                net.johnceo.sparklingmutuals.history.RunHistory.newSession().fold(
                    { context.source.sendFeedback(LocalChat.component("[SM] Previous session saved. New session started.")); 1 },
                    { context.source.sendError(LocalChat.component("[SM] Could not save the new session. See latest.log.")); 0 }
                )
            })
            dispatcher.register(
                historyShortcuts
                    .executes { open(::SafariConfigScreen) }
                    .then(ClientCommands.literal("config").executes { open(::SafariConfigScreen) })
                    .then(ClientCommands.literal("help").executes { context ->
                        CommandHelp.localLines().forEach { context.source.sendFeedback(LocalChat.component(it)) }; 1
                    })
                    .then(ClientCommands.literal("gui").executes { open { ContestGui() } })
                    .then(ClientCommands.literal("history")
                        .then(ClientCommands.literal("import")
                            .then(ClientCommands.argument("folder", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes {
                                LogImporter.runImport(java.nio.file.Path.of(
                                    com.mojang.brigadier.arguments.StringArgumentType.getString(it, "folder").replace("\"", ""))); 1 })
                            .executes { LogImporter.runImport(LogImporter.defaultLogs()); 1 })
                        .executes { open { HistoryScreen(null) } })
                    .then(ClientCommands.literal("debug").executes { context ->
                        SafariEsp.debug(Minecraft.getInstance())
                        net.johnceo.sparklingmutuals.safari.automation.entry.CjsSafariEntry.debug().forEach {
                            org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/cj-entry-dryrun").info(it)
                        }
                        net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard.debug().forEach {
                            org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/entry-guard").info(it)
                        }
                        BeeNests.debug(Minecraft.getInstance())
                        HoneyHives.debug(Minecraft.getInstance())
                        val message = if (net.johnceo.sparklingmutuals.debug.SafariDeveloper.enabled) {
                            runCatching { net.johnceo.sparklingmutuals.debug.SafariDeveloper.snapshot(Minecraft.getInstance()) }
                                .fold({
                                    net.johnceo.sparklingmutuals.debug.SafariDeveloper.event("Manual snapshot · ${it.fileName}")
                                    "[SM] Developer snapshot saved to $it"
                                }, {
                                    org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/debug").warn("Could not write developer snapshot", it)
                                    "[SM] Could not save developer snapshot. See latest.log."
                                })
                        } else "[SM] Entity, nest and honeyhive diagnostics saved to latest.log."
                        context.source.sendFeedback(LocalChat.component(message))
                        1
                    })
            )
            dispatcher.register(ClientCommands.literal("hotspot").executes {
                ConfigManager.hotspotAnnouncer = !ConfigManager.hotspotAnnouncer
                ConfigManager.save()
                it.source.sendFeedback(LocalChat.component("[SM] Hotspot assigning " + if (ConfigManager.hotspotAnnouncer) "on." else "off."))
                if (ConfigManager.hotspotAnnouncer)
                    it.source.sendFeedback(LocalChat.component("[SM] Hotspot parties work best when all party members are using Sparkling Mutuals for the hotspot assignments"))
                1
            })
            dispatcher.register(ClientCommands.literal("rates").executes { SparklingRates.show(it.source) })
            for (alias in listOf("profit", "profits")) {
                val command = ClientCommands.literal(alias).executes { ProfitableCritters.show(it.source) }
                for (ticket in net.johnceo.sparklingmutuals.safari.SafariTicketType.entries) {
                    val name = if (ticket == net.johnceo.sparklingmutuals.safari.SafariTicketType.FIRST_CLASS) "first-class" else ticket.name.lowercase()
                    command.then(ClientCommands.literal(name).executes { ProfitableCritters.show(it.source, ticket) })
                }
                dispatcher.register(command)
            }
            dispatcher.register(ClientCommands.literal("captures").executes { open { CatchCountScreen() } })
            dispatcher.register(ClientCommands.literal("unique").executes { mode(it.source, SafariMode.UNIQUE) })
            dispatcher.register(ClientCommands.literal("sparkle").executes { mode(it.source, SafariMode.SPARKLING) })
            dispatcher.register(ClientCommands.literal("fc").executes { mode(it.source, SafariMode.FULL_CLEAR) })
            dispatcher.register(ClientCommands.literal("fullclear").executes { mode(it.source, SafariMode.FULL_CLEAR) })
            dispatcher.register(ClientCommands.literal("full").then(
                ClientCommands.literal("clear").executes { mode(it.source, SafariMode.FULL_CLEAR) }))
        }
    }
}
