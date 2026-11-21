package net.johnceo.sparklingmutuals.safari

internal enum class SplitTimeFormat(private val digits: Int, private val unitMillis: Int) {
    MILLISECONDS(3, 1), HUNDREDTHS(2, 10), TENTHS(1, 100), SECONDS(0, 1000);

    fun format(millis: Long): String {
        val time = millis.coerceAtLeast(0)
        val clock = "${time / 60_000}:${(time / 1000 % 60).toString().padStart(2, '0')}"
        if (digits == 0) return clock
        return "$clock.${(time % 1000 / unitMillis).toString().padStart(digits, '0')}"
    }
}

/** Elapsed times lock when the mode's capture and report requirements are both met. */
internal class SafariSplits {
    private val unique = mutableMapOf<SafariBiome, Long>()
    private val fullClear = mutableMapOf<SafariBiome, Long>()

    fun time(mode: SafariMode, biome: SafariBiome): Long? = when (mode) {
        SafariMode.UNIQUE -> unique[biome]
        SafariMode.FULL_CLEAR -> fullClear[biome]
        SafariMode.SPARKLING -> null
    }

    fun observe(run: SafariRun, now: Long) {
        if (run.endedAt != null) return
        val elapsed = run.elapsed(now)
        for (biome in SafariBiome.entries) {
            val reported = run.manualReportReceived(biome) || run.backendBiomeReportReceived(biome)
            if (biome !in unique && biome.critters.all {
                    run.captureProgress(it.name) > 0 || reported && it.name in reportOptionalUniques
                }) unique[biome] = elapsed
            if (biome !in fullClear && reported && biome.critters.all {
                    run.capturesLeft(it.name, run.captureProgress(it.name)) == 0
                }) fullClear[biome] = elapsed
        }
    }

    private companion object {
        val reportOptionalUniques = setOf("Macaw", "Rockmite", "Snoozle")
    }
}
