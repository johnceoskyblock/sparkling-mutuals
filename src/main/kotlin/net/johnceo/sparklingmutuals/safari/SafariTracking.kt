package net.johnceo.sparklingmutuals.safari

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** One message ledger shared by all Safari panels; shard amounts never multiply captures. */
object SafariTracking {
    val ledger = SafariLedger()
    private var level: Any? = null
    private var awayTicks = 0
    private var lastCatch = 0L
    private val entry = Regex("^(?:\\[[^]]+]\\s*)?(\\w{1,16}) entered Critter Safari!$")
    val run get() = ledger.current ?: ledger.last
    val visible get() = SafariVisibility.visible(ConfigManager.showWhere, SafariAssist.inSafari, SafariAssist.atEntrance)

    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { receive(message.string) }
        }
    }
    private fun syncWorld(client: Minecraft) {
        if (client.level !== level) { finish(); level = client.level }
    }
    private fun start(now: Long, explicit: Boolean = false) {
        if (explicit || ledger.current == null) {
            if (explicit) ledger.enter(now) else ledger.arrive(now)
            BeeNests.reset(); SparklingEncounters.reset()
        }
        awayTicks = 0
    }
    private fun receive(raw: String) {
        val client = Minecraft.getInstance()
        if (client.level == null || client.player == null) return
        syncWorld(client)
        val text = SafariRules.strip(raw)
        val now = System.currentTimeMillis()
        if (entry.matchEntire(text)?.groupValues?.get(1).equals(client.player!!.name.string, true)) {
            SafariAssist.markEntered(); start(now, explicit = true)
        }
        val catch = SafariCatch.parse(text) ?: return
        // These server capture messages are evidence of a run even before its area line arrives.
        SafariAssist.markEntered()
        start(now)
        ledger.current!!.record(catch)
        lastCatch = now
    }
    fun onClientTick(client: Minecraft) {
        syncWorld(client)
        if (SafariAssist.inSafari) start(System.currentTimeMillis())
        else if (ledger.current != null && ++awayTicks >= 40 && System.currentTimeMillis() - lastCatch > 5000) finish()
    }
    fun ensureRun(client: Minecraft) { syncWorld(client); if (SafariAssist.inSafari) start(System.currentTimeMillis()) }
    fun finish() { ledger.leave(System.currentTimeMillis()); awayTicks = 0; lastCatch = 0; BeeNests.reset(); SparklingEncounters.reset() }

    fun panel(graphics: GuiGraphicsExtractor, x: Int, y: Int, heading: String, rows: List<String>) {
        val font = Minecraft.getInstance().font
        val width = (rows + heading).maxOf(font::width) + 12
        graphics.fill(x, y, x + width, y + 22 + rows.size * 11, 0xDD16211A.toInt())
        graphics.text(font, heading, x + 6, y + 5, 0xFFE3BB67.toInt(), true)
        rows.forEachIndexed { i, row -> graphics.text(font, row, x + 6, y + 20 + i * 11, 0xFFF0E1BE.toInt(), true) }
    }
    fun countRows(biome: SafariBiome) = listOf("Critter · You / Party (seen)") + biome.critters.map {
        "${it.name} · ${run?.own(it.name) ?: 0} / ${run?.party(it.name) ?: 0}"
    }
    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (client.player == null || !visible) return
        val run = run
        val unique = ConfigManager.countUniqueOnly
        if (ConfigManager.progressHud) {
            val elapsed = run?.let { (((it.endedAt ?: System.currentTimeMillis()) - it.startedAt) / 1000).coerceAtLeast(0) } ?: 0
            val rows = listOf("Party (seen) · ${run?.progress(SafariRoster.all, unique) ?: 0}/37",
                "You · ${run?.ownProgress(SafariRoster.all) ?: 0}/37") + SafariBiome.entries.map {
                "${it.label} · ${run?.progress(it.critters, unique) ?: 0}/${it.critters.size}"
            }
            panel(graphics, 8, 70, "${if (ledger.current != null) "Safari" else "Last Safari"} · ${elapsed / 60}m ${elapsed % 60}s", rows)
            val progress = run?.progress(SafariRoster.all, unique) ?: 0
            graphics.fill(14, 159, 142, 162, 0xFF3E6040.toInt())
            graphics.fill(14, 159, 14 + 128 * progress / 37, 162, 0xFFE3BB67.toInt())
        }
        val biome = SafariAssist.biome ?: return
        if (ConfigManager.missingPanel) {
            val rows = biome.critters.filter { run?.complete(it, unique) != true }.map {
                val left = (it.required(unique) - (run?.party(it.name) ?: 0)).coerceAtLeast(0)
                it.name + if (left > 1) " ×$left" else ""
            }.ifEmpty { listOf("Complete!") }
            val width = (rows + "Missing · ${biome.label}").maxOf(client.font::width) + 12
            panel(graphics, (client.window.guiScaledWidth - width - 8).coerceAtLeast(8), 70, "Missing · ${biome.label}", rows)
        }
        if (ConfigManager.catchCountPanel) {
            val rows = countRows(biome)
            val x = if (ConfigManager.progressHud) 190 else 8
            panel(graphics, x, 70, "Captures · ${biome.label}", rows)
        }
    }
}
