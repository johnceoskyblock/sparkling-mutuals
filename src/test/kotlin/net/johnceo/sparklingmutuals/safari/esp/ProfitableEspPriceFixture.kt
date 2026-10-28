package net.johnceo.sparklingmutuals.safari.esp

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.history.BazaarPrices
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/** Supplies cached quotes to the real command/ESP path without making a network request. */
open class ProfitableEspPriceFixture {
    private lateinit var cache: Any
    private lateinit var field: java.lang.reflect.Field
    private var previous: Any? = null
    private var instaSell = false
    private var ticketMode = 0

    @BeforeEach fun supplyCachedPrices() {
        cache = BazaarPrices::class.java.getDeclaredField("cache").apply { isAccessible = true }.get(BazaarPrices)
        field = cache.javaClass.getDeclaredField("prices").apply { isAccessible = true }
        previous = field.get(cache)
        instaSell = ConfigManager.runCoinsInstaSell
        ticketMode = ConfigManager.profitsTicketMode
        ConfigManager.profitsTicketMode = 0
        ConfigManager.runCoinsInstaSell = false
        prices(profitable.associate { BazaarPrices.shardId(it) to doubleArrayOf(100.0, 100.0) })
    }
    protected fun prices(quotes: Map<String, DoubleArray>) { field.set(cache, quotes) }
    @AfterEach fun restoreCachedPrices() {
        field.set(cache, previous)
        ConfigManager.runCoinsInstaSell = instaSell
        ConfigManager.profitsTicketMode = ticketMode
    }
    companion object {
        val profitable = setOf("Hideonfloor", "Hideonwall", "Chuckwalla", "Fluffling", "Mantis Shrimp",
            "Foxtrot", "Bluebird", "Treefrog", "Honeybug", "Woodchucker")
    }
}
