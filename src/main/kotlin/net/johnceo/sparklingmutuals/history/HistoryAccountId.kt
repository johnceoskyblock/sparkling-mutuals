package net.johnceo.sparklingmutuals.history

import java.time.LocalDate
import java.util.Locale
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

internal object HistoryAccountId {
    private val uuid = Regex("[0-9a-f]{32}")
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/history")
    private var lastMarker: Triple<String, String, LocalDate>? = null

    fun normalize(value: String) = value.trim().replace("-", "").lowercase(Locale.ROOT).takeIf(uuid::matches)

    fun active(client: Minecraft): String? =
        client.user.profileId.takeIf { client.currentServer != null && client.connection != null && client.player?.uuid == it }
            ?.toString()?.let(::normalize)

    fun tick(client: Minecraft) {
        val account = active(client) ?: return
        val name = client.user.name
        if (ConfigManager.historyOwnerUuid.isEmpty()) {
            ConfigManager.historyOwnerUuid = account
            ConfigManager.saveSettings()
        }
        val marker = Triple(account, name, LocalDate.now())
        if (lastMarker != marker) {
            logger.info("[SM History] Account {} {}", account, name)
            lastMarker = marker
        }
        if (account == ConfigManager.historyOwnerUuid) {
            RunHistory.claimLegacy(account)
            SparklingLog.claimLegacy(account)
        }
    }

    fun selected(client: Minecraft): String? = active(client)?.takeIf { it == ConfigManager.historyOwnerUuid }
}
