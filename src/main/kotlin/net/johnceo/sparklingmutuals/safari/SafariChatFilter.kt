package net.johnceo.sparklingmutuals.safari

/** Display-only suppression; incoming message events still reach capture and PB tracking. */
object SafariChatFilter {
    private val moundNoise = setOf("Small cracks begin to form in the mound...",
        "The cracks seem to be getting larger, keep hitting it!", "Chunks of the mound begin falling away...",
        "The mound is about to fall to pieces! Keep going!")
    private val interactionNoise = setOf(
        "The Shyworm saw a player and fled!",
        "The Shyworm surfaced nearby! Don't let it see you!",
        "The Shyworm hid back into the ground.",
        "The Shyworm fled because it was startled!",
        "You ran headfirst into the wall. Some more cracks appeared in it...",
        "Keep running into the wall! More cracks are forming!",
        "You destroyed the wall! Maybe something was hiding behind it...",
        "The mound fell apart, revealing a Rockmite hidden inside!",
        "The mound falls apart, but nothing is inside...",
        "The Chuckwalla dodged your critter capsule and started running away!",
        "The Chuckwalla is cornered! Catch it!",
        "Your Critter Capsule can't get through Scrappy's armor!",
        "This particular Scrappy doesn't seem to care for this type of food...",
        "Looks like the hive is empty now...",
        "You found a Duplico that was disguised as a block!",
        "You used your Icebreaker to shatter the ice and free the Troodon trapped within!",
        "The Scrappy cautiously started eating the Flavor-packed Fish.",
        "The Scrappy reaches out its paw for more Flavor-packed Fish.",
        "The Scrappy ate the Flavor-packed Fish and came out of its shell!",
        "The Scrappy cautiously started eating the Wholesale Wheat.",
        "The Scrappy reaches out its paw for more Wholesale Wheat.",
        "The Scrappy ate the Wholesale Wheat and came out of its shell!",
        "The Scrappy cautiously started eating the Lush Lily Pad.",
        "The Scrappy reaches out its paw for more Lush Lily Pad.",
        "The Scrappy ate the Lush Lily Pad and came out of its shell!")
    private val escaped = Regex("\\bThe (?:${SafariRoster.all.joinToString("|") { Regex.escape(it.name) }}) escaped\\b", RegexOption.IGNORE_CASE)
    fun hidden(raw: String, enabled: Boolean) = enabled && SafariMessages.lines(raw).any { line ->
        listOf("You threw a", "CAPTURE!", "LOOT SHARE!").any { line.contains(it, true) } || escaped.containsMatchIn(line) || line in moundNoise || line in interactionNoise
    }
}
