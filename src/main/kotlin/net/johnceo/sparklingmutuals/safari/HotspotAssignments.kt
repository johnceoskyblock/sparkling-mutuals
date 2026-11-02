package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.commands.PartyChat

/** Collect one reporting window, favor fewer prior hotspot wins, then use a shared UUID tie-break. */
class HotspotAssignments(private val wins: MutableMap<String,Int> = mutableMapOf()) {
    private var firstReportAt: Long? = null
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
        if (firstReportAt == null) firstReportAt = now
        reports.putIfAbsent(name.lowercase(),biome)
    }
    fun assignment(members: Set<String>, now: Long, uuids: Map<String,String> = emptyMap(), expectedSize: Int = members.size): SafariBiome? {
        val name=self ?: return null
        val actual=hotspot ?: return null
        if (beganAt == null) return null
        finalAssignment?.let { return it }
        val expected=members.map(String::lowercase).toSet()+name
        if (expectedSize <= 1) return actual.also { finalAssignment=it }
        val first=firstReportAt ?: return null
        val allReported=expected.size >= expectedSize && reports.keys.containsAll(expected)
        if (now-first < 2000 || !allReported && now-first < 5000) return null
        val ordered=reports.filterKeys { it in expected }.toMutableMap()
        ordered.putIfAbsent(name,actual)
        val allocated=allocate(ordered,uuids,wins)
        allocated.forEach { (member,biome) ->
            if (ordered[member] == biome) wins[member]=(wins[member] ?: 0)+1
        }
        val result=allocated[name] ?: actual
        finalAssignment=result
        return result
    }
    companion object {
        // Anchored announcements only: accept other mods without interpreting casual chat or quotes.
        private val announcement=Regex(
            """^(?:\[(?:SM|CritterMod|SafariUtils|CM|SU)]\s*)?(?:My\s+)?(?:Hunting\s+)?Hotspot:\s*(Forest|Cavern|Icy|Ice|Haunted)(?:\s+Biome)?[!.]?$""",
            RegexOption.IGNORE_CASE)

        fun parse(raw: String): Pair<String,SafariBiome>? {
            val chat=PartyChat.parse(raw) ?: return null
            val label=announcement.matchEntire(chat.body.trim())?.groupValues?.get(1) ?: return null
            val biome=if (label.equals("Ice",true)) SafariBiome.ICY else
                SafariBiome.entries.first { it.label.equals(label,true) }
            return chat.sender to biome
        }
        fun allocate(reports: Map<String,SafariBiome>, uuids: Map<String,String> = emptyMap(), wins: Map<String,Int> = emptyMap()): Map<String,SafariBiome> {
            val assigned=linkedMapOf<String,SafariBiome>()
            val used=mutableSetOf<SafariBiome>()
            val duplicates=mutableListOf<String>()
            fun identity(name: String)=uuids[name.lowercase()]?.replace("-","")?.lowercase() ?: name.lowercase()
            reports.entries.groupBy { it.value }.toSortedMap(compareBy { it.ordinal }).forEach { (biome,group) ->
                val ranked=group.map { it.key }.sortedWith(compareBy<String> { wins[it] ?: 0 }
                    .thenBy { identity(it).hashCode() }.thenBy(::identity))
                assigned[ranked.first()]=biome
                used.add(biome)
                duplicates.addAll(ranked.drop(1))
            }
            val ranked=duplicates.sortedWith(compareBy<String> { identity(it).hashCode() }.thenBy(::identity))
            val free=SafariBiome.entries.filter { it !in used }
            ranked.zip(free).forEach { (name,biome) -> assigned[name]=biome }
            return assigned
        }
    }
}
