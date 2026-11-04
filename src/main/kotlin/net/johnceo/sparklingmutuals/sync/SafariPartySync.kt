package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonObject
import net.johnceo.sparklingmutuals.debug.SafariDeveloper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.HotspotAssignmentAlert
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput
import net.johnceo.sparklingmutuals.safari.automation.GemzieGem
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.resources.*
import net.johnceo.sparklingmutuals.safari.structures.*
import net.johnceo.sparklingmutuals.history.RunTally
import net.johnceo.sparklingmutuals.history.TicketCounter
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import org.slf4j.LoggerFactory

/** Adapter between per-visit shared observations and Mutuals' existing mode-aware ledgers. */
object SafariPartySync {
    private val transport=PartySyncTransport()
    private val logger=LoggerFactory.getLogger("sparkling-mutuals/party-sync")
    private var state: PartySyncState?=null
    private var lobby: String?=null
    private var world: Any?=null
    private var began=0L
    private var now=0L
    private var transferRevision: Long? = null
    private var ticketed = false
    private var ticketWindowLocked = false
    private val transferMessage = Regex("^Sending to server [A-Za-z0-9_-]+\\.\\.\\.$")
    private var matchingParty=false
    private var sampleAt=0L
    private var helloAt=0L
    private var lastHello=""
    private val local=IntArray(PartySyncProtocol.AGGREGATE_SIZE).apply {
        this[17] = -1
        this[33] = PartySyncProtocol.AGGREGATE_VERSION
    }
    private val localSparkling=IntArray(4)
    private var localNests=0
    private var lastNests=-1
    private var lastSparkling=""
    private var lastAggregate=""
    private var produced=""
    private var liveFingerprint=""
    private var due=0L
    private var pendingLive=false
    private var seenWalls=0
    private var seenTroodon=0
    private val pendingMoundHits = mutableMapOf<Triple<Int, Int, Int>, Long>()
    private var lastRenderSummary = ""
    private val openedWalls=mutableMapOf<Int,Long>()
    private var appliedRun: SafariRun?=null
    private val appliedReports=mutableSetOf<Pair<String,String>>()
    private var lastInstructionAt=0L
    private val acknowledgments=linkedSetOf<String>()
    private var lastRoster=emptySet<String>()
    private val reportQueue = linkedMapOf<String, Boolean>()
    private val submittedReports = mutableMapOf<String, Boolean>()
    private val pendingReports=linkedMapOf<String,SafariBiome>()
    private val notifiedSparks=mutableSetOf<String>()
    private val itemOrder=listOf(FloorDropType.LIME,FloorDropType.ORANGE,FloorDropType.PURPLE,FloorDropType.ICEBREAKER,
        FloorDropType.COIN,FloorDropType.INCENSE,FloorDropType.BERRY,FloorDropType.WORM,FloorDropType.SEEDS)
    private val birds=listOf("Bluebird","Parakeet","Macaw")
    private val food=listOf("Bag of Seeds","Wriggleworm","Yogi Berry")
    private val discoveryReports=linkedMapOf<String,SafariBiome>()
    fun register() {
        transport.onReady={
            lastNests=-1;lastSparkling="";lastAggregate="";lastHello="";helloAt=0;pendingReports.clear()
            discoveryReports.forEach { (id,biome) -> pendingReports[id]=biome }
            submittedReports.forEach { (code, chat) -> reportQueue.putIfAbsent(code, chat) }
            mark(false);pendingLive=true
        }
        transport.onMessage=::receive
        ClientReceiveMessageEvents.GAME.register { message,overlay ->
            if(!overlay) Minecraft.getInstance().execute {
                observeReport(message.string)
                SafariMessages.lines(message.string).forEach(::observeChat)
            }
        }
        ClientReceiveMessageEvents.CHAT.register { message,_,_,_,_ ->
            Minecraft.getInstance().execute { observeReport(message.string) }
        }
    }
    internal fun moundAttack(entity: Entity) {
        if (!SafariIsland.onIsland || SafariAssist.biome != SafariBiome.CAVERN ||
            transferRevision == SafariIsland.locationRevision) return
        val pos = entity.blockPosition()
        val observed = Triple(pos.x, pos.y, pos.z)
        val site = PartySyncSites.moundAt(observed)
        if (site == null || !SafariStructures.isMoundAttack(entity, site, Minecraft.getInstance())) {
            SafariDeveloper.event("Unmatched cavern attack · ${entity.type} · $observed", showOnHud = false)
            return
        }
        val hitAt = System.currentTimeMillis()
        val previous = pendingMoundHits.put(site, hitAt)
        if (previous == null || hitAt - previous > 2_000)
            SafariDeveloper.event("Local mound hit · slot ${PartySyncSites.mounds.indexOf(site)} · $observed → $site")
    }
    private fun roster(client: Minecraft): List<String>? {
        val self=client.player?.uuid?.toString() ?: return null
        return PartyManager.withLocal(self)?.map { PartySyncProtocol.uuid(it) ?: return null }?.distinct()?.sorted()?.takeIf { it.size in 1..4 }
    }
    private fun initialize(client: Minecraft) {
        if(transferRevision == SafariIsland.locationRevision || !SafariIsland.onIsland || client.player==null || client.level==null) return
        transferRevision = null
        val server = SafariIsland.serverName?.let(PartySyncProtocol::canonicalLobby) ?: return
        if(lobby!=null && (!server.equals(lobby,true) || world!==client.level)) reset()
        if(lobby==null) { lobby=server;world=client.level;began=System.currentTimeMillis() }
        val wasMatching=matchingParty
        matchingParty=false
        val members=roster(client) ?: return
        val self=PartySyncProtocol.uuid(client.player!!.uuid.toString()) ?: return
        val active=state
        if(active==null && members.size>1) state=PartySyncState(self,members.toSet())
        else if(active!=null && members.toSet().containsAll(active.roster) && members.size>active.roster.size) {
            val present=client.connection?.onlinePlayers?.mapNotNull { PartySyncProtocol.uuid(it.profile.id.toString()) }?.toSet().orEmpty()
            if((members.toSet()-active.roster).all { it in present }) {
                // Keep sequence and observed state through a confirmed, present roster expansion.
                active.expand(members.toSet())
            }
        }
        lastRoster=members.toSet()
        matchingParty=state?.roster==lastRoster
        if(matchingParty && !wasMatching) lastHello=""
    }
    fun tick(client: Minecraft) {
        now=System.currentTimeMillis()
        if(transferRevision == SafariIsland.locationRevision) return
        if(!SafariIsland.onIsland || client.level == null || client.connection == null) { if(lobby!=null) reset();return }
        initialize(client)
        val current=state ?: return
        val server=lobby ?: return
        val run=SafariTracking.ledger.current
        if (run?.entry?.admission != null || run?.entry?.hotspot != null) ticketed = true
        transport.tick(PartySyncTransport.Context(server,current.self,current.roster.sorted()),now)
        current.inventoriesCurrent=transport.ready
        if(ticketed) run?.entry?.hotspot?.let { report(current.self,it) }
        SparklingEncounters.ticketReminder(!ticketed && !ticketWindowLocked && now-began<30_000 &&
            (0..1).any { word ->
                current.sparkling[word] and (current.sparkling[word+2] or localSparkling[word+2]).inv() != 0
            })
        if(transport.ready && transport.hotspotCapability && transport.biomeCapability && matchingParty) {
            val hello=hello(client,current)
            if(hello!=lastHello || now-helloAt>=15_000) {
                if(send("H",hello)) { lastHello=hello;helloAt=now }
            }
            if(lastHello.isNotEmpty()) {
                acknowledgments.firstOrNull()?.let { id -> if(sendH("ack") { addProperty("id",id) }) acknowledgments.remove(id) }
                val entry=pendingReports.entries.firstOrNull { ticketed || it.key != current.self }
                if(entry!=null && sendH("report") { addProperty("player",entry.key);addProperty("biome",entry.value.name) }) pendingReports.remove(entry.key)
            }
        }
        if(ticketed && now>=sampleAt && run!=null) { sampleAt=now+500;sample(client,run);apply(run) }
        if(transport.ready) {
            if(localNests!=lastNests && send("N",localNests.toString())) lastNests=localNests
            val sparkle=localSparkling.joinToString(",")
            if(sparkle!=lastSparkling && send("S",sparkle)) lastSparkling=sparkle
            if(ticketed && run!=null && due!=0L && now>=due) {
                val value=local.joinToString(",")
                if (value == lastAggregate || send("A", value, !pendingLive)) {
                    if (value != lastAggregate && SafariDeveloper.enabled) {
                        val changes = aggregateChanges(PartySyncProtocol.numbers("A", lastAggregate), local)
                        SafariDeveloper.event("Outgoing objectives #${current.sequence} · ${changes.ifEmpty { "objective update" }}")
                    }
                    lastAggregate = value
                    due = 0
                    pendingLive = false
                }
            }
            if (matchingParty && transport.biomeCapability && due == 0L && run != null) reportQueue.entries.firstOrNull { (code, _) ->
                val biome = PartySyncProtocol.codes.getValue(code)
                !run.sharedSuppliesPending(biome) && !run.hasUnusedStartingForBiome(biome)
            }?.let { (code, chat) ->
                if (current.biomeReports.none { it.second == code } && send("B", code)) {
                    submittedReports[code] = chat
                    reportQueue.remove(code)
                } else if (current.biomeReports.any { it.second == code }) reportQueue.remove(code)
            }
        }
        if(ConfigManager.hotspotAnnouncer && current.authority && !current.conflict && matchingParty && transport.ready &&
            PartyManager.isLeader(client.player!!.uuid.toString()) && current.leader==current.self && now-lastInstructionAt>=1500) {
            current.notifications.firstOrNull { it.id !in current.sentNotifications }?.let { notice ->
                val name=name(client,notice.player) ?: return@let
                if (!PartyManager.sendPartyChat(client, "$name you are assigned ${notice.biome.label} biome!")) return@let
                current.sentNotifications.add(notice.id);lastInstructionAt=now
                acknowledgments.add(notice.id)
            }
        }
    }
    /** Authority stays adopted across an outage. It never starts a competing fallback allocator. */
    val backendCoordination get() = state?.authority==true
    val waitingForAuthority get() = !backendCoordination && shouldWait()
    val pauseHotspotChat get() = backendCoordination || shouldWait()
    private fun shouldWait(): Boolean {
        if(!SafariIsland.onIsland) return false
        val start=began.takeIf { it>0 } ?: return true
        return System.currentTimeMillis()-start<7000 && lastRoster.size!=1
    }
    fun assignment(): SafariBiome? = state?.takeUnless { it.conflict }?.let { it.locks[it.self] }
    fun finalized(plan: Map<String,SafariBiome>) {
        val current=state ?: return
        val client=Minecraft.getInstance()
        val ids=current.roster.associateBy { name(client,it)?.lowercase() }
        val mapped=plan.mapNotNull { (name,biome) -> ids[name.lowercase()]?.let { it to biome } }.toMap()
        if(mapped.size==plan.size && current.importLocks(mapped)) lastHello=""
    }
    fun report(id: String,biome: SafariBiome) {
        val current=state ?: return
        if(id !in current.roster || discoveryReports.containsKey(id)) return
        discoveryReports[id]=biome;current.reports[id]=biome;pendingReports[id]=biome
    }
    private fun observeReport(raw: String) {
        if(!SafariIsland.onIsland || transferRevision == SafariIsland.locationRevision) return
        val parsed=HotspotAssignments.parseReport(raw) ?: return
        val client=Minecraft.getInstance()
        state?.roster?.firstOrNull { name(client,it).equals(parsed.sender,true) }?.let { report(it,parsed.biome) }
    }
    private fun hello(client: Minecraft,current: PartySyncState): String = JsonObject().apply {
        addProperty("version",1);addProperty("action","hello");addProperty("display",ConfigManager.hotspotAnnouncer)
        addProperty("ticketed",ticketed)
        addProperty("leader",PartyManager.isLeader(client.player!!.uuid.toString()))
        val present=com.google.gson.JsonArray()
        // The shared server tab list establishes presence on this Safari island, not party membership alone.
        client.connection?.onlinePlayers?.mapNotNull { PartySyncProtocol.uuid(it.profile.id.toString()) }
            ?.filter { it in current.roster }?.sorted()?.forEach(present::add)
        add("present",present)
        add("locked",JsonObject().apply { current.locks.forEach { (id,biome) -> addProperty(id,biome.name) } })
    }.toString()
    private fun sendH(action: String,body: JsonObject.()->Unit={}) = send("H",JsonObject().apply {
        addProperty("version",1);addProperty("action",action);body()
    }.toString())
    private fun send(kind: String,payload: String,catchUpOnly: Boolean=false): Boolean {
        val current=state ?: return false
        if(!transport.ready) return false
        return transport.send(current.nextSequence(),kind,payload,catchUpOnly)
    }
    fun completion(code: String, chat: Boolean): Boolean {
        val coordinated = backendCoordination || transport.ready && transport.biomeCapability && matchingParty
        if (code in PartySyncProtocol.codes && state?.biomeReports?.none { it.second == code } == true)
            reportQueue[code] = chat && coordinated
        return coordinated
    }
    fun found(species: String): Boolean {
        val i=PartySyncProtocol.species.indexOf(species)
        if(i<0 || !SafariIsland.onIsland || transferRevision == SafariIsland.locationRevision) return false
        val word=if(i<30) 0 else 1; val bit=1 shl (if(i<30) i else i-30)
        val alreadyShared = localSparkling[word] and bit == 0 &&
            state?.sparkling?.let { (it[word] or it[word+2]) and bit != 0 } == true
        localSparkling[word]=localSparkling[word] or bit
        if (alreadyShared) notifiedSparks.add(species)
        return species in notifiedSparks
    }
    private fun caught(species: String) {
        if (transferRevision == SafariIsland.locationRevision) return
        found(species)
        val i=PartySyncProtocol.species.indexOf(species);if(i<0) return
        val word=if(i<30) 2 else 3;val bit=1 shl (if(i<30) i else i-30)
        localSparkling[word]=localSparkling[word] or bit
    }
    private fun observeChat(text: String) {
        if(!SafariIsland.onIsland) return
        if (transferMessage.matches(text)) {
            reset()
            transferRevision = SafariIsland.locationRevision
            return
        }
        if (transferRevision == SafariIsland.locationRevision) return
        initialize(Minecraft.getInstance());observeReport(text)
        if (TicketCounter.isTicket(text)) ticketed = true
        if (text == "[NPC] Safari Manager: Dude, if you're not gonna pay, you're not gonna play.") ticketWindowLocked = true
        // Public encounter messages are still processed locally; these uploads only preserve catch-up facts.
        var public=false;var changed=false
        fun flag(i: Int) { if(local[i]==0) { local[i]=1;changed=true } }
        if(text.startsWith("A rumbling sound can be heard")) { flag(25);public=true }
        if(text.startsWith("Something stirs in the Haunted Biome") || text.startsWith("Your ritual")) { flag(26);public=!text.startsWith("Your ritual") }
        if(text.startsWith("The Doomspiral retreats back underground")) { flag(26);flag(30);public=true }
        if(text.startsWith("The Wumpa has awoken")) { flag(28);public=true }
        if(text.contains("fainted by a Wumpa") && text.endsWith("lost some of your items!")) { flag(28);flag(32) }
        if(text.startsWith("You used the Soothing Incense to light the candle")) { local[24]=(local[24]+1).coerceAtMost(4);changed=true }
        SafariCatch.parse(text)?.let { capture ->
            birds.indexOf(capture.critter.name).takeIf { it>=0 }?.let { local[14+it]=(local[14+it]-1).coerceAtLeast(0) }
            when(capture.critter.name) {
                "Gemzie" -> { flag(25);flag(31);public=true }
                "Doomspiral" -> { flag(26);flag(27);public=true }
                "Wumpa" -> { flag(28);flag(29);public=true }
            }
            if(capture.sparkling) caught(capture.critter.name)
        }
        RunTally.sparkling.matchEntire(text)?.let { caught(it.groupValues[2].trim()) }
        Regex("^(A|Two) (Bluebirds?|Parakeets?|Macaws?) (?:was|were) attracted to the Birdfeeder!$").matchEntire(text)?.let { spawn ->
            val species=spawn.groupValues[2].removeSuffix("s")
            val index=birds.indexOf(species)
            if(index>=0) { local[14+index]=(local[14+index]+if(spawn.groupValues[1]=="Two") 2 else 1).coerceAtMost(256);changed=true }
        }
        if(SafariMounds.outcome(text)!=null) {
            val outcomeAt = System.currentTimeMillis()
            pendingMoundHits.entries.removeIf { outcomeAt - it.value !in 0..2_000 }
            val site = pendingMoundHits.keys.singleOrNull()
            if (site != null) {
                SafariTracking.ledger.current?.moundSurvey?.confirmBreak(site)
                SafariDeveloper.event("Confirmed mound slot ${PartySyncSites.mounds.indexOf(site)} from break chat")
            } else {
                net.johnceo.sparklingmutuals.debug.SafariDeveloper.event("Mound break unassigned · ${pendingMoundHits.size} recent sites")
            }
            pendingMoundHits.clear()
        }
        if(local[26]!=0) local[24]=4
        if(changed) mark(public)
    }
    private fun mark(public: Boolean) {
        if(due==0L) due=System.currentTimeMillis()+500
        if(!public) pendingLive=true
    }
    private fun sample(client: Minecraft,run: SafariRun) {
        val before=local.joinToString(",")
        val includeStarting = ConfigManager.announceStartingItems
        itemOrder.forEachIndexed { i,type -> local[i]=run.resources.coordinatedDiscovered(type, includeStarting).coerceIn(0,256) }
        local[9]=listOf(FloorDropType.SEEDS, FloorDropType.WORM, FloorDropType.BERRY)
            .sumOf(run.resources::floorPickupCount).coerceIn(0,64)
        food.forEachIndexed { i,label -> local[10+i]=run.birds.held(label).coerceIn(0,64) }
        local[13]=run.birds.spawnEvents.coerceIn(0,256)
        // Reuse the cached ESP observations; relay packets never trigger world scans.
        val observed=SafariEsp.observations()
        if (SafariDeveloper.enabled) {
            val biome = SafariAssist.biome
            val critters = observed.critters.filter { it.biome == biome && !it.mound }
                .groupingBy { it.species }.eachCount().toSortedMap()
            val summary = "Local render · ${biome?.label ?: "Safari"} · critters=$critters" +
                " · mound entities=${SafariStructures.nearbyMounds} · floor markers=${observed.floorDrops.size}"
            if (summary != lastRenderSummary) {
                lastRenderSummary = summary
                SafariDeveloper.event(summary, showOnHud = false)
            }
        }
        birds.forEachIndexed { i,species -> local[14+i]=maxOf(local[14+i],observed.critters.count { it.species==species }).coerceIn(0,256) }
        local[17]=food.indexOf(run.birds.localFeederFood).takeIf { run.birds.localFeederCount>0 } ?: -1
        local[18]=run.birds.localFeederCount.coerceIn(0,64)
        listOf(FloorDropType.LIME,FloorDropType.ORANGE,FloorDropType.PURPLE,FloorDropType.INCENSE).forEachIndexed { i,type -> local[19+i]=run.resources.heldCount(type).coerceIn(0,64) }
        local[PartySyncProtocol.HELD_COINS] = run.resources.coordinatedHeld(FloorDropType.COIN, includeStarting).coerceIn(0, 64)
        listOf(GemzieGem.LIME,GemzieGem.ORANGE,GemzieGem.PURPLE).forEachIndexed { i,gem -> if(gem in run.gems.locallyPlaced) local[23]=local[23] or (1 shl i) }
        SafariStructures.localWallStates.forEachIndexed { i,wall ->
            if(wall==WallState.INTACT) seenWalls=seenWalls or (1 shl i)
            val pos=SafariStructures.snooperPositions[i]
            val resident=observed.critters.any { it.species=="Snoozle" && (it.x-pos.x)*(it.x-pos.x)+(it.z-pos.z)*(it.z-pos.z)<=25 }
            if(wall==WallState.BROKEN && seenWalls and (1 shl i)!=0 && !resident) {
                val opened=openedWalls.getOrPut(i) { now }
                if(now-opened>=1500) local[34]=local[34] or (1 shl i)
            } else openedWalls.remove(i)
        }
        PartySyncSites.troodon.forEachIndexed { i,p ->
            val pos=net.minecraft.core.BlockPos(p.first,p.second,p.third)
            if(client.level?.isLoaded(pos)==true) {
                if(client.level!!.getBlockState(pos).isAir) {
                    val resident=observed.critters.any { it.species=="Troodon" && (it.x-pos.x)*(it.x-pos.x)+(it.z-pos.z)*(it.z-pos.z)<=25 }
                    if(seenTroodon and (1 shl i)!=0 && !resident) {
                        val opened=openedWalls.getOrPut(5+i) { now }
                        if(now-opened>=1500) local[35]=local[35] or (1 shl i)
                    } else openedWalls.remove(5+i)
                }
                else seenTroodon=seenTroodon or (1 shl i)
            }
        }
        localNests=localNests or PartySyncSites.mask(PartySyncSites.nests,BeeNests.locallyResolved)
        val moundMask = PartySyncSites.mask(PartySyncSites.mounds, run.moundSurvey.locallyResolved)
        local[36] = local[36] or (moundMask and 0xFFFF)
        local[37] = local[37] or (moundMask ushr 16)
        val value=local.joinToString(",")
        // Ignore public encounter flags in this comparison; any inventory/world/feeder change takes live precedence.
        val live=local.filterIndexed { i,_ -> i !in 25..32 }.joinToString(",")
        if(value!=produced || before!=value) { if(live!=liveFingerprint) pendingLive=true;produced=value;liveFingerprint=live;mark(!pendingLive) }
    }
    private fun receive(root: JsonObject,mapping: Map<String,String>) {
        val current=state ?: return
        when(PartySyncProtocol.text(root.get("type"))) {
            "sparklings" -> if (transport.roomSparklings) {
                val payload = PartySyncProtocol.text(root.get("payload"), 64) ?: return
                if (current.receiveRoomSparklings(payload)) sharedSparklings(current)
            }
            "hotspots" -> if(transport.hotspotCapability && transport.biomeCapability && matchingParty) {
                val payload=root.get("payload")?.takeIf { it.isJsonObject }?.asJsonObject ?: return
                if(!current.hotspots(payload)) logger.debug("Party sync rejected hotspot snapshot")
            }
            "state" -> {
                val sender=mapping[PartySyncProtocol.text(root.get("sender"))] ?: return
                val sequence=PartySyncProtocol.integer(root.get("sequence"),1) ?: return
                val kind=PartySyncProtocol.text(root.get("kind"),1) ?: return
                if(kind=="S" && transport.roomSparklings) return
                val payload=PartySyncProtocol.text(root.get("payload"),3072) ?: return
                if(kind=="B" && !transport.biomeCapability) return
                val previous = if (SafariDeveloper.enabled && kind == "A") current.inventories[sender] else null
                val previousNests = current.nestMask
                if (!current.receive(sender, sequence, kind, payload)) return
                if (SafariDeveloper.enabled) {
                    val player = name(Minecraft.getInstance(), sender) ?: "Party peer"
                    val detail = when (kind) {
                        "A" -> current.inventories[sender]?.let { aggregateChanges(previous, it) }.orEmpty()
                        "N" -> "nests +${Integer.bitCount(current.nestMask and previousNests.inv())}"
                        "B" -> "${PartySyncProtocol.codes[payload]?.label} completion"
                        "U" -> current.usage?.let { feederLabel(it[4], it[5]) }.orEmpty()
                        else -> "sparkling observations"
                    }
                    SafariDeveloper.event("From $player · $kind #$sequence · ${detail.ifEmpty { "objective update" }}")
                }
                if (kind == "B") {
                    val announce = submittedReports.remove(payload) == true
                    val biome = PartySyncProtocol.codes.getValue(payload)
                    if (sender == current.self && announce && ConfigManager.autoBiomeReports &&
                        SafariTracking.ledger.current?.manualReportReceived(biome) != true &&
                        current.biomeReports.firstOrNull { it.second == payload }?.first == sender)
                        PartyManager.sendPartyChat(Minecraft.getInstance(), payload)
                }
                if(kind=="S") sharedSparklings(current)
                SafariTracking.ledger.current?.let(::apply)
            }
        }
    }
    private fun sharedSparklings(current: PartySyncState) {
        PartySyncProtocol.species.forEachIndexed { i,species ->
            val word=if(i<30) 0 else 1
            val bit=1 shl (i%30)
            if (current.sparkling[word+2] and bit != 0) SparklingEncounters.captured(species, emptyList())
            else if (current.sparkling[word] and bit != 0 && (localSparkling[word] or localSparkling[word+2]) and bit == 0 &&
                SafariTracking.ledger.current?.sparklingCaptures?.captured(species)!=true && notifiedSparks.add(species)) {
                SafariDeveloper.event("Shared sparkling · $species")
                SparklingEncounters.shared(species)
                net.johnceo.sparklingmutuals.alerts.SparkleAutoParty.detected(species)
            }
        }
    }
    private fun apply(run: SafariRun) {
        val current=state ?: return
        if(appliedRun!==run) { appliedRun=run;appliedReports.clear() }
        current.biomeReports.filter { it !in appliedReports }.forEach { (sender,code) ->
            PartySyncProtocol.codes[code]?.let(run::sharedBiomeReport)
            appliedReports.add(sender to code)
        }
        val peers = current.inventories.filterKeys { it in lastRoster }.values
        run.sharedHeldObjectives(
            peers.any { it[10] + it[11] + it[12] > 0 },
            peers.any { (it.getOrNull(PartySyncProtocol.HELD_COINS) ?: 0) > 0 })
        BeeNests.sharedResolved(PartySyncSites.resolved(current.nestMask,PartySyncSites.nests))
        run.gems.shared(current.facts[23],current.facts[25]!=0)
        SafariStructures.shareWalls(current.facts[34])
        run.moundSurvey.sharedResolved(PartySyncSites.resolved(current.facts[36] or (current.facts[37] shl 16),PartySyncSites.mounds))
        current.usage?.let { usage ->
            run.birds.sharedUsage(usage)
            run.birdFoodCheck.population(usage[0],run.birds.feedingTarget)
        }
        // Terminal encounter flags deliberately do not call record(), PBs, history, or catch alerts.
    }
    private fun name(client: Minecraft,id: String): String? = client.connection?.onlinePlayers?.firstOrNull {
        PartySyncProtocol.uuid(it.profile.id.toString())==id }?.profile?.name ?: runCatching { SafariPartyInput.nameOf(java.util.UUID.fromString(
            "${id.substring(0,8)}-${id.substring(8,12)}-${id.substring(12,16)}-${id.substring(16,20)}-${id.substring(20)}").toString()) }.getOrNull()
    fun diagnostics() = "ready=${transport.ready} authority=$backendCoordination conflict=${state?.conflict} roster=${state?.roster?.size ?: 0} matchingParty=$matchingParty sequence=${state?.sequence ?: 0} peers=${state?.inventories?.size ?: 0}"
    fun objectiveDone(species: String): Boolean = state?.facts?.let { when(species) {
        "Gemzie" -> it[31]!=0
        "Wumpa" -> it[29]!=0
        "Doomspiral" -> it[27]!=0
        else -> false
    } }==true
    fun doomSummoned() = local[26]!=0 || state?.facts?.get(26)==1

    private fun feederLabel(type: Int, count: Int) =
        if (count == 0) "Empty" else "$count ${food.getOrNull(type) ?: "feed"}"

    private fun aggregateChanges(previous: IntArray?, next: IntArray): String = buildList {
        fun changed(index: Int) = previous?.getOrNull(index) != next.getOrNull(index)
        if (changed(17) || changed(18)) add("feeder ${feederLabel(next[17], next[18])}")
        if (changed(13)) add("feed used ${next[13]}")
        if (changed(9)) add("forest food picked up ${next[9]}")
        if ((10..12).any(::changed)) add("held feed seeds=${next[10]} worms=${next[11]} berries=${next[12]}")
        if (changed(PartySyncProtocol.HELD_COINS)) add("coins ${next.getOrNull(PartySyncProtocol.HELD_COINS) ?: "?"}")
        if (changed(22)) add("incense ${next[22]}")
        if ((19..21).any(::changed)) add("held gems lime=${next[19]} orange=${next[20]} purple=${next[21]}")
        if (changed(23)) add("gems placed ${Integer.bitCount(next[23])}")
        if (changed(24)) add("candles lit ${next[24]}")
        if (changed(34)) add("Snoozle walls ${Integer.bitCount(next[34])}")
        if (changed(35)) add("Troodon walls ${Integer.bitCount(next[35])}")
        val moundMask = next[36] or (next[37] shl 16)
        val previousMask = previous?.let { it[36] or (it[37] shl 16) } ?: 0
        val newSlots = PartySyncSites.mounds.indices.filter { (moundMask and previousMask.inv()) and (1 shl it) != 0 }
        if (newSlots.isNotEmpty()) add("mound slots +${newSlots.joinToString(",")}")
    }.joinToString(" · ")

    internal fun developerRows(): List<net.johnceo.sparklingmutuals.hud.HudRow> {
        val current = state
        val assignment = when {
            current?.conflict == true -> "Conflict"
            backendCoordination -> "Backend"
            waitingForAuthority -> "Waiting"
            else -> "Local / chat"
        }
        val row = net.johnceo.sparklingmutuals.hud.HudRow("Lobby", lobby ?: "Unknown", icon = null)
        val sharedFeeder = current?.usage?.let { feederLabel(it[4], it[5]) } ?: "Unknown"
        val localMounds = Integer.bitCount(local[36] or (local[37] shl 16))
        val peerMounds = current?.facts?.let { Integer.bitCount(it[36] or (it[37] shl 16)) } ?: 0
        return listOf(row, row.copy(label = "Transport", value = transport.developerStatus()),
            row.copy(label = "Room roster", value = "${current?.roster?.size ?: 0} players"),
            row.copy(label = "Safari ticket", value = when {
                ticketed -> "Accepted"
                ticketWindowLocked || began > 0 && now - began >= 30_000 -> "Entry closed"
                else -> "Awaiting acceptance"
            }),
            row.copy(label = "Sparkling sync", value = if (transport.roomSparklings) "Room snapshot" else "Peer relay"),
            row.copy(label = "Assignments", value = assignment),
            row.copy(label = "Sequence", value = (current?.sequence ?: 0).toString()),
            row.copy(label = "Feeder · local", value = if (SafariTracking.ledger.current?.birds?.localFeederObserved == true)
                feederLabel(local[17], local[18]) else "Unobserved"),
            row.copy(label = "Feeder · received", value = sharedFeeder),
            row.copy(label = "Mounds · loaded", value = SafariStructures.nearbyMounds.toString()),
            row.copy(label = "Mounds · known remaining", value = (SafariTracking.ledger.current?.moundSurvey?.remaining ?: 0).toString()),
            row.copy(label = "Mounds · resolved", value = "$localMounds local / $peerMounds received"))
    }

    internal fun developerSnapshot(): Map<String, Any?> {
        val current = state
        return mapOf(
            "lobby" to lobby, "roster" to current?.roster, "self" to current?.self,
            "rosterMatches" to matchingParty, "transport" to transport.developerSnapshot(),
            "ticketed" to ticketed, "ticketWindowLocked" to ticketWindowLocked,
            "sequence" to current?.sequence, "authority" to backendCoordination,
            "waitingForAuthority" to waitingForAuthority, "conflict" to current?.conflict,
            "serverRun" to current?.serverRun, "leader" to current?.leader,
            "assignments" to current?.locks, "reports" to discoveryReports,
            "pendingReports" to pendingReports, "pendingBiomeReports" to reportQueue,
            "biomeReports" to current?.biomeReports, "pendingAcknowledgments" to acknowledgments,
            "notifications" to current?.notifications, "sentNotifications" to current?.sentNotifications,
            "localAggregate" to local.copyOf(), "peerAggregates" to current?.inventories,
            "peerInventoriesCurrent" to current?.inventoriesCurrent,
            "mergedFacts" to current?.facts, "mergedFeeder" to current?.usage,
            "localNestMask" to localNests, "sharedNestMask" to current?.nestMask,
            "localSparklingMask" to localSparkling.copyOf(), "sharedSparklingMask" to current?.sparkling,
            "aggregateDueMillis" to due.takeIf { it != 0L }, "aggregatePendingLive" to pendingLive
        )
    }

    internal fun objectiveSupplies(run: SafariRun?, biome: SafariBiome): List<Pair<String, Map<FloorDropType, Int>>> {
        val slots = when (biome) {
            SafariBiome.FOREST -> mapOf(FloorDropType.SEEDS to 10, FloorDropType.WORM to 11, FloorDropType.BERRY to 12)
            SafariBiome.CAVERN -> {
                if (run == null || run.gems.opened) return emptyList()
                mapOf(FloorDropType.LIME to 19, FloorDropType.ORANGE to 20, FloorDropType.PURPLE to 21)
                    .filterKeys { type -> run.gems.placed.none { it.label == type.label } }
            }
            else -> return emptyList()
        }
        return supplySnapshots(run).mapNotNull { (player, snapshot) ->
            val held = slots.mapValues { (_, slot) -> snapshot[slot] }.filterValues { it > 0 }
            if (held.isEmpty()) null else player to held
        }
    }

    private fun supplySnapshots(run: SafariRun?): List<Pair<String, IntArray>> {
        val current = state ?: return emptyList()
        if (run == null || run !== SafariTracking.ledger.current || !transport.ready) return emptyList()
        val client = Minecraft.getInstance()
        return current.roster.mapNotNull { id ->
            val player = name(client, id) ?: return@mapNotNull null
            val snapshot = if (id == current.self) local else current.inventories[id] ?: return@mapNotNull null
            player to snapshot
        }
    }
    fun reset() {
        val current=state;transport.close(true,current?.nextSequence() ?: 0)
        state=null;lobby=null;world=null;began=0;sampleAt=0;helloAt=0;lastHello="";matchingParty=false
        transferRevision=null;ticketed=false;ticketWindowLocked=false
        SparklingEncounters.reset()
        local.fill(0);local[17]=-1;local[33]=PartySyncProtocol.AGGREGATE_VERSION;localSparkling.fill(0);localNests=0;lastNests=-1;lastSparkling=""
        lastAggregate="";produced="";liveFingerprint="";due=0;pendingLive=false;seenWalls=0;lastRoster=emptySet()
        reportQueue.clear();submittedReports.clear();pendingReports.clear();discoveryReports.clear();notifiedSparks.clear();lastInstructionAt=0
        seenTroodon=0;appliedRun=null;appliedReports.clear()
        pendingMoundHits.clear();openedWalls.clear()
        lastRenderSummary = ""
        acknowledgments.clear()
    }
}
