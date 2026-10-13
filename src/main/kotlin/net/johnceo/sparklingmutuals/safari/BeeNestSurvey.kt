package net.johnceo.sparklingmutuals.safari

enum class NestBlock { UNKNOWN, NEST, HIVE }

/** A run's 13 possible sites must be classified before any nest completion is trusted. */
class BeeNestSurvey {
    private val states = positions.associateWith { NestBlock.UNKNOWN }.toMutableMap()
    private val punched = mutableSetOf<Triple<Int, Int, Int>>()
    val remaining get() = states.filterValues { it == NestBlock.NEST }.keys - punched
    val punchedCount get() = punched.size
    val allChecked get() = states.values.none { it == NestBlock.UNKNOWN } &&
        states.values.count { it == NestBlock.NEST } >= 3 && remaining.isEmpty()
    fun observe(pos: Triple<Int, Int, Int>, block: NestBlock) {
        // A punched nest can disappear on the server; that cannot undo the interaction.
        if (pos in states && !(pos in punched && block == NestBlock.UNKNOWN)) states[pos] = block
    }
    fun punch(pos: Triple<Int, Int, Int>) = states[pos] == NestBlock.NEST && punched.add(pos)
    fun reset() { states.keys.forEach { states[it] = NestBlock.UNKNOWN }; punched.clear() }
    companion object {
        val positions = listOf(Triple(-17, 69, 41), Triple(-22, 69, 39), Triple(25, 71, 41),
            Triple(23, 71, 46), Triple(-1, 82, 60), Triple(-9, 82, 58), Triple(-13, 104, 21),
            Triple(-11, 104, 13), Triple(-1, 85, 9), Triple(20, 89, 33), Triple(15, 89, 31),
            Triple(18, 115, 79), Triple(16, 115, 87))
    }
}
