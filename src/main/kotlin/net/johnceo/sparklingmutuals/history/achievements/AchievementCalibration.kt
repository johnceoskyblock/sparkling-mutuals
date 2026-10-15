package net.johnceo.sparklingmutuals.history.achievements

import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.sqrt

/** Fixed effective exposures from the immutable workbook; not personal capture probabilities. */
object AchievementCalibration {
    val q: Double = 2047.0 / 2048.0
    val rates: Map<String, Double> = mapOf(
        "Cavernfish" to 6.0, // 'Critters Per Run'!C3
        "Flitter" to 7.0, // 'Critters Per Run'!C4
        "Shyworm" to 6.0, // 'Critters Per Run'!C5
        "Driftling" to 4.0, // 'Critters Per Run'!C6
        "Chuckwalla" to 3.0, // 'Critters Per Run'!C7
        "Rockmite" to 4.125, // 'Critters Per Run'!C8
        "Scrappy" to 2.5, // 'Critters Per Run'!C9
        "Snoozle" to 2.5, // 'Critters Per Run'!C10
        "Gemzie" to 3.0, // 'Critters Per Run'!C11
        "Foxtrot" to 7.0, // 'Critters Per Run'!C13
        "Bluebird" to 6.199671201, // 'Critters Per Run'!C14
        "Honeybug" to 4.5, // 'Critters Per Run'!C15
        "Treefrog" to 4.5, // 'Critters Per Run'!C16
        "Woodchucker" to 4.5, // 'Critters Per Run'!C17
        "Fluffling" to 2.0, // 'Critters Per Run'!C18
        "Hideonfloor" to 2.0, // 'Critters Per Run'!C19
        "Parakeet" to 3.153415248, // 'Critters Per Run'!C20
        "Macaw" to 1.293827103, // 'Critters Per Run'!C21
        "Areita" to 4.5, // 'Critters Per Run'!C23
        "Bloodbat" to 4.5, // 'Critters Per Run'!C24
        "Duplico" to 3.0, // 'Critters Per Run'!C25
        "Gazer" to 4.0, // 'Critters Per Run'!C26
        "Litterbug" to 5.0, // 'Critters Per Run'!C27
        "Solsnatcher" to 6.0, // 'Critters Per Run'!C28
        "Gimmiegold" to 4.722222222, // 'Critters Per Run'!C29
        "Hideonwall" to 3.0, // 'Critters Per Run'!C30
        "Hideyho" to 1.0, // 'Critters Per Run'!C31
        "Doomspiral" to 1.0, // 'Critters Per Run'!C32
        "Strongarm" to 6.0, // 'Critters Per Run'!C34
        "Tepid" to 7.0, // 'Critters Per Run'!B35
        "Polaris" to 3.0, // 'Critters Per Run'!B36
        "Shuddersquid" to 4.5, // 'Critters Per Run'!B37
        "Billygoat" to 3.0, // 'Critters Per Run'!B38
        "Mantis Shrimp" to 4.5, // 'Critters Per Run'!B39
        "Nozzlenose" to 3.0, // 'Critters Per Run'!B40
        "Troodon" to 3.0, // 'Critters Per Run'!B41
        "Wumpa" to 1.0, // 'Critters Per Run'!B42
    )
    private val logQ = ln(q)

    fun hazard(exposure: Double): Double {
        require(exposure.isFinite() && exposure >= 0) { "Exposure must be finite and nonnegative" }
        return if (exposure == 0.0) 0.0 else -exposure * logQ
    }

    fun dryProbability(exposure: Double): Double = exp(-hazard(exposure))

    fun expectedDistinct(runs: Int): Double {
        require(runs >= 0) { "Runs must be nonnegative" }
        return rates.values.sumOf { -expm1(it * runs * logQ) }
    }

    private fun logCompletionProbability(runs: Int): Double =
        rates.values.sumOf { ln(-expm1(it * runs * logQ)) }

    fun completionProbability(runs: Int): Double {
        require(runs >= 0) { "Runs must be nonnegative" }
        return exp(logCompletionProbability(runs))
    }

    /** Inclusive workbook convention: sum survival at n=0 through horizon, not horizon-1. */
    fun completionMoments(horizon: Int): Pair<Double, Double> {
        require(horizon >= 0) { "Horizon must be nonnegative" }
        var mean = 0.0
        var secondMoment = 0.0
        for (n in 0..horizon) {
            val survival = -expm1(logCompletionProbability(n))
            mean += survival
            secondMoment += (2.0 * n + 1.0) * survival
        }
        return mean to sqrt((secondMoment - mean * mean).coerceAtLeast(0.0))
    }

    fun firstCompletionQuantile(probability: Double): Int {
        require(probability.isFinite() && probability > 0.0 && probability < 1.0) {
            "Completion probability must be strictly between zero and one"
        }
        var low = 0
        var high = 1
        while (completionProbability(high) < probability) {
            check(high <= Int.MAX_VALUE / 2) { "Quantile exceeds supported run range" }
            high *= 2
        }
        while (high - low > 1) {
            val middle = low + (high - low) / 2
            if (completionProbability(middle) >= probability) high = middle else low = middle
        }
        return high
    }

    fun droughtThreshold(k: Int): Double {
        require(k >= 0) { "Opportunity count must be nonnegative" }
        return -k * ln1p(-.02)
    }
}
