package net.johnceo.sparklingmutuals.commands

import java.util.Locale
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

data class SparklingRateEstimate(val biomes: List<SafariBiome>, val boat: Map<String, Double>,
    val passive: Map<String, Double>, val manual: Map<String, Double>) {
    val checkedPerRun get() = boat.values.sum() + passive.values.sum() + manual.values.sum()
    val runsPerSparkling get() = checkedPerRun.takeIf { it > 0 }?.let { 2048.0 / it }
}

/** Expected Sparkling-mode checks, not current entities or completion quotas. No network requests. */
object SparklingRates {
    val boatDetected = setOf("Flitter", "Shyworm", "Driftling", "Chuckwalla", "Mantis Shrimp", "Troodon")

    fun estimate(party: PartySparklingState, averages: Map<String, Double> = CritterSpawnAverages.lazyParty): SparklingRateEstimate {
        require(party.ready) { "Party discovery data is not ready" }
        val biomes = SafariBiome.entries.filter { biome ->
            biome.critters.any { it.name !in boatDetected && party.needs(it.name) }
        }
        val allBirdsShared = SafariRoster.all.filter { SafariLookup.isBird(it.name) }.all { !party.needs(it.name) }
        val boat = linkedMapOf<String, Double>()
        val passive = linkedMapOf<String, Double>()
        val manual = linkedMapOf<String, Double>()
        SafariRoster.all.forEach { critter ->
            val name = critter.name
            val destination = when {
                name in boatDetected -> boat
                critter.biome !in biomes -> return@forEach
                !SafariLookup.isManualCheck(name) -> passive
                SafariLookup.isBird(name) -> if (allBirdsShared) return@forEach else manual
                party.needs(name) -> manual
                else -> return@forEach
            }
            val average = averages.getValue(name)
            require(average.isFinite() && average >= 0) { "Invalid spawn average for $name" }
            destination[name] = average
        }
        return SparklingRateEstimate(biomes, boat, passive, manual)
    }

    fun message(result: SparklingRateEstimate): String {
        val runs = result.runsPerSparkling ?: return "[SM] 0.00 critters checked per run. No sparkling rate can be estimated without checks."
        return String.format(Locale.ROOT,
            "[SM] %.2f critters checked per run. Your party should find a sparkling every %.2f runs.", result.checkedPerRun, runs)
    }

    fun breakdown(result: SparklingRateEstimate): String = buildString {
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
        append("\n\nLazy Party spawn averages; 1/2048 chance per critter.\nAssumes every planned check is completed.")
    }

    fun show(source: FabricClientCommandSource): Int {
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
        val result = estimate(party)
        val text = LocalChat.component(message(result)).copy().withStyle {
            it.withHoverEvent(HoverEvent.ShowText(Component.literal(breakdown(result))))
        }
        source.sendFeedback(text)
        return 1
    }
}
