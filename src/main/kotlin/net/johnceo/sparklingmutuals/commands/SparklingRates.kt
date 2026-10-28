package net.johnceo.sparklingmutuals.commands

import java.util.Locale
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

data class SparklingRateEstimate(val biomes: List<SafariBiome>, val boat: Map<String, Double>,
    val passive: Map<String, Double>, val manual: Map<String, Double>, val mode: SafariMode = SafariMode.SPARKLING) {
    val checkedPerRun get() = boat.values.sum() + passive.values.sum() + manual.values.sum()
    val runsPerSparkling get() = checkedPerRun.takeIf { it > 0 }?.let { 2048.0 / it }
}

/** Expected check opportunities for the selected mode; no current-entity counts or network requests. */
object SparklingRates {
    val boatDetected = setOf("Flitter", "Shyworm", "Driftling", "Chuckwalla", "Mantis Shrimp", "Troodon")

    fun estimate(party: PartySparklingState, averages: Map<String, Double>? = null,
        mode: SafariMode = SafariMode.SPARKLING): SparklingRateEstimate {
        require(mode == SafariMode.FULL_CLEAR || party.ready) { "Party discovery data is not ready" }
        val spawnAverages = averages ?: if (mode == SafariMode.FULL_CLEAR) CritterSpawnAverages.fullParty else CritterSpawnAverages.lazyParty
        val biomes = if (mode != SafariMode.SPARKLING) SafariBiome.entries.toList() else SafariBiome.entries.filter { biome ->
            biome.critters.any { it.name !in boatDetected && party.needs(it.name) }
        }
        val allBirdsShared = mode != SafariMode.FULL_CLEAR &&
            SafariRoster.all.filter { SafariLookup.isBird(it.name) }.all { !party.needs(it.name) }
        val boat = linkedMapOf<String, Double>()
        val passive = linkedMapOf<String, Double>()
        val manual = linkedMapOf<String, Double>()
        SafariRoster.all.forEach { critter ->
            val name = critter.name
            val destination = when {
                name in boatDetected -> boat
                critter.biome !in biomes -> return@forEach
                !SafariLookup.isManualCheck(name) -> passive
                mode != SafariMode.SPARKLING -> manual
                SafariLookup.isBird(name) -> if (allBirdsShared) return@forEach else manual
                party.needs(name) -> manual
                else -> return@forEach
            }
            val average = spawnAverages.getValue(name)
            require(average.isFinite() && average >= 0) { "Invalid spawn average for $name" }
            destination[name] = if (mode == SafariMode.UNIQUE && destination === manual &&
                (if (SafariLookup.isBird(name)) allBirdsShared else !party.needs(name))) minOf(1.0, average) else average
        }
        return SparklingRateEstimate(biomes, boat, passive, manual, mode)
    }

    fun message(result: SparklingRateEstimate): String {
        val runs = result.runsPerSparkling ?: return "[SM] 0.00 critters checked per run. No sparkling rate can be estimated without checks."
        return String.format(Locale.ROOT,
            "[SM] %.2f critters checked per run. Your party should find a sparkling every %.2f runs.", result.checkedPerRun, runs)
    }

    fun breakdown(result: SparklingRateEstimate): String = buildString {
        append("Mode: ${when (result.mode) { SafariMode.UNIQUE -> "Unique"; SafariMode.FULL_CLEAR -> "Full Clear"; SafariMode.SPARKLING -> "Sparkle" }}\n")
        append("Biomes to load (${result.biomes.size}): ")
        append(result.biomes.joinToString(", ") { it.label }.ifEmpty { "None" })
        fun section(title: String, checks: Map<String, Double>) {
            append("\n\n$title: ")
            append(String.format(Locale.ROOT, "%.2f average checks", checks.values.sum()))
            if (checks.isEmpty()) append("\nNone")
            else SafariBiome.entries.forEach { biome ->
                val names = checks.keys.filter { SafariRoster.named(it)?.biome == biome }
                if (names.isNotEmpty()) append("\n${biome.label}: ${names.joinToString(", ")}")
            }
        }
        section("Detected from boat", result.boat)
        section("Additional biome-load detections", result.passive)
        section("Manual checks / spawns", result.manual)
        append("\n\n${if (result.mode == SafariMode.FULL_CLEAR) "Full" else "Lazy"} Party spawn averages; 1/2048 chance per critter.\nAssumes every planned check is completed.")
        if (result.mode == SafariMode.UNIQUE) append("\nShared manual species: one check per available type (capped by its average). Needed bird checks include the entire bird spawning group.")
    }

    fun show(source: FabricClientCommandSource): Int {
        val mode = SafariFullClear.mode
        if (mode == SafariMode.FULL_CLEAR) return display(source, estimate(PartySparklingState(), mode = mode))
        val client = Minecraft.getInstance()
        val self = client.player ?: return 1
        // Read the verified current roster and retained cache only; never start a roster or profile lookup.
        val members = PartyManager.withLocal(self.uuid.toString())
        val discoveries = members?.mapNotNull { id -> SafariLookup.known(id)?.let { id to it } }?.toMap()
        val party = PartySparklingState()
        if (members == null || discoveries == null) {
            source.sendFeedback(LocalChat.component("[SM] Party roster is not loaded yet. Try again once party data is available."))
            return 1
        }
        party.select(members)
        if (!party.accept(members, discoveries)) {
            source.sendFeedback(LocalChat.component("[SM] Cached discoveries are not loaded for every party member yet. No rate calculated."))
            return 1
        }
        return display(source, estimate(party, mode = mode))
    }
    private fun display(source: FabricClientCommandSource, result: SparklingRateEstimate): Int {
        val text = LocalChat.component(message(result)).copy().withStyle {
            it.withHoverEvent(HoverEvent.ShowText(Component.literal(breakdown(result))))
        }
        source.sendFeedback(text)
        return 1
    }
}
