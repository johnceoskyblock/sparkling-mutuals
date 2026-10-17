package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.SparklingEntry
import net.johnceo.sparklingmutuals.history.SparklingLog
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTracking

/** Streaks since any party Sparkling credited during a recorded or current Safari run. */
object SafariSinceReply {
    private fun label(unique: Boolean) = if (unique) "unique sparkling" else "sparkling"
    fun format(player: String, unique: Boolean, runs: List<RunRecord>, entries: List<SparklingEntry>, active: SafariRun? = null): String {
        val validRuns = runs.filter { it.start > 0 }.sortedBy { it.start }
        fun credited(entry: SparklingEntry): Boolean {
            if (entry.at <= 0) return false
            if (active != null && entry.at >= active.startedAt && (active.endedAt == null || entry.at <= active.endedAt!!)) return true
            var lo = 0; var hi = validRuns.size
            while (lo < hi) { val mid = (lo + hi) ushr 1; if (validRuns[mid].start <= entry.at) lo = mid + 1 else hi = mid }
            val run = validRuns.getOrNull(lo - 1) ?: return false
            if (run.length > 0) return entry.at - run.start <= run.length
            // An incomplete imported end time cannot prove membership by date alone.
            // Its run-specific Sparkling list can still establish the credit.
            return run.sparklings.any { it.equals("${entry.player}:${entry.critter}", true) }
        }
        val last = entries.filter { (!unique || it.unique) && credited(it) }.maxByOrNull { it.at }
        val label = label(unique)
        if (last == null) return "$player: No $label in history (${validRuns.size} runs tracked)."
        val count = validRuns.count { it.start > last.at }
        return "$player: $count run${if (count == 1) "" else "s"} since last $label: ${last.critter} caught by ${last.player}."
    }
    /** Called on the client thread before the async reply pipeline, so histories cannot race an import/save. */
    fun current(command: PartyCommand) = format(command.ign, command.kind == PartyCommandKind.SINCE_UNIQUE,
        RunHistory.runs, SparklingLog.entries, SafariTracking.ledger.current)
    fun matches(command: PartyCommand, body: String): Boolean {
        val name = Regex.escape(command.ign)
        val label = label(command.kind == PartyCommandKind.SINCE_UNIQUE)
        return Regex("^$name: (?:[0-9]+ runs? since last $label: .+ caught by [A-Za-z0-9_]{1,16}\\.|No $label in history \\([0-9]+ runs tracked\\)\\.)$",
            RegexOption.IGNORE_CASE).matches(body)
    }
}
