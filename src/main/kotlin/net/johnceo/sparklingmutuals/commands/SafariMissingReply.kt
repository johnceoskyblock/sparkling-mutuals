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

/** `!m` and `!m <biome>`: compact current-biome capture and checking requirements for this mode. */
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
        fun list(b: SafariBiome) = missing(b).ifEmpty { listOf("Done") }.joinToString(", ")
        return prefix(biome) + if (biome != null) list(biome)
            else SafariBiome.entries.joinToString(" | ") { "${it.label}: ${list(it)}" }
    }
    fun current(biome: SafariBiome?, run: SafariRun? = SafariTracking.ledger.current): String {
        val selected = biome ?: SafariAssist.biome ?: return "Missing: biome unavailable"
        return format(selected) { requirements(run, selected, SafariFullClear.mode, SafariSparklingMode.state,
            SafariStructures.nearbyMounds, SafariStructures.walls.remaining) }
    }
    /** Reads retained run evidence plus currently rendered source counts; never fetches API data. */
    fun requirements(run: SafariRun?, biome: SafariBiome, mode: SafariMode, party: PartySparklingState,
        mounds: Int, walls: String): List<String> {
        if (run == null) return listOf("run unavailable")
        val result = mutableListOf<String>()
        if (mode == SafariMode.FULL_CLEAR) {
            biome.critters.forEach { critter ->
                val left = run.capturesLeft(critter.name)
                if (left > 0) result.add("${critter.name}: $left")
            }
            if (biome == SafariBiome.CAVERN) {
                if (mounds > 0) result.add("Mounds: $mounds")
                if (walls != "0") result.add("Snoozle Walls: $walls")
            }
            if (biome == SafariBiome.FOREST && run.birdFoodLeftToUse > 0) result.add("Bird food: ${run.birdFoodLeftToUse} left to use")
            return result
        }
        if (mode == SafariMode.UNIQUE) biome.critters.filter { run.count(it.name) == 0 && !run.uniqueComplete(it.name, party) }
            .forEach { result.add(it.name) }
        if (!party.ready) return result + "shiny requirements loading"
        val needed = biome.critters.filter { party.needs(it.name) }
        var foodListed = false
        needed.forEach { critter ->
            val name = critter.name
            // This note is intentional even after a first bee capture or an earlier nest check.
            if (name == "Honeybug") {
                if (mode == SafariMode.UNIQUE || !run.sparklingComplete(name, party)) result.add("Make sure to clear all nests")
                return@forEach
            }
            if (run.sparklingComplete(name, party)) return@forEach
            val note = when (name) {
                "Rockmite" -> if (mounds > 0) "Mounds: $mounds" else "Rockmite: check spawns"
                "Snoozle" -> if (walls != "0") "Snoozle Walls: $walls" else "Snoozle: check spawns"
                "Bluebird", "Parakeet", "Macaw" -> {
                    if (foodListed) return@forEach
                    foodListed = true
                    "Bird food: ${run.birdFoodLeftToUse} left to use; check birds"
                }
                "Gimmiegold" -> "Shining Coins: ${run.coinsLeftToUse?.toString() ?: "?"} left to use; check Gimmiegolds"
                "Wumpa", "Doomspiral" -> if (run.count(name) == 0) "$name: capture" else return@forEach
                else -> "$name: check spawns"
            }
            // The capture name is enough when the same line already says to capture it.
            if (note.endsWith(": capture")) result.remove(name)
            result.add(note)
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
                (command.biome == null || command.biome == currentBiome))
}
