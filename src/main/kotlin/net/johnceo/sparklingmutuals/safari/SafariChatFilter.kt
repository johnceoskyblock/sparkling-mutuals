package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.SafariChatCategory
import net.johnceo.sparklingmutuals.config.SafariChatConfig

/** Display-only SafariUtils chat classification (MIT); tracking receives messages before this filter. */
object SafariChatFilter {
    private val ROCKMITE_MESSAGES = setOf(
		"Small cracks begin to form in the mound...",
		"The cracks seem to be getting larger, keep hitting it!",
		"Chunks of the mound begin falling away...",
		"The mound is about to fall to pieces! Keep going!",
		"The mound falls apart, but nothing is inside...",
		"The mound fell apart, revealing a Rockmite hidden inside!"
    )
    private val CHUCKWALLA_MESSAGES = setOf(
		"The Chuckwalla dodged your critter capsule and started running away!",
		"The Chuckwalla is cornered! Catch it!"
    )
    private val SCRAPPY_MESSAGES = setOf(
		"The Scrappy happily eats the Wholesale Wheat.",
		"The Scrappy happily eats the Flavor-packed Fish.",
		"The Scrappy happily eats the Lush Lily Pad.",
		"[NPC] Hunter Jasper: Hmm that \"Hunter\" Tobias guy was right... Nice Catch!",
		"[NPC] Hunter Jasper: There might be more Scrappy around - I wonder if my brothers have found them yet.",
		"[NPC] Hunter Luigi: That really worked, you caught the Scrappy! Nice going!",
		"[NPC] Hunter Luigi: My brothers might be around here, maybe they found more Scrappy...",
		"[NPC] Hunter Reeves: You really managed to catch that Scrappy? Nice going!",
		"[NPC] Hunter Reeves: Keep an eye out for my brothers - they might have found more Scrappy around...",

		"This particular Scrappy doesn't seem to care for this type of food...",
		"The Scrappy cautiously started eating the Wholesale Wheat.",
		"The Scrappy reaches out its paw for more Wholesale Wheat.",
		"The Scrappy ate the Wholesale Wheat and came out of its shell!",
		"The Scrappy, shocked at your betrayal, decided to attack!",
		"The Scrappy cautiously started eating the Flavor-packed Fish.",
		"The Scrappy reaches out its paw for more Flavor-packed Fish.",
		"The Scrappy ate the Flavor-packed Fish and came out of its shell!",
		"The Scrappy cautiously started eating the Lush Lily Pad.",
		"The Scrappy reaches out its paw for more Lush Lily Pad.",
		"The Scrappy ate the Lush Lily Pad and came out of its shell!",
		"Your Critter Capsule can't get through Scrappy's armor!",
		"[NPC] Hunter Jasper: That little ball right here is actually a Scrappy.",
		"[NPC] Hunter Jasper: Critter Capsules don't seem to do anything to them.",
		"[NPC] Hunter Jasper: \"Hunter\" Tobias had mentioned to me that you can lure them out with Food.",
		"[NPC] Hunter Jasper: He said that they might like either Wholesale Wheat, Flavor-packed Fish, or Lush Lily Pads.",
		"[NPC] Hunter Luigi: See that little creature next to me? That's actually a Scrappy that curled itself into a ball.",
		"[NPC] Hunter Luigi: My Critter Capsules seem to just bounce right off it!",
		"[NPC] Hunter Luigi: That \"Hunter\" Tobias guy told me that they love Food.",
		"[NPC] Hunter Luigi: He said that they might be motivated to come out if given either Wholesale Wheat, Flavor-packed Fish, or Lush Lily Pads.",
		"[NPC] Hunter Reeves: That little ball nearby is actually a Scrappy all curled up.",
		"[NPC] Hunter Reeves: My Critter Capsules are useless against it in this state...",
		"[NPC] Hunter Reeves: However, that \"Hunter\" Tobias guy told me that you can sometimes coax them into coming out with Food.",
		"[NPC] Hunter Reeves: He said that each Scrappy is different, and they like either Wholesale Wheat, Flavor-packed Fish, or Lush Lily Pads."
    )
    private val NPC_MESSAGES = setOf(
		"[NPC] Hunter Lennard: I saw a really strange  Critter the other day...",
		"[NPC] Hunter Lennard: It looked different from the usual stuff I see around here - it was SPARKLING...",
		"[NPC] Hunter Lennard: I stopped what I was doing and tried my best to catch it...but it got away.",
		"[NPC] Hunter Lennard: Haven't seen one since - they must be pretty rare...",
		"[NPC] Hunter Lennard: Perhaps \"Hunter\" Tobias knows something about them?",
		"[NPC] Huntress Alissa: There are lots of useful things lying about on the ground in the Critter Safari.",
		"[NPC] Huntress Alissa: Us hunters like to call them Floor Drops.",
		"[NPC] Huntress Alissa: Certain  Critters can only be found with the help of specific floor drops. How exciting!",
		"That's not a  Critter...",
		"[NPC] Birdwatcher Tim: The Birdfeeder in this here Forest Biome is simply amazing!",
		"[NPC] Birdwatcher Tim: If you place some food in it and wait a while, a Bird will show up!",
		"[NPC] Birdwatcher Tim: Different birds seem to prefer different foods, though.",
		"[NPC] Birdwatcher Tim: From what I can tell, Bluebirds prefer Yogi Berries...",
		"[NPC] Birdwatcher Tim: Parakeets prefer Wriggleworms...",
		"[NPC] Birdwatcher Tim: And Macaws, the rarest of the lot, prefer a good ol' Bag of Seeds!",
		"[NPC] Birdwatcher Tim: From what I've seen, you're more likely to attract a certain type of Bird if you use its favorite food...",
		"[NPC] Hunter Billy: Hey there!",
		"[NPC] Hunter Billy: Do you want it?",
		"[NPC] Hunter Billy: Alright, alright. Guess it's worthless...",
		"[NPC] Hunter Billy: Trying to scam me? I'll hunt YOU!",
		"[NPC] Hunter Billy: Nice doing business with ya!",
		"[NPC] Hunter Dennis: Brr!",
		"[NPC] Hunter Dennis: It's f-f-f-freezing out here - d-d-don't sit around for t-t-too long!",
		"[NPC] Hunter Dennis: Fair enough.",
		"[NPC] Hunter Dennis: Watch your s-s-step...",
		"[NPC] Hunter Dennis: Let's g-g-goo! T-t-t-thanks...",
		"[NPC] Hunter Dennis: Stop trying to s-s-swindle me!",
		"[NPC] Hunter Dennis: Awesome trade! T-t-thanks!",
		"[NPC] Hunter Harry: Phew!",
		"[NPC] Hunter Harry: Glad to find someone else out here - this place is scary!",
		"[NPC] Hunter Harry: Erm, okay.",
		"[NPC] Hunter Harry: Don't go too far, it's scary...",
		"[NPC] Hunter Harry: Don't mess with me!",
		"[NPC] Hunter Harry: Sweet, 'ppreciate it!",
		"[NPC] Hunter Harry: Thanks for the trade!",
		"[NPC] Huntress Melissa: Heyyy!",
		"[NPC] Huntress Melissa: Hmph, suit yourself.",
		"[NPC] Huntress Melissa: Yippee! Thanks for the trade!",
		"[NPC] Huntress Melissa: Come back when you find one!",
		"[NPC] Huntress Melissa: I'll let you know if I ever find another Shard to trade!"
    )
    private val SHYWORM_MESSAGES = setOf(
		"The Shyworm surfaced nearby! Don't let it see you!",
		"The Shyworm hid back into the ground.",
		"The Shyworm fled because it was startled!",
		"The Shyworm saw a player and fled!"
    )
    private val SNOOZLE_MESSAGES = setOf(
		"You ran headfirst into the wall. Some more cracks appeared in it...",
		"Keep running into the wall! More cracks are forming!",
		"You destroyed the wall! Maybe something was hiding behind it..."
    )
    private val COLD_MESSAGES = setOf(
		"BRRR! It's getting really cold! But you've got to keep moving...",
		"BRRR! It's so cold that you can barely feel your fingers. Moving is getting difficult...",
		"BRRR! Your movement slows to a crawl as the cold threatens to take over. Time to get out of here...",
		"BRRR! You're freezing! All you can think about is getting out of here to a warm campfire..."
    )
    private val GAZER_MESSAGES = setOf(
		"You wake up suddenly...",
		"You don't feel like sleeping in that bed again..."
    )

    private val escaped = Regex("^The .+ (?:escaped\\b|dodged your critter capsule\\b)", RegexOption.IGNORE_CASE)

    fun category(line: String): SafariChatCategory? = when {
        line.startsWith("HEAD START!") || line.startsWith("HOTSPOT!") -> SafariChatCategory.RUN_START
        line.startsWith("CAPTURE!") -> SafariChatCategory.CRITTER_CATCHES
        line.startsWith("LOOT SHARE!") -> SafariChatCategory.LOOT_SHARES
        line.startsWith("FLOOR DROP!") -> SafariChatCategory.FLOOR_DROPS
        line.startsWith("[NPC] Safari Manager:") -> SafariChatCategory.MANAGER
        line in ROCKMITE_MESSAGES -> SafariChatCategory.ROCKMITE
        line in CHUCKWALLA_MESSAGES -> SafariChatCategory.CHUCKWALLA
        line in SCRAPPY_MESSAGES -> SafariChatCategory.SCRAPPY
        line in SHYWORM_MESSAGES -> SafariChatCategory.SHYWORM
        line in SNOOZLE_MESSAGES -> SafariChatCategory.SNOOZLE
        line in COLD_MESSAGES -> SafariChatCategory.COLD
        line in GAZER_MESSAGES -> SafariChatCategory.GAZER
        line.startsWith("You placed the ") && line.endsWith(" Gem on its podium!") ||
            line.startsWith("A rumbling sound can be heard") -> SafariChatCategory.GEMZIE
        line.startsWith("You hear the sound of massive footsteps") || line.startsWith("The Wumpa has awoken") ||
            line == "The Wumpa is enraged and can't be caught right now!" ||
            line.startsWith("The cave is collapsing") || line.startsWith("The cave opens up again") ||
            "fainted by a Wumpa" in line && line.endsWith("lost some of your items!") -> SafariChatCategory.WUMPA
        line == "You used your Icebreaker to shatter the ice and free the Troodon trapped within!" -> SafariChatCategory.TROODON
        line == "This candle is already lit..." || line.startsWith("You used the Soothing Incense to light the candle") ||
            line.startsWith("Your ritual summoned a Doomspiral") || line == "The Doomspiral is enraged and can't be caught right now!" ||
            line.startsWith("Something stirs in the Haunted Biome") || line.startsWith("The darkness in the Haunted Biome fades away") ||
            line.startsWith("The Doomspiral retreats back underground") -> SafariChatCategory.DOOMSPIRAL
        line == "You startled the Bloodbat!" -> SafariChatCategory.BLOODBAT
        line == "You found a Duplico that was disguised as a block!" -> SafariChatCategory.DUPLICO
        line == "A Gimmiegold appeared out of nowhere and gobbled up your Shining Coin!" -> SafariChatCategory.GIMMIEGOLD
        line.startsWith("[MOB] Hideyho:") || line == "Your Critter Capsule doesn't seem to work on this creature..." -> SafariChatCategory.HIDEYHO
        Regex("^(A|Two) (Bluebirds?|Parakeets?|Macaws?) (was|were) attracted to the Birdfeeder!$").matches(line) -> SafariChatCategory.BIRD_SPAWNS
        line == "Looks like the hive is empty now..." -> SafariChatCategory.EMPTY_NESTS
        line in setOf("You cannot drop items yet!", "You must double tap the drop button to drop this item!",
            "You can disable this in the Settings in your SkyBlock Menu!") -> SafariChatCategory.DROP_ITEMS
        line in NPC_MESSAGES || Regex("^\\[NPC] (Hunter Billy|Hunter Dennis|Hunter Harry|Huntress Melissa): .+").matches(line) -> SafariChatCategory.NPCS
        line.startsWith("You threw a ") || escaped.containsMatchIn(line) &&
            SafariRoster.all.any { line.startsWith("The " + it.name + " ") } -> SafariChatCategory.CATCH_ATTEMPTS
        else -> null
    }

    fun hidden(raw: String, enabled: Boolean, selected: (SafariChatCategory) -> Boolean = SafariChatConfig::hidden): Boolean {
        if (!enabled) return false
        val text = SafariRules.strip(raw).trim()
        if (!text.startsWith("[NPC] ") && !text.startsWith("[MOB] ") && SafariMessages.lines(text).isEmpty()) return false
        return raw.split(Regex("\\r?\\n|\\\\n")).any { rawLine ->
            val line = SafariRules.strip(rawLine).trim()
            val serverDialogue = line.startsWith("[NPC] ") || line.startsWith("[MOB] ")
            if (!serverDialogue && SafariMessages.lines(line).isEmpty()) false
            else category(line)?.let(selected) == true
        }
    }
}
