package net.johnceo.sparklingmutuals

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.alerts.AlertManager
import net.johnceo.sparklingmutuals.commands.AlertCommand
import net.johnceo.sparklingmutuals.commands.ApiKeyCommand
import net.johnceo.sparklingmutuals.commands.PartyCommands
import net.johnceo.sparklingmutuals.commands.SparklingCommand
import net.johnceo.sparklingmutuals.commands.SparklingLookup
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.contest.ContestHud
import net.johnceo.sparklingmutuals.contest.ContestTracker
import net.johnceo.sparklingmutuals.history.Achievements
import net.johnceo.sparklingmutuals.history.OwnStats
import net.johnceo.sparklingmutuals.history.PlaytimeReader
import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.history.SparklingLog
import net.johnceo.sparklingmutuals.history.TicketCounter
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import net.johnceo.sparklingmutuals.honeyhives.HoneyHives
import net.johnceo.sparklingmutuals.misc.phantomleaf.PhantomleafSolver
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.automation.AutoClicker
import net.johnceo.sparklingmutuals.safari.automation.SafariDowntime
import net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard
import net.johnceo.sparklingmutuals.safari.completion.SafariBiomeClearTracking
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.esp.TeammateEsp
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoQuest
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoSpotter
import net.johnceo.sparklingmutuals.safari.resources.SafariInventoryAlerts
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.safari.structures.BeeNests
import net.johnceo.sparklingmutuals.safari.structures.SafariStructures
import net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreview
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/** Registers the mod's features and preserves the order of their lifecycle callbacks. */
object SparklingMutuals : ModInitializer {
    const val MOD_ID = "sparkling-mutuals"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        logger.info("Sparkling Mutuals loaded!")
        val configDirectory = FabricLoader.getInstance().configDir
        ConfigManager.init(configDirectory)
        ContestConfig.init(configDirectory)
        registerCommands()
        registerFeatures()
        registerCallbacks()
    }

    private fun registerCommands() {
        PartyCommands.register()
        ApiKeyCommand.register()
        SparklingCommand.register()
        SparklingLookup.register()
        net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.register()
        AlertCommand.register()
        net.johnceo.sparklingmutuals.alerts.SparkleAutoParty.register()
    }

    private fun registerFeatures() {
        PartyManager.init()
        HideyhoQuest.register()
        HideyhoSpotter.register()
        SafariTracking.register()
        AlertManager.onInitialize()
        RunHistory.register()
        SparklingLog.register()
        TicketCounter.register()
        Achievements.register()
        PlaytimeReader.register()
        BeeNests.register()
        HoneyHives.register()
        PhantomleafSolver.register()
        SafariEsp.register()
        SafariAssist.register()
        TeammateEsp.register()
        SafariIsland.register()
        SafariManagerGuard.register()
        CapsulePreview.register()
    }

    private fun registerCallbacks() {
        ClientTickEvents.START_CLIENT_TICK.register(AutoClicker::tick)
        ClientTickEvents.END_CLIENT_TICK.register(::onClientTick)
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> resetSession() }
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            id("contest_hud")
        ) { graphics, deltaTracker ->
            ContestHud.render(graphics, deltaTracker)
            SafariAssist.render(graphics)
            SafariTracking.render(graphics)
            SparklingEncounters.render(graphics)
            SmallAlerts.render(graphics)
            net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.render(graphics)
            net.johnceo.sparklingmutuals.hud.MaxWin.render(graphics)
        }
    }

    private fun onClientTick(client: Minecraft) {
        net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.tick(client)
        JoinWelcome.tick(client)
        PartyManager.onClientTick(client)
        net.johnceo.sparklingmutuals.alerts.SparkleAutoParty.tick(client)
        PartyCommands.onClientTick(client)
        ContestTracker.onClientTick(client)
        HoneyHives.tick(client)
        PhantomleafSolver.tick(client)
        SafariAssist.onClientTick(client)
        net.johnceo.sparklingmutuals.safari.ClearWater.tick(client)
        SafariTracking.onClientTick(client)
        RunHistory.tick(client)
        Achievements.tick(client)
        OwnStats.tick(client)
        PlaytimeReader.tick(client)
        LogImporter.tick(client)
        SafariSparklingMode.tick(client)
        BeeNests.tick(client)
        SafariStructures.tick(client)
        SafariEsp.tick(client)
        SafariBiomeClearTracking.tick(client)
        SafariInventoryAlerts.tick(client)
        net.johnceo.sparklingmutuals.hud.MaxWin.tick(client)
        HideyhoQuest.onClientTick(client)
        HideyhoSpotter.onClientTick(client)
        SafariDowntime.onClientTick(client)
        SafariManagerGuard.onClientTick(client)
        net.johnceo.sparklingmutuals.safari.automation.SafariTicketSelect.tick(client)
        CapsulePreview.tick(client)
    }

    private fun resetSession() {
        net.johnceo.sparklingmutuals.safari.ClearWater.reset()
        net.johnceo.sparklingmutuals.hud.MaxWin.reset()
        net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.reset()
        JoinWelcome.reset()
        PhantomleafSolver.reset()
        CapsulePreview.reset()
        PartyCommands.reset()
        PartyManager.reset()
        SafariSparklingMode.reset()
        AlertManager.cancelPendingAlert()
        SafariAssist.reset()
        HideyhoQuest.reset()
        HideyhoSpotter.reset()
        SafariTracking.finish()
        SafariEsp.reset()
        AutoClicker.reset()
        SafariManagerGuard.reset()
        SafariDowntime.transfer()
        net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput.disconnect()
        SafariIsland.reset()
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
