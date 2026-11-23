package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.DoomspiralLottery
import net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreview
import net.johnceo.sparklingmutuals.compat.*

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.config.ConfigStorage
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
import net.johnceo.sparklingmutuals.safari.automation.SafariDowntime
import net.johnceo.sparklingmutuals.safari.completion.SafariBiomeClearTracking
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.hideyho.HideyhoSpotter
import net.johnceo.sparklingmutuals.safari.resources.SafariInventoryAlerts
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.safari.structures.BeeNests
import net.johnceo.sparklingmutuals.safari.structures.SafariStructures
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/** Registers the mod's features and preserves the order of their lifecycle callbacks. */
object SparklingMutuals : ModInitializer {
    const val MOD_ID = "sparkling-mutuals"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        logger.info("Sparkling Mutuals loaded!")
        val configDirectory = ConfigStorage.migrate(FabricLoader.getInstance().configDir)
        ConfigManager.init(configDirectory, net.johnceo.sparklingmutuals.config.ModUpdates.installed) {
            net.johnceo.sparklingmutuals.config.PreviousModVersion.fromLogs(FabricLoader.getInstance().gameDir.resolve("logs"))
        }
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
    }

    private fun registerFeatures() {
        net.johnceo.sparklingmutuals.alerts.WarpListRanks.register()
        DoomspiralLottery.register()
        CapsulePreview.register()
        net.johnceo.sparklingmutuals.safari.SafariEntryCooldown.register()
        PartyManager.init()
        HideyhoSpotter.register()
        net.johnceo.sparklingmutuals.safari.StandardSafariKeys.register()
        SafariTracking.register()
        net.johnceo.sparklingmutuals.hud.HotspotAssignmentAlert.register()
        RunHistory.register()
        SparklingLog.register()
        TicketCounter.register()
        Achievements.register()
        PlaytimeReader.register()
        BeeNests.register()
        HoneyHives.register()
        ContestTracker.register()

        net.johnceo.sparklingmutuals.skyblock.SkillOverflow.register()
        net.johnceo.sparklingmutuals.skyblock.UbiksCubeTimer.register()
        net.johnceo.sparklingmutuals.skyblock.market.ShardSaleGuidance.register()
        SafariEsp.register()
        SafariIsland.register()
    }

    private fun registerCallbacks() {
        ClientTickEvents.END_CLIENT_TICK.register(::onClientTick)
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> resetSession() }
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            id("contest_hud")
        ) { graphics, deltaTracker ->
            if (DoomspiralLottery.active) return@attachElementBefore
            ContestHud.render(graphics, deltaTracker)
            SafariAssist.render(graphics)
            net.johnceo.sparklingmutuals.hud.HotspotHud.render(graphics)
            net.johnceo.sparklingmutuals.safari.SafariEntryCooldown.render(graphics)
            SafariTracking.render(graphics)
            net.johnceo.sparklingmutuals.skyblock.UbiksCubeTimer.render(graphics)
            net.johnceo.sparklingmutuals.debug.SafariDeveloper.render(graphics, Minecraft.getInstance())
            net.johnceo.sparklingmutuals.hud.HotspotAssignmentAlert.render(graphics)
            SparklingEncounters.render(graphics)
            SmallAlerts.render(graphics)
            if(Minecraft.getInstance().smScreen !is net.johnceo.sparklingmutuals.config.CatchCelebrationPreviewScreen)
                net.johnceo.sparklingmutuals.hud.SpecialCatchAlert.render(graphics)
            net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.render(graphics)
            net.johnceo.sparklingmutuals.hud.MaxWin.render(graphics)
        }
    }

    private fun onClientTick(client: Minecraft) {
        net.johnceo.sparklingmutuals.alerts.AutomatedCommands.tick(client)
        net.johnceo.sparklingmutuals.safari.SafariEntryCooldown.tick(client)
        net.johnceo.sparklingmutuals.skyblock.market.ShardSaleGuidance.tick(client)
        net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.tick(client)
        net.johnceo.sparklingmutuals.hud.SpecialCatchAlert.tick(client)
        net.johnceo.sparklingmutuals.hud.SafariBanners.tick(client)
        JoinWelcome.tick(client)
        PartyManager.onClientTick(client)
        PartyCommands.onClientTick(client)
        ContestTracker.onClientTick(client)
        HoneyHives.tick(client)

        SafariAssist.onClientTick(client)
        SafariTracking.onClientTick(client)
        net.johnceo.sparklingmutuals.skyblock.UbiksCubeTimer.tick(client)
        net.johnceo.sparklingmutuals.sync.SafariPartySync.tick(client)
        net.johnceo.sparklingmutuals.hud.HotspotAssignmentAlert.tick(client)
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
        CapsulePreview.tick(client)
        DoomspiralLottery.tick(client)
        if (!DoomspiralLottery.active) net.johnceo.sparklingmutuals.hud.SafariAlertSequence.tick()
        HideyhoSpotter.onClientTick(client)
        SafariDowntime.onClientTick(client)
    }

    private fun resetSession() {
        DoomspiralLottery.reset()
        CapsulePreview.reset()
        net.johnceo.sparklingmutuals.alerts.AutomatedCommands.reset()
        net.johnceo.sparklingmutuals.safari.SafariEntryCooldown.reset()
        HoneyHives.disconnect()
        net.johnceo.sparklingmutuals.hud.SafariAlertSequence.queue.reset()
        net.johnceo.sparklingmutuals.skyblock.market.ShardSaleGuidance.reset()
        net.johnceo.sparklingmutuals.sync.SafariPartySync.reset()
        net.johnceo.sparklingmutuals.skyblock.SkillOverflow.reset()
        net.johnceo.sparklingmutuals.skyblock.UbiksCubeTimer.reset()
        net.johnceo.sparklingmutuals.hud.SafariBanners.reset()
        net.johnceo.sparklingmutuals.hud.MaxWin.reset()
        net.johnceo.sparklingmutuals.safari.DoomspiralThrowTimer.reset()
        net.johnceo.sparklingmutuals.hud.SpecialCatchAlert.reset()
        JoinWelcome.reset()
        Achievements.resetAnnouncements()
        PhantomleafSolver.reset()
        PartyCommands.reset()
        PartyManager.reset()
        SafariSparklingMode.reset()
        net.johnceo.sparklingmutuals.hud.HotspotAssignmentAlert.reset()
        SafariAssist.reset()
        HideyhoSpotter.reset()
        SafariTracking.finish()
        SafariEsp.reset()
        SafariDowntime.transfer()
        net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput.disconnect()
        SafariIsland.reset()
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
