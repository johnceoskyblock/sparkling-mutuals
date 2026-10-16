package net.johnceo.sparklingmutuals.safari.shots

import com.google.gson.JsonParser
object ShotTestFixtures {
    val zero = ShotVector(0.0, 0.0, 0.0)
    fun metadata() = ShotMetadata("session-test", "26.1.2", "3.3.0", "test", "minecraft:overworld", true, false)
    private var tokens = 0
    fun input(token: String = "s${++tokens}", variant: ShotVariant = ShotVariant.ORDINARY, hand: ShotHand = ShotHand.MAIN) =
        ShotObservation.Input(token, hand, variant, ShotUseRoute.AIR, ShotPose(zero, zero, zero, 0f, 0f, ShotVector(0.0,0.0,1.0),zero))
    class Budget(override var usedBytes: Int = 0, val max: Int = 4*1024*1024) : ShotBufferBudget {
        override fun tryReserve(bytes: Int): Boolean { if (usedBytes + bytes > max) return false; usedBytes += bytes; return true }
        override fun release(bytes: Int) { usedBytes -= bytes }
    }
    class Sink(val budget: ShotBufferBudget, var allowed: Boolean = true) : ShotSink {
        val lines = mutableListOf<String>()
        override fun offer(line: ByteArray): Boolean { if (!allowed) return false; lines.add(line.toString(Charsets.UTF_8)); budget.release(line.size); return true }
        override fun finish(footer: ByteArray) { lines.add(footer.toString(Charsets.UTF_8)); budget.release(64*1024) }
        override fun queuedBytes() = 0
        fun records() = lines.map { JsonParser.parseString(it).asJsonObject }
    }
    fun session(limits: ShotLimits = ShotLimits()) = Budget().let { ShotSession(metadata(), limits, Sink(it), it) }
}
