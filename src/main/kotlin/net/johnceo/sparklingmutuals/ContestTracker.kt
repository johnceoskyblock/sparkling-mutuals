package net.johnceo.sparklingmutuals

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ContestTracker {

    private const val FIRST_START_MINUTE = 15
    private const val WINDOW_SECONDS = 1200
    private const val END_GAP_SECONDS = 30
    private const val STALE_GUARD_SECONDS = 10

    private var tickCounter = 0

    private var windowKey = -1L
    private var secondsIntoWindow = 0

    private var complete = false

    private val warnedMinutes = linkedSetOf<Int>()

    private var points = -1
    private var tier = ""

    private val completeTiers = setOf(
        "UNCOMMON",
        "RARE",
        "EPIC",
        "LEGENDARY",
        "MYTHIC",
        "DIVINE",
        "SPECIAL"
    )

    fun init() {
        // Reserved for future setup.
    }

    fun onClientTick(client: Minecraft) {
        if (!ContestConfig.trackContest) {
            return
        }

        if (client.player == null || client.level == null) {
            return
        }

        tickCounter++

        if (tickCounter < 10) {
            return
        }

        tickCounter = 0

        rollWindow()
        readSidebar(client)
        maybeWarn(client)
    }

    private fun rollWindow() {
        val now = LocalTime.now()

        val shifted = Math.floorMod(
            now.toSecondOfDay() - FIRST_START_MINUTE * 60,
            86400
        )

        val key = (shifted / WINDOW_SECONDS).toLong()

        secondsIntoWindow = shifted % WINDOW_SECONDS

        if (key != windowKey) {
            val first = windowKey == -1L

            windowKey = key
            complete = false
            warnedMinutes.clear()
            points = -1
            tier = ""

            if (!first) {
                println(
                    "Sparkling Mutuals: Contest window rolled at $now"
                )
            }
        }
    }

    fun secondsToEnd(): Int {
        return max(
            0,
            WINDOW_SECONDS - END_GAP_SECONDS - secondsIntoWindow
        )
    }

    fun secondsToNextStart(): Int {
        return WINDOW_SECONDS - secondsIntoWindow
    }

    fun betweenContests(): Boolean {
        return secondsToEnd() == 0
    }

    private fun readSidebar(client: Minecraft) {
        var foundPoints = -1
        var foundTier = ""

        for (line in SkyblockSidebar.lines(client)) {
            val match = TIER_ROW.find(line)

            if (match != null && isTierName(match.groupValues[1])) {
                foundTier = match.groupValues[1]
                foundPoints = parseInt(match.groupValues[2])
                break
            }
        }

        if (foundTier.isNotEmpty()) {
            tier = foundTier
            points = foundPoints
        }

        if (
            tier.isNotEmpty() &&
            !complete &&
            secondsIntoWindow >= STALE_GUARD_SECONDS
        ) {
            if (completeTiers.contains(tier.uppercase())) {
                complete = true
                println(
                    "Sparkling Mutuals: Contest complete — $tier with $points"
                )
            }
        }
    }

    private fun maybeWarn(client: Minecraft) {
        if (
            complete ||
            !ContestConfig.contestWarnEnabled ||
            betweenContests()
        ) {
            return
        }

        if (!SkyblockSidebar.inSkyblock(client)) {
            return
        }

        val remaining = secondsToEnd()
        var rang = false

        for (minutes in warnMinutes()) {
            if (
                remaining <= minutes * 60 &&
                warnedMinutes.add(minutes)
            ) {
                rang = true

                client.player?.sendSystemMessage(
                    Component.literal(
                        "§b[Sparkling Mutuals] §c§lContest not complete§r — " +
                                "${formatRemaining(remaining)} left."
                    )
                )
            }
        }

        if (rang) {
            playSound(client)

            if (ContestConfig.contestWarnTitle) {
                showTitle(client, remaining)
            }
        }
    }

    private fun warnMinutes(): List<Int> {
        return ContestConfig.contestWarnMinutes
            .split(Regex("[,\\s]+"))
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
            .sortedDescending()
    }

    private fun playSound(client: Minecraft) {
        val id = Identifier.tryParse(
            ContestConfig.contestSound.trim()
        ) ?: return

        val sound = BuiltInRegistries.SOUND_EVENT
            .getOptional(id)
            .orElse(null)

        if (sound == null) {
            println(
                "Sparkling Mutuals: Unknown contest sound ${ContestConfig.contestSound}"
            )
            return
        }

        val wanted =
            max(
                0f,
                ContestConfig.contestSoundVolume / 100f
            )

        val copies = min(
            MAX_SOUND_COPIES,
            ceil(wanted.toDouble()).toInt()
        )

        for (i in 0 until copies) {
            val gain = min(
                1f,
                wanted - i
            )

            client.soundManager.play(
                SimpleSoundInstance.forUI(
                    sound,
                    1.0f,
                    gain
                )
            )
        }
    }

    private fun showTitle(
        client: Minecraft,
        remaining: Int
    ) {
        client.gui.setTimes(
            5,
            60,
            15
        )

        client.gui.setTitle(
            Component.literal(
                "§c§lINCOMPLETE!"
            )
        )

        client.gui.setSubtitle(
            Component.literal(
                "§e${formatRemaining(remaining)} left to reach Uncommon"
            )
        )
    }

    fun isComplete(): Boolean {
        return complete
    }

    fun currentTier(): String {
        return tier
    }

    fun currentPoints(): Int {
        return points
    }

    fun isActive(): Boolean {
        return windowKey != -1L && !betweenContests()
    }

    fun secondsIntoWindow(): Int {
        return secondsIntoWindow
    }

    fun formatRemaining(seconds: Int): String {
        return String.format(
            "%d:%02d",
            seconds / 60,
            seconds % 60
        )
    }

    private fun isTierName(word: String): Boolean {
        val upper = word.uppercase()

        return upper == "COMMON" ||
                completeTiers.contains(upper)
    }

    private fun parseInt(token: String): Int {
        var text = token
            .trim()
            .lowercase()
            .replace(",", "")

        val scale = when {
            text.endsWith("k") -> {
                text = text.dropLast(1)
                1000.0
            }

            text.endsWith("m") -> {
                text = text.dropLast(1)
                1_000_000.0
            }

            else -> 1.0
        }

        return try {
            kotlin.math.round(
                text.toDouble() * scale
            ).toInt()
        } catch (_: NumberFormatException) {
            -1
        }
    }

    private val TIER_ROW =
        Regex("""^\s*([A-Za-z]+)\s+with\s+(\S+)""")

    private const val MAX_SOUND_COPIES = 20
}