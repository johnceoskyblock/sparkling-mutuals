package net.johnceo.sparklingmutuals

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object SparklingMutuals : ModInitializer {

	const val MOD_ID: String = "sparkling-mutuals"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Sparkling Mutuals loaded!")

		ConfigManager.init(
			net.fabricmc.loader.api.FabricLoader
				.getInstance()
				.configDir
		)

		ContestConfig.init(
			net.fabricmc.loader.api.FabricLoader
				.getInstance()
				.configDir
		)

		MutualsCommand.register()
		TicketsCommand.register()
		MissingCommand.register()
		ApiKeyCommand.register()
		SparklingCommand.register()
		PartyManager.init()

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			ContestTracker.onClientTick(client)
		}

		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(
				MOD_ID,
				"contest_hud"
			)
		) { graphics, deltaTracker ->
			ContestHud.render(
				graphics,
				deltaTracker
			)
		}

	}

	fun id(path: String): Identifier =
		Identifier.fromNamespaceAndPath(MOD_ID, path)
}