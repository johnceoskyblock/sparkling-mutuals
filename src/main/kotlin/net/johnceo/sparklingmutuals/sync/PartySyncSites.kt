package net.johnceo.sparklingmutuals.sync

import net.johnceo.sparklingmutuals.safari.structures.BeeNestSurvey

/** Exact v2 canonical slot order. Never sort walls or infer bits from entity order. */
internal object PartySyncSites {
    val nests=BeeNestSurvey.positions.sortedWith(compareBy({it.first},{it.second},{it.third}))
    val mounds=listOf(-137 to (60 to 57),-135 to (40 to 53),-130 to (62 to 42),-129 to (60 to 47),
        -128 to (41 to 60),-128 to (58 to 66),-127 to (62 to 41),-122 to (40 to 55),-121 to (43 to 24),
        -120 to (41 to 64),-119 to (39 to 78),-118 to (40 to 50),-114 to (43 to 32),-113 to (46 to 35),
        -110 to (62 to 31),-108 to (41 to 23),-107 to (61 to 50),-104 to (62 to 42),-99 to (39 to 79),
        -99 to (48 to 94),-99 to (62 to 40),-97 to (40 to 26),-88 to (39 to 65),-84 to (40 to 30),
        -82 to (60 to 64),-80 to (57 to 61),-80 to (62 to 65),-70 to (40 to 62),-70 to (56 to 58),
        -67 to (51 to 45),-61 to (45 to 57),-56 to (61 to 61)).map { Triple(it.first,it.second.first,it.second.second) }
    val troodon=listOf(Triple(-104,81,-95),Triple(-131,79,-62),Triple(-109,90,-27))
    fun moundAt(position: Triple<Int, Int, Int>): Triple<Int, Int, Int>? {
        val candidates = mounds.map { site ->
            val dx = site.first - position.first
            val dy = site.second - position.second
            val dz = site.third - position.third
            site to (dx * dx + dy * dy + dz * dz)
        }
        val nearest = candidates.minOf { it.second }
        return if (nearest <= 2) candidates.singleOrNull { it.second == nearest }?.first else null
    }
    fun resolved(mask: Int,sites: List<Triple<Int,Int,Int>>) = sites.filterIndexed { i,_ -> mask and (1 shl i)!=0 }
    fun mask(sites: List<Triple<Int,Int,Int>>,resolved: Collection<Triple<Int,Int,Int>>) =
        sites.withIndex().fold(0) { bits,(i,pos) -> if(pos in resolved) bits or (1 shl i) else bits }
}
