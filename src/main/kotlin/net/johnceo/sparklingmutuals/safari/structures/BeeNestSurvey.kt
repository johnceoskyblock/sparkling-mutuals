package net.johnceo.sparklingmutuals.safari.structures

enum class NestBlock { UNKNOWN, NEST, HIVE }
data class NestWaypoint(val position: Triple<Int, Int, Int>, val nest: Boolean)

/** A run's 13 possible sites must be classified before any nest completion is trusted. */
class BeeNestSurvey {
    private val states = positions.associateWith { NestBlock.UNKNOWN }.toMutableMap()
    private var partyCleared=false
    fun acceptPartyClear() { partyCleared=true; punched.addAll(states.filterValues { it == NestBlock.NEST }.keys) }
    private val punched = mutableSetOf<Triple<Int, Int, Int>>()
    val remaining get() = states.filterValues { it == NestBlock.NEST }.keys - punched
    val unverified get() = states.filterValues { it == NestBlock.UNKNOWN }.keys.toSet()
    val nestCount get() = states.values.count { it == NestBlock.NEST }
    fun classification(pos: Triple<Int, Int, Int>) = states[pos] ?: NestBlock.UNKNOWN
    val punchedCount get() = punched.size
    val allChecked get() = partyCleared || states.values.none { it == NestBlock.UNKNOWN } &&
        states.values.any { it == NestBlock.NEST } && remaining.isEmpty()
    /** null means no client block data. Only an inspected nest or hive establishes coverage. */
    fun scan(blockAt: (Triple<Int, Int, Int>) -> NestBlock?) {
        positions.forEach { site -> blockAt(site)?.let { observe(site, it) } }
    }
    fun waypoints(needed: Boolean): List<NestWaypoint> = if (!needed) emptyList() else
        remaining.map { NestWaypoint(it, true) } + unverified.map { NestWaypoint(it, false) }
    fun observe(pos: Triple<Int, Int, Int>, block: NestBlock) {
        // The initial source classification belongs to the run, even after collection or unloading.
        if (pos in states && block != NestBlock.UNKNOWN && states[pos] != NestBlock.NEST) states[pos] = block
        if(partyCleared && block == NestBlock.NEST) punched.add(pos)
    }
    fun punch(pos: Triple<Int, Int, Int>) = states[pos] == NestBlock.NEST && punched.add(pos)
    fun reset() { partyCleared=false; states.keys.forEach { states[it] = NestBlock.UNKNOWN }; punched.clear() }
    companion object {
        // Live block diagnostics confirmed the original Y values pointed at fences above these blocks.
        val positions = listOf(Triple(-17, 68, 41), Triple(-22, 68, 39), Triple(25, 70, 41),
            Triple(23, 70, 46), Triple(-1, 81, 60), Triple(-9, 81, 58), Triple(-13, 103, 21),
            Triple(-11, 103, 13), Triple(-1, 84, 9), Triple(20, 88, 33), Triple(15, 88, 31),
            Triple(18, 114, 79), Triple(16, 114, 87))
    }
}
