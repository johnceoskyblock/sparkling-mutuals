package net.johnceo.sparklingmutuals.safari

class HideyhoPromptWindow {
    private var lastDialogue: Long? = null
    fun observe(text: String, now: Long) {
        val plain = SafariRules.strip(text)
        if (SafariRules.isHideyhoDialogue(plain)) lastDialogue = now
        else if (plain.startsWith("[NPC]")) reset()
    }
    fun acceptsOptions(now: Long) = lastDialogue?.let { now - it in 0..15000 } ?: false
    fun reset() { lastDialogue = null }
}
