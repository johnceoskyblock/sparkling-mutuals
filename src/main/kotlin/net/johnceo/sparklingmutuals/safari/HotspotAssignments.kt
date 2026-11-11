package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.commands.PartyChat

/** Collect one reporting window, favor fewer prior hotspot wins, then use a shared UUID tie-break. */
class HotspotAssignments(private val wins: MutableMap<String,Int> = mutableMapOf()) {
    private var firstReportAt: Long? = null
    private val reports=linkedMapOf<String,SafariBiome>()
    private val reportTimes=mutableMapOf<String,Long>()
    private val coordinated=mutableSetOf<String>()
    private var allocated: Map<String,SafariBiome>?=null
    private var credited=emptySet<String>()
    private var preRunWins: Map<String,Int>?=null
    private var leaderControlled=false
    private var leaderFinal=false
    val finalPlan get()=allocated?.toMap()
    fun observedHotspots()=reports.toMap()
    private val notified=mutableSetOf<String>()
    private var self: String?=null
    private var hotspot: SafariBiome?=null
    private var beganAt: Long?=null
    private var finalAssignment: SafariBiome?=null
    fun begin(name: String, actual: SafariBiome, now: Long) {
        if (beganAt != null) return
        self=name.lowercase();hotspot=actual;beganAt=now
    }
    fun snapshot(members: Set<String>): Map<String,Int> {
        if(preRunWins==null) preRunWins=members.associate { it.lowercase() to (wins[it.lowercase()] ?: 0) }
        return preRunWins!!.toMap()
    }
    private fun fresh(now: Long,limit: Long)=beganAt?.let { now-it in 0..limit }==true
    fun acceptWins(message: HotspotProtocol.Wins,leader: String?,members: Set<String>,now: Long): Boolean {
        if(leader==null || !message.sender.equals(leader,true) || !fresh(now,60_000) || leaderControlled || leaderFinal ||
            message.counts.keys!=members.map(String::lowercase).toSet() || message.counts.values.any { it !in 0..1_000_000 } ||
            reports[leader.lowercase()]?.let { it!=message.hotspot }==true) return false
        uncredit()
        wins.clear();wins.putAll(message.counts)
        preRunWins=message.counts.toMap()
        allocated=null;finalAssignment=null
        leaderControlled=true
        report(message.sender,message.hotspot,now,true)
        return true
    }
    fun acceptAssignment(message: HotspotProtocol.Assignment,leader: String?,members: Set<String>,now: Long): Boolean {
        val plan=message.biomes
        if(leader==null || !message.sender.equals(leader,true) || !fresh(now,60_000) || leaderFinal ||
            plan.keys!=members.map(String::lowercase).toSet() || self !in plan || plan.size !in 1..4 ||
            plan.values.toSet().size!=plan.size) return false
        finish(plan)
        leaderFinal=true
        return true
    }
    private fun uncredit() {
        credited.forEach { wins[it]=((wins[it] ?: 0)-1).coerceAtLeast(0) }
        credited=emptySet()
    }
    private fun finish(plan: Map<String,SafariBiome>) {
        uncredit()
        val actuals=reports.toMutableMap()
        self?.let { name -> hotspot?.let { actuals.putIfAbsent(name,it) } }
        credited=plan.filter { (name,biome) -> actuals[name]==biome }.keys.toSet()
        credited.forEach { wins[it]=(wins[it] ?: 0)+1 }
        allocated=plan.toMap();finalAssignment=plan[self]
    }
    fun report(name: String, biome: SafariBiome, now: Long, modReport: Boolean = false) {
        if (firstReportAt == null) firstReportAt = now
        val key=name.lowercase()
        reports.putIfAbsent(key,biome)
        reportTimes.putIfAbsent(key,now)
        if(modReport) coordinated.add(key)
    }
    fun assignment(members: Set<String>, now: Long, uuids: Map<String,String> = emptyMap(), expectedSize: Int = members.size): SafariBiome? {
        val name=self ?: return null
        val actual=hotspot ?: return null
        if (beganAt == null) return null
        allocated?.let { locked ->
            if (!leaderFinal) {
                val arrivals=reports.filterKeys { it !in locked && it in members.map(String::lowercase) }
                val first=arrivals.keys.mapNotNull(reportTimes::get).minOrNull()
                if(first!=null && now-first>=250) {
                    val free=SafariBiome.entries.filter { it !in locked.values }.toMutableSet()
                    val added=linkedMapOf<String,SafariBiome>()
                    val ranked=arrivals.keys.sortedWith(compareBy<String> { preRunWins?.get(it) ?: wins[it] ?: 0 }
                        .thenBy { identity(it,uuids).hashCode() }.thenBy { identity(it,uuids) })
                    ranked.forEach { member -> arrivals[member]?.takeIf { it in free }?.let {
                        added[member]=it;free.remove(it)
                    } }
                    ranked.filter { it !in added }.zip(free).forEach { (member,biome) -> added[member]=biome }
                    if(added.isNotEmpty()) finish(locked+added)
                }
            }
            return finalAssignment
        }
        finalAssignment?.let { return it }
        if(leaderControlled && fresh(now,30_000)) return null
        val expected=members.map(String::lowercase).toSet()+name
        if (expectedSize <= 1) return actual.also { finalAssignment=it }
        val first=firstReportAt ?: return null
        val times=reportTimes.filterKeys { it in expected }.values.sorted()
        val readyAt=if(times.size>=expectedSize) now else first+30_000
        if(now<readyAt || expected.size<expectedSize) return null
        val ordered=reports.filterKeys { it in expected }.toMutableMap()
        ordered.putIfAbsent(name,actual)
        // A roster entry alone does not prove admission. The chat fallback only knows reporters.
        val resultMap=allocate(ordered,uuids,wins)
        snapshot(expected)
        finish(resultMap)
        return finalAssignment
    }
    /** Only the leader sends instructions, once per uncoordinated member per run. */
    fun notifications(leader: Boolean, self: String): List<Pair<String,SafariBiome>> {
        if(!leader) return emptyList()
        return allocated.orEmpty().filterKeys { it!=self.lowercase() && it !in coordinated && it in reports && it !in notified }
            .map { (name,biome) -> notified.add(name); name to biome }
    }
    companion object {
        data class Report(val sender: String,val biome: SafariBiome,val coordinated: Boolean)
        private val prefix=Regex("""^\[(SM|SU|SafariUtils|CritterMod|CM)]\s*""",RegexOption.IGNORE_CASE)
        private val filler=setOf("my","hunting","hotspot","is","the","biome","i","got","have","im","in","at")
        private val aliases=mapOf(SafariBiome.FOREST to listOf("forest"),SafariBiome.CAVERN to listOf("cavern","cav","cave"),
            SafariBiome.ICY to listOf("icy","ice"),SafariBiome.HAUNTED to listOf("haunted","haunt","hvant"))
        private fun close(a: String,b: String): Boolean {
            if(a==b) return true
            if(a.length<4 || kotlin.math.abs(a.length-b.length)>1) return false
            if(a.length==b.length) {
                val mismatches=a.indices.filter { a[it]!=b[it] }
                return mismatches.size==1 || mismatches.size==2 && mismatches[1]==mismatches[0]+1 &&
                    a[mismatches[0]]==b[mismatches[1]] && a[mismatches[1]]==b[mismatches[0]]
            }
            val longer=if(a.length>b.length) a else b
            val shorter=if(a.length>b.length) b else a
            return longer.indices.any { longer.removeRange(it,it+1)==shorter }
        }
        fun parseReport(raw: String, allowFreeText: Boolean = false): Report? {
            HotspotProtocol.parseWins(raw)?.let { return Report(it.sender,it.hotspot,true) }
            val chat=PartyChat.parse(raw) ?: return null
            val tag=prefix.find(chat.body)
            val body=chat.body.removePrefix(tag?.value ?: "").lowercase()
            if (allowFreeText) {
                if (HotspotProtocol.parseAssignment(raw) != null || body.contains("you are assigned")) return null
                val words = body.split(Regex("[^a-z]+")).filter(String::isNotEmpty)
                val meaningful=words.filter { it !in filler }
                val short = mapOf("f" to SafariBiome.FOREST, "c" to SafariBiome.CAVERN,
                    "i" to SafariBiome.ICY, "h" to SafariBiome.HAUNTED)
                val single=short[body.trim()]
                if(single==null && meaningful.size!=1 && "hotspot" !in words) return null
                val biome = single ?: aliases.filter { (biome, names) ->
                    meaningful.any { word -> word == "for" && meaningful.size==1 && biome == SafariBiome.FOREST ||
                        names.any { close(word, it) } }
                }.keys.singleOrNull() ?: return null
                return Report(chat.sender, biome, tag?.groupValues?.get(1)?.lowercase() in setOf("sm","su","safariutils"))
            }
            val words=body.replace(Regex("[^a-z\\s]")," ").trim().split(Regex("\\s+")).filter { it !in filler }
            if(words.size!=1) return null
            val matches=aliases.filterValues { names -> names.any { close(words.single(),it) } }.keys
            val biome=matches.singleOrNull() ?: return null
            return Report(chat.sender,biome,tag?.groupValues?.get(1)?.lowercase() in setOf("sm","su","safariutils"))
        }

        fun parse(raw: String): Pair<String,SafariBiome>? {
            val report=parseReport(raw) ?: return null
            return report.sender to report.biome
        }
        fun manualReportWindow(startedAt: Long?, now: Long) =
            startedAt != null && now - startedAt in 0L..30_000L
        private fun identity(name: String,uuids: Map<String,String>)=uuids[name.lowercase()]?.replace("-","")?.lowercase() ?: name.lowercase()
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
