package net.johnceo.sparklingmutuals.config

/** Optional starting resources participate only when the owner enables this feature. */
internal fun withStartingItems(test: () -> Unit) {
    val previous = ConfigManager.announceStartingItems
    ConfigManager.announceStartingItems = true
    try { test() } finally { ConfigManager.announceStartingItems = previous }
}
