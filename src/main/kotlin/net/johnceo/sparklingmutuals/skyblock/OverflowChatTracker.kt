/* Adapted from SkyHanni SkillApi and SkillProgress, LGPL-2.1. See licenses/SkyHanni-Skills-NOTICE.txt. */
package net.johnceo.sparklingmutuals.skyblock

import net.johnceo.sparklingmutuals.safari.SafariRules
import java.util.Locale

internal data class OverflowLevelChange(val skill: String, val old: Int, val new: Int) {
    fun message(numbers: Boolean): String {
        fun level(value: Int) = if (numbers) value.toString() else SkillOverflowMath.roman(value)
        val border = "§3§l---------------------------------------------"
        return "$border\n  §r§b§lSKILL LEVEL UP §3$skill §8${level(old)}➜§3${level(new)}\n\n$border"
    }
}

/** Uses absolute action-bar XP, never summed gain notifications or API requests. */
internal class OverflowChatTracker {
    private val levels = mutableMapOf<String, Int>()
    private val pattern = Regex("\\+[\\d.,]+ ([A-Za-z]+) \\(([\\d.,]+)/([\\d.,]+[kmbt]?)\\)", RegexOption.IGNORE_CASE)
    fun clear() = levels.clear()
    fun observe(raw: String): List<OverflowLevelChange> = buildList {
        for (match in pattern.findAll(SafariRules.strip(raw))) {
            val skill = match.groupValues[1].lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
            val cap = SkillOverflowMath.cap(skill) ?: continue
            val needed = SkillOverflowMath.number(match.groupValues[3]) ?: continue
            if (needed != 0L) { levels.remove(skill); continue }
            val current = SkillOverflowMath.number(match.groupValues[2]) ?: continue
            val base = SkillOverflowMath.totalFor(cap)
            if (current > Long.MAX_VALUE - base) continue
            val level = SkillOverflowMath.calculate(base + current, cap).level
            val previous = levels.put(skill, level) ?: continue // Initial observation is a baseline, not a level-up.
            if (level > previous && level > cap) add(OverflowLevelChange(skill, previous, level))
        }
    }
}
