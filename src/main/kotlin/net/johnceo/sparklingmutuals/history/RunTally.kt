package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.safari.SafariCatch

/**
 * What a run's chat says beyond the captures the run itself counts: shards, Rainbow Feathers, Macaws and sparklings.
 * Fed server lines only; party, guild and private chat never reach it.
 */
class RunTally {
    val shards = linkedMapOf<String, Int>()
    val catches = linkedMapOf<String, Int>()
    val sparklings = mutableListOf<String>()
    var feathers = 0
    var macaws = 0
    var caught = 0
    var lootShares = 0
    val party = linkedSetOf<String>()
    var ownSparklings = 0
    var safariEssence = -1
    var summaryShards = -1
    var huntingXp = -1L
    private var inSummary = false
    var allTypesAt: Long? = null
        private set

    fun summaryLine(text: String): Boolean {
        if (text == "SAFARI REWARD SUMMARY") { inSummary = true; return true }
        if (!inSummary) return false
        val match = summaryReward.matchEntire(text) ?: return false
        val amount = match.groupValues[1].replace(",", "").toLongOrNull() ?: return false
        when (match.groupValues[2]) {
            "Shards" -> summaryShards = amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            "Safari Essence" -> safariEssence = amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            "Hunting Exp" -> huntingXp = amount
        }
        return true
    }

    fun line(text: String, self: String, at: Long? = null) {
        if (summaryLine(text)) return
        if (text == MACAW_BONUS) { feathers++; return }
        sparkling.matchEntire(text)?.let { match ->
            val player = match.groupValues[1]
            sparklings += "$player:${match.groupValues[2].trim()}"
            if (player.equals(self, true)) ownSparklings++
            return
        }
        val catch = SafariCatch.parse(text) ?: return
        val critter = catch.critter.name
        party += critter
        if (party.size == net.johnceo.sparklingmutuals.safari.SafariRoster.all.size && allTypesAt == null && at != null) allTypesAt = at
        if (catch.personal) { caught++; catches.merge(critter, 1, Int::plus) } else lootShares++
        if (critter == "Macaw") macaws++
        if ("Shard" in text) shards.merge(critter, quantity.find(text)?.groupValues?.get(1)?.toInt() ?: 1, Int::plus)
        feather.find(text)?.let { feathers += it.groupValues[1].toIntOrNull() ?: 1 }
    }

    companion object {
        private val summaryReward = Regex("^\\+([0-9,]+) (Shards|Safari Essence|Hunting Exp)$")
        const val MACAW_BONUS = "MACAW! You found a bonus Rainbow Feather!"
        /** "gained 2x Cavernfish Shard": the quantity, read off the shard itself. */
        val quantity = Regex("""(\d+)x [A-Za-z' ]+? Shard""")
        /** "received a Rainbow Feather" / "received 2x Rainbow Feather" on a sparkling catch or its loot share. */
        val feather = Regex("""(?:(\d+)x |an? )Rainbow Feather""")
        val sparkling = Regex("""^SPARKLING!\s+(?:\[[^]\r\n]* head]\s*)?([A-Za-z0-9_]{1,16})\s+(?:caught|found) a SPARKLING (.+?)!$""", RegexOption.IGNORE_CASE)
    }
}
