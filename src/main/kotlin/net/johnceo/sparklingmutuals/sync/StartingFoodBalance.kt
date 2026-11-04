package net.johnceo.sparklingmutuals.sync

/** Starting notices identify grants. WebSocket inventory snapshots cap what can still be unused. */
internal class StartingFoodBalance {
    private var announced = 0
    var remaining = 0
        private set

    fun observe(starting: Int, held: Int) {
        remaining = (remaining + (starting - announced).coerceAtLeast(0)).coerceAtMost(held)
        announced = maxOf(announced, starting)
    }
}
