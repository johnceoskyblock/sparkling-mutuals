package net.johnceo.sparklingmutuals.safari.shots

data class ShotLimits(val maxInputs: Int = 300, val maxEvents: Int = 100_000, val maxBytes: Long = 10 * 1024 * 1024,
    val bufferBytes: Int = 4 * 1024 * 1024, val footerReserve: Int = 64 * 1024,
    val preInputMicros: Long = 500_000, val sampleWindowMicros: Long = 8_000_000,
    val associationMicros: Long = 1_500_000, val feedbackMicros: Long = 60_000_000,
    val associationDistanceSquared: Double = 64.0)
