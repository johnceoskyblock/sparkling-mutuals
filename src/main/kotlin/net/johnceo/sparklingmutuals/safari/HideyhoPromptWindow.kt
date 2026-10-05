package net.johnceo.sparklingmutuals.safari

class HideyhoPromptWindow {
    private var lastDialogue: Long? = null
    private var world: Any? = null
    private var connection: Any? = null
    fun synchronize(world: Any?, connection: Any?) {
        if (world !== this.world || connection !== this.connection) reset()
        this.world = world
        this.connection = connection
    }
    fun observe(text: String, now: Long, world: Any? = null, connection: Any? = null) {
        synchronize(world, connection)
        val plain = SafariRules.strip(text)
        if (SafariRules.isHideyhoDialogue(plain)) lastDialogue = now
        else if (SafariRules.isDialogue(plain)) reset()
    }
    fun acceptsOptions(now: Long) = lastDialogue?.let { now - it in 0..15000 } ?: false
    fun reset() { lastDialogue = null; world = null; connection = null }
}
