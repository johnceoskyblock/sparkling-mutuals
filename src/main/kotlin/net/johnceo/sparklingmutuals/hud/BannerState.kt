// Banner stages and queue adapted from SafariUtils EncounterAlerts (MIT).
package net.johnceo.sparklingmutuals.hud

enum class BannerKind(val label: String,val text: String,val rgb: Int=0x55FF55,val sound: Int=9,val seconds: Float=2.5f,val priority: Int=10) {
    FULL_PARTY("Full party joined","Full Party Joined!"),
    HOTSPOT("Hotspot assignment","<BIOME> Biome"),
    FLOOR_DROPS("Floor drops collected","<BIOME> Floor Drops Done",0x55FFAA,44),
    BIOME_DONE("Biome complete","<BIOME> Complete"),
    ALL_BUT_MACAW("All except Macaw caught","All Except Macaw Done",sound=0),
    RUN_COMPLETE("Run complete","Run Complete",sound=0,priority=20),
    GEMS("All gems ready","All Gems Ready",0xFFAA00),
    BIRD_FOOD("All bird food collected","All Bird Food Collected"),
    COINS("All coins collected","All Coins Collected"),
    INCENSE("All incense ready","All Incense Ready",0xD28AFF),
    GEMZIE_READY("Gemzie chamber open","Gemzie Ready",0xFFAA00,0),
    GEMZIE_DONE("Gemzie caught","Gemzie Done!",sound=27),
    WUMPA_READY("Wumpa waking","Wumpa Ready",0xFFAA00,0),
    WUMPA_STARTED("Wumpa awake","Wumpa Started",0xFF5555,0,priority=20),
    WUMPA_DONE("Wumpa caught or retreated","Wumpa Done!",sound=27),
    DOOM_READY("Doomspiral ritual","Doomspiral Ready",0xFFAA00,0),
    DOOM_STARTED("Doomspiral summoned","Doomspiral Started",0xFF5555,0,priority=20),
    DOOM_DONE("Doomspiral caught or retreated","Doomspiral Done!",sound=27),
    HIDEYHO("Hideyho found","Hideyho Found",0x9955FF,17),
    MACAW("Macaw spawned","Macaw Spawned",0xFFAA00,29),
    BIRDS("Bluebird or Parakeet spawned","<CRITTER> Spawned",sound=0),
    FEED_GONE("All feed placed","All Feed Used",0x61FF55),
    FEEDER_EMPTY("Birdfeeder empty","Birdfeeder Empty",0xFF5555),
    FEED_DONE("All feed spawned birds","All Feed Done"),
    CONTEST_START("Contest started","Contest Started",sound=30),
    CONTEST_FIVE("Contest: five minutes","5 Minutes Left",0xFFFF55,30),
    CONTEST_THREE("Contest: three minutes","3 Minutes Left",0xFFCC55,30),
    CONTEST_ONE("Contest: one minute","1 Minute Left",0xFFAA00,30,priority=20),
    CONTEST_END("Contest ended","Contest Ended",0xFF5555,30),
    TICKET("Ticket earned","<TICKET> Ticket Earned!",sound=49,priority=20),
    WARP("Warping now","Warping Now",0xFF5555,9,priority=30);
    val supported get()=this in setOf(FULL_PARTY,BIOME_DONE,RUN_COMPLETE,GEMS,BIRD_FOOD,COINS,INCENSE,FEEDER_EMPTY,
        CONTEST_START,CONTEST_FIVE,CONTEST_THREE,CONTEST_ONE,CONTEST_END,TICKET,WARP)
    val encounterBiome get()=when(this) { GEMS -> net.johnceo.sparklingmutuals.safari.SafariBiome.CAVERN
        BIRD_FOOD,FEEDER_EMPTY -> net.johnceo.sparklingmutuals.safari.SafariBiome.FOREST
        COINS,INCENSE -> net.johnceo.sparklingmutuals.safari.SafariBiome.HAUNTED;else -> null }
}

data class BannerNotice(val kind: BannerKind,val text: String,val color: Int,val duration: Long,val sound: Int,val volume: Float,val pitch: Float,val soundOnly: Boolean=false)
data class ActiveBanner(val notice: BannerNotice,val started: Long) {
    fun remaining(now: Long)=(1.0-(now-started).coerceAtLeast(0)/notice.duration.toDouble()).coerceIn(0.0,1.0).toFloat()
    fun alpha(now: Long)=minOf(((now-started).coerceAtLeast(0)/180f).coerceIn(0f,1f),((notice.duration-(now-started))/1000f).coerceIn(0f,1f))
}
