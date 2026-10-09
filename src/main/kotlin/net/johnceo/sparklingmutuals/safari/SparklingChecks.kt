package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.commands.PartyChat
import java.util.Locale
import java.util.UUID

/** Run-owned check evidence, independent of captures and personal bests. */
class SparklingChecks {
    private val completed = mutableSetOf<String>()
    private val observed = mutableMapOf<String, MutableSet<UUID>>()
    private val centersReached = mutableSetOf<SafariBiome>()
    private val espSeenAt = mutableMapOf<UUID, Long>()
    private var coinsPicked = 0
    private var coinsSpent = 0
    private var heldCoins: Int? = null
    fun checked(name: String) = name in completed
    fun keepEsp(uuid: UUID, now: Long) = now - espSeenAt.getOrPut(uuid) { now } < 10000
    fun wumpaPrerequisite(name: String, party: PartySparklingState) = name != "Wumpa" &&
        SafariRoster.named(name)?.biome == SafariBiome.ICY && party.needs("Wumpa") && !checked("Wumpa")
    fun scan(biome: SafariBiome, x: Double, z: Double, seen: List<Pair<String, UUID>>,
        nestsChecked: Boolean = false, moundsCleared: Boolean = false, wallsCleared: Boolean = false,
        birdsSpawned: Boolean = false, hauntedDropsRemaining: Int = -1) {
        seen.forEach { (name, uuid) -> observed.getOrPut(name) { mutableSetOf() }.add(uuid) }
        val (cx, cz) = centers.getValue(biome)
        val atCenter = (x - cx) * (x - cx) + (z - cz) * (z - cz) <= 225
        if (atCenter) centersReached.add(biome)
        if (biome in centersReached) completed.addAll(biome.critters.map { it.name }.filterNot { it in spawned })
        fun enough(name: String, amount: Int = 1) = (observed[name]?.size ?: 0) >= amount
        fun check(name: String, ready: Boolean) { if (ready) completed.add(name) }
        when (biome) {
            SafariBiome.FOREST -> {
                check("Honeybug", nestsChecked && enough("Honeybug", 3))
                if (birdsSpawned) completed.addAll(birds)
            }
            SafariBiome.CAVERN -> {
                check("Rockmite", moundsCleared); check("Snoozle", wallsCleared)
                check("Gemzie", enough("Gemzie", 3))
            }
            SafariBiome.ICY -> check("Wumpa", enough("Wumpa"))
            SafariBiome.HAUNTED -> {
                check("Gazer", enough("Gazer", 4)); check("Doomspiral", enough("Doomspiral"))
                check("Gimmiegold", atCenter && hauntedDropsRemaining == 0 &&
                    coinsPicked > 0 && coinsSpent >= coinsPicked && heldCoins == 0 && enough("Gimmiegold", 3))
            }
        }
    }
    fun chat(raw: String, manualAllowed: Boolean = false) {
        val text = SafariRules.strip(raw)
        if (text.startsWith("FLOOR DROP!") && Regex("\\bShining Coin\\b(?! Shard)").containsMatchIn(text)) {
            coinsPicked++; heldCoins = null
        }
        if (text == "A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!") {
            coinsSpent++; heldCoins = null
        }
        if (manualAllowed) {
            globalCompletions[text]?.let(completed::add)
            completed.addAll(doneBiome(text)?.critters?.map { it.name } ?: emptyList())
        }
    }
    fun inventory(stacks: List<Pair<String, Int>>) {
        heldCoins = stacks.filter { SafariRules.strip(it.first).trim() == "Shining Coin" }.sumOf { it.second.coerceAtLeast(0) }
    }
    companion object {
        private val globalCompletions = mapOf(
            "A rumbling sound can be heard, and the door at the back of the chamber opens..." to "Gemzie",
            "The darkness in the Haunted Biome fades away..." to "Doomspiral",
            "The cave is collapsing..." to "Wumpa")
        private val birds = setOf("Bluebird", "Parakeet", "Macaw")
        private val spawned = birds + setOf("Honeybug", "Rockmite", "Snoozle", "Gemzie", "Wumpa", "Gazer", "Gimmiegold", "Doomspiral")
        private val centers = mapOf(SafariBiome.FOREST to (6.0 to 51.0), SafariBiome.CAVERN to (-114.0 to 49.0),
            SafariBiome.ICY to (-112.0 to -54.0), SafariBiome.HAUNTED to (-4.0 to -64.0))
        fun doneBiome(raw: String): SafariBiome? {
            val word = PartyChat.parse(SafariRules.strip(raw))?.body?.trim()?.removeSuffix(".")?.lowercase(Locale.ROOT)
            return doneWords[word]
        }
        private val doneWords = mapOf("fd" to SafariBiome.FOREST, "cd" to SafariBiome.CAVERN,
            "id" to SafariBiome.ICY, "hd" to SafariBiome.HAUNTED)
    }
}
