package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.SafariMode

/** Short-lived current-run display evidence. Never written into capture/history ledgers. */
class SharedCritterProgress(private val clock: () -> Long = System::currentTimeMillis) {
    data class Evidence(val unique: Boolean, val checked: Boolean, val fullClear: Boolean, val checks: Int, val captures: Int)
    private data class Report(val sequence: Long, val expires: Long, val rows: Map<String, Evidence>)
    private val reports = mutableMapOf<String, Report>()
    fun replace(reporter: String, sequence: Long, rows: Map<String, Evidence>, ageMillis: Long = 0): Boolean {
        if (sequence <= 0 || sequence <= (reports[reporter]?.sequence ?: 0) || ageMillis !in 0 until TTL) return false
        reports[reporter] = Report(sequence, clock() + TTL - ageMillis, rows.toMap())
        return true
    }
    private fun current() = reports.values.filter { clock() < it.expires }
    fun complete(name: String, mode: SafariMode) = current().any { report -> report.rows[name]?.let {
        when (mode) { SafariMode.UNIQUE -> it.unique; SafariMode.SPARKLING -> it.checked; SafariMode.FULL_CLEAR -> it.fullClear }
    } == true }
    // Maxima are conservative: two teammates may have checked/caught the same entities.
    fun checks(name: String) = current().maxOfOrNull { it.rows[name]?.checks ?: 0 } ?: 0
    fun captures(name: String) = current().maxOfOrNull { it.rows[name]?.captures ?: 0 } ?: 0
    fun clear() = reports.clear()
    companion object { const val TTL = 45_000L }
}
