package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.structures.SafariStructures
import net.johnceo.sparklingmutuals.safari.structures.BeeNests

/** `!m` and `!m <biome>`: compact assigned-biome capture and checking requirements for this mode. */
object SafariMissingReply {
    /** Full names, the short and colloquial forms typed mid-run, and single letters. */
    fun biome(word: String): SafariBiome? = when (word.lowercase()) {
        "c", "cav", "cave", "caves", "cavern" -> SafariBiome.CAVERN
        "f", "for", "fore", "forest", "wood", "woods" -> SafariBiome.FOREST
        "i", "ice", "icy", "frozen", "cold" -> SafariBiome.ICY
        "h", "haunt", "haunted", "spooky", "ghost" -> SafariBiome.HAUNTED
        else -> null
    }
    fun prefix(biome: SafariBiome?) = if (biome == null) "Missing: " else "Missing ${biome.label}: "
    fun format(biome: SafariBiome?, missing: (SafariBiome) -> List<String>): String {
        fun list(b: SafariBiome) = missing(b).ifEmpty { listOf("None") }.joinToString("; ")
        return prefix(biome) + if (biome != null) list(biome)
            else SafariBiome.entries.joinToString(" | ") { "${it.label}: ${list(it)}" }
    }
    fun current(biome: SafariBiome?, run: SafariRun? = SafariTracking.ledger.current): String {
        val selected = biome ?: run?.biomeAtThirtySeconds ?: return "Missing: None"
        return format(selected) { requirements(run, selected, SafariFullClear.mode, SafariSparklingMode.state,
            SafariStructures.nearbyMounds, SafariStructures.walls.remaining,
            BeeNests.unpunchedCount.takeIf { run?.beeNestCountKnown == true || run?.trustLateForestNestReport == true }) }
    }
    /** Reads retained run evidence plus currently rendered source counts; never fetches API data. */
    fun requirements(run: SafariRun?, biome: SafariBiome, mode: SafariMode, party: PartySparklingState,
        mounds: Int, walls: String, nests: Int? = null): List<String> {
        if (run == null) return emptyList()
        val result = mutableListOf<String>()
        if (mode == SafariMode.FULL_CLEAR) {
            biome.critters.forEach { critter ->
                val left = run.capturesLeft(critter.name)
                if (left > 0) result.add("${critter.name}: $left")
            }
            if (biome == SafariBiome.CAVERN) {
                if (mounds > 0) result.add("Mounds: $mounds")
                if ((walls.toIntOrNull() ?: 0) > 0) result.add("Snoozle Walls: $walls")
            }
            if (biome == SafariBiome.FOREST && run.birdFoodLeftToUse > 0) result.add("Bird food: ${run.birdFoodLeftToUse} left to use")
            return result
        }
        if (mode == SafariMode.UNIQUE) {
            val left=biome.critters.filter { run.count(it.name) == 0 && !run.uniqueUnavailable(it.name) }.map { it.name }
            result.add("Uniques left to capture: ${left.joinToString(", ").ifEmpty { "None" }}")
        }
        if (!party.ready) return result
        fun needs(name: String) = party.needs(name) && !run.sparklingComplete(name,party)
        when(biome) {
            SafariBiome.CAVERN -> {
                if(needs("Rockmite") && mounds > 0) result.add("Mounds: $mounds")
                if(needs("Snoozle") && (walls.toIntOrNull() ?: 0) > 0) result.add("Snoozle Walls: $walls")
                if(needs("Gemzie") && !run.sparklingChecks.gemzieChamberOpened && run.count("Gemzie") == 0)
                    result.add("Gemzie: chamber unopened")
            }
            SafariBiome.FOREST -> {
                if(listOf("Bluebird","Parakeet","Macaw").any(::needs))
                    run.knownBirdFoodLeftToUse?.takeIf { it > 0 }?.let { result.add("Bird food: $it left to use") }
                if(needs("Honeybug")) nests?.takeIf { it > 0 }?.let { result.add("Bee nests: $it") }
            }
            SafariBiome.ICY -> Unit
            SafariBiome.HAUNTED -> {
                if(needs("Gazer")) (4-run.sparklingChecks.checkCount("Gazer")).coerceAtLeast(0).takeIf { it > 0 }
                    ?.let { result.add("Gazers: $it left to check") }
                if(needs("Gimmiegold")) run.coinsLeftToUse?.takeIf { it > 0 }?.let { result.add("Shining Coins: $it left to use") }
            }
        }
        return result
    }
    /**
     * An `!m` answer belongs to the run it was asked in. Leaving the Safari, or the ledger moving on to
     * another run while the election or a queued reply waits, drops it instead of answering for the wrong run.
     * The world is checked too, because command publication can run before the ledger's next world update.
     * Every other command is not bound to a run.
     */
    fun holds(command: PartyCommand, asked: SafariRun?, current: SafariRun?, inSafari: Boolean,
        askedWorld: Any? = null, currentWorld: Any? = askedWorld, currentBiome: SafariBiome? = SafariAssist.biome) =
        command.kind != PartyCommandKind.MISSING_RUN ||
            (inSafari && asked != null && asked === current && askedWorld === currentWorld &&
                asked.biomeAtThirtySeconds != null && (command.biome == null || command.biome == asked.biomeAtThirtySeconds))
}
