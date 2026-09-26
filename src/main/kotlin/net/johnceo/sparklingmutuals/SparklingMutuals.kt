package net.johnceo.sparklingmutuals

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object SparklingMutuals : ModInitializer {

	const val MOD_ID: String = "sparkling-mutuals"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Sparkling Mutuals loaded!")

		ConfigManager.init(
			net.fabricmc.loader.api.FabricLoader.getInstance().configDir
		)

		MutualsCommand.register()
		TicketsCommand.register()
		MissingCommand.register()
		ApiKeyCommand.register()
		PartyManager.init()
		AlertManager.onInitialize()
		AlertCommand.register()
	}

	fun id(path: String): Identifier =
		Identifier.fromNamespaceAndPath(MOD_ID, path)
}