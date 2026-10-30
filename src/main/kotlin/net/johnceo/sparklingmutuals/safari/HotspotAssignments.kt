package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.commands.PartyChat

/** Server party-chat order gives the first reporter each hotspot; duplicates share one tie-break. */
class HotspotAssignments {
    private val reports=linkedMapOf<String,SafariBiome>()
    private var self: String?=null
    private var hotspot: SafariBiome?=null
    private var beganAt: Long?=null
    private var finalAssignment: SafariBiome?=null
    fun begin(name: String, actual: SafariBiome, now: Long) {
        if (beganAt != null) return
        self=name.lowercase();hotspot=actual;beganAt=now
    }
    fun report(name: String, biome: SafariBiome, now: Long) {
        if (beganAt?.let { now-it > 10_000 } == true) return
        reports.putIfAbsent(name.lowercase(),biome)
    }
    fun assignment(members: Set<String>, now: Long, uuids: Map<String,String> = emptyMap(), expectedSize: Int = members.size): SafariBiome? {
        val name=self ?: return null
        val actual=hotspot ?: return null
        val start=beganAt ?: return null
        finalAssignment?.let { return it }
        val expected=members.map(String::lowercase).toSet()+name
        if (expectedSize <= 1) return actual.also { finalAssignment=it }
        val allReported=expected.size >= expectedSize && reports.keys.containsAll(expected)
        if ((!allReported || name !in reports) && now-start < 3500) return null
        val ordered=reports.filterKeys { it in expected }.toMutableMap()
        // If the server echo was unavailable, still show a local assignment after the short wait.
        ordered.putIfAbsent(name,actual)
        val result=allocate(ordered,uuids)[name] ?: actual
        if (allReported || now-start >= 10_000) finalAssignment=result
        return result
    }
    companion object {
        fun parse(raw: String): Pair<String,SafariBiome>? {
            val chat=PartyChat.parse(raw) ?: return null
            val biome=Regex("^\\[SM] Hotspot: (Forest|Cavern|Icy|Haunted)$").matchEntire(chat.body)?.groupValues?.get(1) ?: return null
            return chat.sender to SafariBiome.entries.first { it.label == biome }
        }
        fun allocate(reports: Map<String,SafariBiome>, uuids: Map<String,String> = emptyMap()): Map<String,SafariBiome> {
            val assigned=linkedMapOf<String,SafariBiome>()
            val used=mutableSetOf<SafariBiome>()
            val duplicates=mutableListOf<String>()
            reports.forEach { (name,biome) -> if (used.add(biome)) assigned[name]=biome else duplicates.add(name) }
            // Hash UUIDs for a shared, stable pseudo-random player rank, independent of local RNG.
            fun identity(name: String)=uuids[name.lowercase()]?.replace("-","")?.lowercase() ?: name.lowercase()
            val ranked=duplicates.sortedWith(compareBy<String> { identity(it).hashCode() }.thenBy(::identity))
            val free=SafariBiome.entries.filter { it !in used }
            ranked.zip(free).forEach { (name,biome) -> assigned[name]=biome }
            return assigned
        }
    }
}
