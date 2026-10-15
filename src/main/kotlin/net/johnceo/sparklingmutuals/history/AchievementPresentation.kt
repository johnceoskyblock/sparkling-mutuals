package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.history.achievements.LegacyAward
import net.johnceo.sparklingmutuals.history.achievements.AchievementCalibration

data class AchievementProgress(val value: Double?, val goal: Double, val unit: String, val coverage: String, val modeled: Boolean) {
    val reached: Boolean get() = value != null && value.isFinite() && goal > 0 && value >= goal
}
data class AchievementRow(val name: String, val description: String, val detail: String, val fraction: Double?, val unlockedAt: Long?, val progressLabel: String = "")
data class AchievementTotals(val currentEarned: Int, val currentTotal: Int, val legacyEarned: Int)
enum class AchievementFilter(val label: String) { ALL("All"), UNLOCKED("Unlocked"), LOCKED("Locked"), LEGACY("Legacy") }
object AchievementPresentation {
    fun visible(filter: AchievementFilter, unlocked: Boolean): Boolean = when (filter) {
        AchievementFilter.ALL -> true
        AchievementFilter.UNLOCKED -> unlocked
        AchievementFilter.LOCKED -> !unlocked
        AchievementFilter.LEGACY -> false
    }
    val rarityOrder: Comparator<Achievement> = compareBy { it.tier.displayRank }
    private val baselineHazard = AchievementCalibration.hazard(AchievementCalibration.rates.values.sum())
    fun current(a: Achievement, p: AchievementProgress, at: Long?): AchievementRow {
        if (a.secret && at == null) return AchievementRow("???", "Secret achievement: keep playing to find out.", "", null, null)
        val label = when {
            p.value == null -> "Unknown"
            p.modeled -> "${number(p.value / baselineHazard)}/${number(p.goal / baselineHazard)} modeled full runs"
            else -> "${number(p.value)}/${number(p.goal)} ${p.unit}"
        }
        val detail = when {
            p.value == null -> "Unknown ${p.unit}; ${p.coverage}"
            p.modeled -> "$label equivalent; ${number(p.value)}/${number(p.goal)} modeled hazard. ${p.coverage}. Model exposure, not actual runs or personal odds."
            else -> "$label; ${p.coverage}"
        }
        return AchievementRow(a.name, a.description, detail,
            if (at != null || !a.hasProgress || p.value == null) null else (p.value / p.goal).coerceIn(0.0, 1.0), at, label)
    }
    fun legacy(a: LegacyAward): AchievementRow = AchievementRow("Legacy: ${a.id}", "Original criterion: ${a.originalGoal}", "Legacy ${a.originalTier}; ${a.reason}", null, a.unlockedAt)
    fun totals(all: List<Achievement>, unlocked: Map<String, Long>, legacy: List<LegacyAward>) = AchievementTotals(all.count { it.id in unlocked }, all.size, legacy.size)
    private fun number(n: Double) = if (n % 1.0 == 0.0) n.toLong().toString() else String.format(java.util.Locale.ROOT, "%.3f", n)
}
