package net.johnceo.sparklingmutuals

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.alerts.AlertManager
import net.johnceo.sparklingmutuals.commands.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.contest.ContestHud
import net.johnceo.sparklingmutuals.contest.ContestTracker
import net.johnceo.sparklingmutuals.party.PartyManager
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object SparklingMutuals : ModInitializer {

	const val MOD_ID = "sparkling-mutuals"

	private val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		logger.info("Sparkling Mutuals loaded!")

		val configDirectory = FabricLoader.getInstance().configDir

		ConfigManager.init(configDirectory)
		ContestConfig.init(configDirectory)

		MutualsCommand.register()
		TicketsCommand.register()
		MissingCommand.register()
		ApiKeyCommand.register()
		SparklingCommand.register()
		AlertCommand.register()

		PartyManager.init()
		AlertManager.onInitialize()

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			ContestTracker.onClientTick(client)
		}

		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			id("contest_hud")
		) { graphics, deltaTracker ->
			ContestHud.render(graphics, deltaTracker)
		}
	}

	fun id(path: String): Identifier =
		Identifier.fromNamespaceAndPath(MOD_ID, path)
}