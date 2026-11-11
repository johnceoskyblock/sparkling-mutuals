package net.johnceo.sparklingmutuals.sync

import com.google.gson.*
import net.johnceo.sparklingmutuals.safari.SafariBiome

/** SafariUtils aggregate v2/v3 and hotspots-v1. */
internal object PartySyncProtocol {
    const val AGGREGATE_VERSION = 3
    const val AGGREGATE_SIZE = 39
    const val HELD_COINS = 38
    private val lobbyId = Regex("[A-Za-z0-9_-]{1,64}")
    private val miniLobby = Regex("mini([0-9][A-Za-z0-9]*)")

    fun canonicalLobby(raw: String): String? {
        if (!lobbyId.matches(raw)) return null
        val normalized=raw.lowercase()
        val mini = miniLobby.matchEntire(normalized) ?: return normalized
        return "m${mini.groupValues[1]}"
    }
    const val MAX_SEQUENCE = 9_007_199_254_740_991L
    fun uuid(raw: String) = raw.replace("-", "").lowercase().takeIf { it.matches(Regex("[0-9a-f]{32}")) }
    fun compact(raw: String) = raw.takeIf { it.matches(Regex("[0-9a-f]{32}")) }
    fun integer(value: JsonElement?, min: Long = 0, max: Long = MAX_SEQUENCE): Long? =
        value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber && it.toString().matches(Regex("-?[0-9]+")) }
            ?.asString?.toLongOrNull()?.takeIf { it in min..max }
    fun text(value: JsonElement?, limit: Int = 128) = value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString?.takeIf { it.length <= limit }
    fun bool(value: JsonElement?) = value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean
    fun numbers(kind: String, payload: String): IntArray? {
        if (payload.length > 3072) return null
        val parts = payload.split(',')
        val size = when(kind) { "A" -> if (parts.size == AGGREGATE_SIZE) AGGREGATE_SIZE else 38; "N" -> 1; "S" -> 4; "U" -> 6; else -> return null }
        if (parts.size != size) return null
        val values = parts.map { if (!it.matches(Regex("-?[0-9]+"))) return null else it.toIntOrNull() ?: return null }.toIntArray()
        val version = if (size == AGGREGATE_SIZE) AGGREGATE_VERSION else 2
        val bounds = when(kind) {
            "A" -> List(9) { 0..256 } + List(4) { 0..64 } + List(4) { 0..256 } + listOf(-1..2,0..64) +
                List(4) { 0..64 } + listOf(0..7,0..4) + List(8) { 0..1 } +
                listOf(version..version,0..31,0..7,0..65535,0..65535) +
                if (version == AGGREGATE_VERSION) listOf(0..64) else emptyList()
            "N" -> listOf(0..8191)
            "S" -> listOf(0..1073741823,0..127,0..1073741823,0..127)
            else -> listOf(0..256,0..256,0..256,0..256,-1..2,0..64)
        }
        if(!values.indices.all { index -> values[index] in bounds[index] }) return null
        if(kind=="A" && values[18]>0 && values[17]<0 || kind=="U" && values[5]>0 && values[4]<0) return null
        return values
    }
    fun welcome(root: JsonObject, self: String, roster: List<String>): Map<String,String>? {
        val tags = root.get("roster")?.takeIf { it.isJsonArray }?.asJsonArray?.map { text(it) ?: return null } ?: return null
        if (tags.size != roster.size || tags.toSet().size != tags.size || tags.any { it.isBlank() }) return null
        val map = tags.zip(roster).toMap()
        return map.takeIf { it[text(root.get("self"))] == self }
    }
    fun envelope(sequence: Long, kind: String, payload: String, catchUpOnly: Boolean = false): String {
        require(sequence in 1..MAX_SEQUENCE && payload.length <= 3072)
        return JsonObject().apply {
            addProperty("sequence",sequence); addProperty("kind",kind); addProperty("payload",payload)
            if (kind == "A" && catchUpOnly) addProperty("catchUpOnly",true)
        }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= 4096) }
    }
    fun biome(raw: String?) = SafariBiome.entries.firstOrNull { it.name == raw }
    val codes = mapOf("fd" to SafariBiome.FOREST,"cd" to SafariBiome.CAVERN,"id" to SafariBiome.ICY,"hd" to SafariBiome.HAUNTED)
    val species = listOf("Cavernfish","Flitter","Shyworm","Driftling","Chuckwalla","Rockmite","Scrappy","Snoozle","Gemzie",
        "Strongarm","Tepid","Polaris","Shuddersquid","Billygoat","Mantis Shrimp","Nozzlenose","Troodon","Wumpa",
        "Areita","Bloodbat","Duplico","Gazer","Litterbug","Solsnatcher","Gimmiegold","Hideonwall","Hideyho","Doomspiral",
        "Foxtrot","Bluebird","Honeybug","Treefrog","Woodchucker","Fluffling","Hideonfloor","Parakeet","Macaw")
}

/** All state belongs to one visit, independent of socket generations. */
internal class PartySyncState(val self: String, roster: Set<String>) {
    private val bootstrap=PartySyncBootstrap()
    private var provider="primary"
    private var providerGeneration: String?=null
    private val instructed=linkedMapOf<String,SafariBiome>()
    private val assignmentClients=mutableSetOf(self)
    private val instructionGrace=mutableMapOf<String,Long>()
    fun assignmentClient(player: String) { if(player in roster) assignmentClients.add(player) }
    fun knownAssignmentClients()=assignmentClients.toSet()
    /** The roster contains non-mod players too; only positive client evidence suppresses chat. */
    fun assignmentInstructions(now: Long): List<Notification> {
        val run=serverRun ?: return emptyList()
        if(!authority || conflict) return emptyList()
        locks.keys.forEach { instructionGrace.putIfAbsent(it,now) }
        val requested=notifications.associateBy { it.player }
        return locks.mapNotNull { (player,biome) ->
            if(player in assignmentClients || wasInstructed(player,biome) ||
                now-(instructionGrace[player] ?: now)<2_000) null
            else requested[player] ?: Notification("$run:$player",player,biome)
        }
    }
    var primaryFairness: JsonObject?=null; private set
    fun transitionAuthority(next: String, generation: String): Boolean {
        if(next==provider)return providerGeneration==generation
        if(provider!="primary" || next!="backup" || generation.isBlank())return false
        provider=next;providerGeneration=generation;serverRun=null;revision=-1;notifications=emptyList()
        inventories.clear();inventoriesCurrent=false
        return true
    }
    fun bootstrapFacts()=bootstrap.frames()
    fun localBootstrap(values: IntArray,nests: Int,sparkling: IntArray) {
        bootstrap.observe(self,"A",values);bootstrap.observe(self,"N",intArrayOf(nests));bootstrap.observe(self,"S",sparkling)
    }
    fun importBootstrap(value: JsonObject): Boolean {
        val validated=bootstrap.accept(value,roster) ?: return false
        nestMask=nestMask or validated.get("nests").asInt
        val f=validated.getAsJsonArray("facts")
        PartySyncBootstrap.indices.forEach { i -> facts[i]=if(i==24) maxOf(facts[i],f[i].asInt) else facts[i] or f[i].asInt }
        val c=validated.getAsJsonArray("caught");for(i in 0..1)sparkling[i+2]=sparkling[i+2] or c[i].asInt
        return true
    }
    fun markInstructed(player: String,biome: SafariBiome) { instructed[player]=biome }
    fun wasInstructed(player: String,biome: SafariBiome)=instructed[player]==biome
    fun instructedTargets()=instructed.toMap()
    var roster=roster; private set
    fun expand(next: Set<String>) { require(next.size<=4 && next.containsAll(roster));if(next!=roster)primaryFairness=null;roster=next }
    var sequence = 0L; private set
    fun nextSequence() = (++sequence).also { check(it <= PartySyncProtocol.MAX_SEQUENCE) }
    private val received = mutableMapOf<String,LinkedHashSet<Long>>()
    private val latest = mutableMapOf<String,Long>()
    private val latestFeeder = mutableMapOf<String,Long>()
    val inventories = mutableMapOf<String,IntArray>()
    val biomeReports = linkedSetOf<Pair<String,String>>()
    var inventoriesCurrent = false
    fun invalidateInventory() {
        inventories.clear()
        inventoriesCurrent=false
        usage?.let { it[4]=-1; it[5]=0 }
        feederReporter=null
    }
    var nestMask = 0; private set
    val sparkling = IntArray(4)
    /** Negative evidence only. Discovery bits carry no observation time. */
    internal fun capturedSpecies(): Set<String> = PartySyncProtocol.species.filterIndexed { index, _ ->
        sparkling[if (index < 30) 2 else 3] and (1 shl (index % 30)) != 0
    }.toSet()
    val facts = IntArray(38)
    var usage: IntArray? = null; private set
    private var feederReporter: String? = null
    var authority = false; private set
    var conflict = false; private set
    var serverRun: String? = null; private set
    private var revision = -1L
    val locks = linkedMapOf<String,SafariBiome>()
    val reports = linkedMapOf<String,SafariBiome>()
    val hotspotWins = linkedMapOf<String,Int>()
    val sentNotifications = mutableSetOf<String>()
    data class Notification(val id: String,val player: String,val biome: SafariBiome)
    var notifications = emptyList<Notification>(); private set
    var leader: String? = null; private set
    /** Protocol 2 guarantees server ordering across replaced sender sockets, whose counters may restart. */
    fun receiveOrdered(sender: String, kind: String, payload: String, inventoryFresh: Boolean): Boolean =
        receive(sender,(received[sender]?.maxOrNull() ?: 0)+1,kind,payload,inventoryFresh)
    fun receive(sender: String, sequence: Long, kind: String, payload: String, inventoryFresh: Boolean = true): Boolean {
        if (sender !in roster || sender == self && kind != "B" || sequence !in 1..PartySyncProtocol.MAX_SEQUENCE) return false
        val values = if(kind == "B") { if(payload !in PartySyncProtocol.codes) return false; null }
            else PartySyncProtocol.numbers(kind,payload) ?: return false
        val seen = received.getOrPut(sender) { linkedSetOf() }
        if (!seen.add(sequence)) return false
        if(seen.size > 256) seen.remove(seen.first())
        if(values!=null)bootstrap.observe(sender,kind,values)
        when(kind) {
            "B" -> biomeReports.add(sender to payload)
            "N" -> nestMask = nestMask or values!![0]
            "S" -> mergeSparklings(values!!)
            "U" -> mergeUsage(values!!,sender,freshFeeder(sender,sequence))
            "A" -> {
                if(sequence > (latest[sender] ?: -1)) {
                    latest[sender]=sequence
                    if(inventoryFresh) { inventories[sender]=values!!; inventoriesCurrent=true }
                    else inventories.remove(sender)
                }
                val v=values!!
                for(i in listOf(23,25,26,27,28,29,30,31,32,34,35,36,37)) facts[i] = facts[i] or v[i]
                facts[24]=maxOf(facts[24],v[24])
                val usageValues=v.copyOfRange(13,19)
                if(!inventoryFresh) { usageValues[4]=-1; usageValues[5]=0 }
                mergeUsage(usageValues,sender,inventoryFresh && freshFeeder(sender,sequence))
            }
        }
        assignmentClient(sender)
        return true
    }
    fun receiveRoomSparklings(payload: String): Boolean {
        val values = PartySyncProtocol.numbers("S", payload) ?: return false
        mergeSparklings(values)
        bootstrap.observe(self,"S",values,"room")
        return true
    }
    private fun mergeSparklings(values: IntArray) {
        for (i in sparkling.indices) sparkling[i] = sparkling[i] or values[i]
    }
    private fun freshFeeder(sender: String,sequence: Long): Boolean {
        val previous=latestFeeder[sender] ?: -1
        if(sequence>previous) latestFeeder[sender]=sequence
        return sequence>previous
    }
    private fun mergeUsage(next: IntArray,sender: String,replaceFeeder: Boolean) {
        val old=usage
        if(old==null || next[0]>old[0]) {
            usage=next.copyOf()
            feederReporter=sender
        }
        else if(next[0]==old[0]) {
            for(i in 1..3) old[i]=minOf(old[i],next[i])
            if(replaceFeeder && (next[5]>0 || old[5]>0 && feederReporter==sender)) {
                old[4]=next[4];old[5]=next[5];feederReporter=sender
            }
        }
    }
    fun importLocks(next: Map<String,SafariBiome>): Boolean {
        if(next.keys.any { it !in roster } || next.values.toSet().size!=next.size ||
            next.any { (id,biome) -> locks[id]?.let { it!=biome } == true || locks.any { it.key!=id && it.value==biome } }) {
            conflict=true; return false
        }
        locks.putAll(next); return true
    }
    fun hotspots(payload: JsonObject): Boolean = runCatching {
        if(PartySyncProtocol.integer(payload.get("version"),1,1)==null) return false
        val run=PartySyncProtocol.text(payload.get("run"),128)?.takeIf { it.isNotBlank() } ?: return false
        val rev=PartySyncProtocol.integer(payload.get("revision")) ?: return false
        if(serverRun!=null && serverRun!=run) { conflict=true; return false }
        if(rev<revision) return false
        val assignments=payload.get("assignments")?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        val next=assignments.entrySet().associate { (id,value) ->
            if(PartySyncProtocol.compact(id)==null || id !in roster) return false
            id to (PartySyncProtocol.biome(PartySyncProtocol.text(value)) ?: return false)
        }
        val fair=payload.get("fairness")?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        if(fair.entrySet().any { it.key !in roster || PartySyncProtocol.integer(it.value,0,1_000_000)==null }) return false
        val nextLeader=payload.get("leader").let { if(it==null || it.isJsonNull) null else PartySyncProtocol.text(it)?.takeIf { id -> id in roster } ?: return false }
        val notices=payload.get("notifications")?.takeIf { it.isJsonArray && it.asJsonArray.size()<=4 }?.asJsonArray ?: return false
        val pending=notices.map { element ->
            val n=element.takeIf { it.isJsonObject }?.asJsonObject ?: return false
            val id=PartySyncProtocol.text(n.get("player"))?.takeIf { it in roster } ?: return false
            val biome=PartySyncProtocol.biome(PartySyncProtocol.text(n.get("biome"))) ?: return false
            val key=PartySyncProtocol.text(n.get("id"),200)?.takeIf { it=="$run:$id" } ?: return false
            if(next[id]!=biome && locks[id]!=biome) return false
            Notification(key,id,biome)
        }
        val hasConflict=PartySyncProtocol.bool(payload.get("conflict")) ?: return false
        PartySyncProtocol.bool(payload.get("initialDone")) ?: return false
        // Notification eligibility changes after the display grace period without a new allocation.
        // Accept that control-only refresh, but never change durable state at the same revision.
        if(rev==revision) {
            val counts=fair.entrySet().associate { it.key to it.value.asInt }
            if(next!=locks || counts!=hotspotWins || nextLeader!=leader || hasConflict!=conflict || pending==notifications) return false
        }
        if(!importLocks(next)) return false
        serverRun=run; revision=rev; authority=true; conflict=conflict || hasConflict; leader=nextLeader
        if(provider=="primary")primaryFairness=JsonObject().apply {
            addProperty("run",run);addProperty("revision",rev);add("counts",fair.deepCopy())
        }
        hotspotWins.clear();fair.entrySet().forEach { hotspotWins[it.key]=it.value.asInt }
        notifications=if(conflict) emptyList() else pending
        true
    }.getOrDefault(false)
}
