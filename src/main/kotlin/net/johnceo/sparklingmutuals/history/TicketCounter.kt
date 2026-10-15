package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.history.achievements.AdmissionEvidence
import net.johnceo.sparklingmutuals.history.achievements.AchievementScope
import net.johnceo.sparklingmutuals.safari.SafariRun

/** One authoritative admission fan-out, driven by the tracker's ordered chat/entry observations. */
object TicketCounter {
    private const val MANAGER = "[NPC] Safari Manager: "
    private val lines = setOf("Looks good to me. Have fun out there!", "I already saw your ticket, so you're free to go.")
    fun isTicket(text: String) = text.startsWith(MANAGER) && text.removePrefix(MANAGER).trim() in lines

    private var current: SafariRun? = null
    private val evidence = AdmissionEvidence(observeEntry = { id, scope, at -> Achievements.collectEntry(id, scope, at) }) { id, scope, at ->
        if (Achievements.collectAdmission(id, scope, at)) {
            SparklingLog.onTicket()
            current?.let(RunHistory::onTicket)
            Achievements.onTicket()
        }
    }
    fun acknowledge(text: String, at: Long) = evidence.acknowledge(text, at)
    fun confirmEntry(run: SafariRun, scope: AchievementScope?, at: Long) {
        current = run
        if (scope == null) evidence.confirmUnscopedEntry(run.runId, at)
        else evidence.confirmEntry(run.runId, scope, at)
    }
    fun leave() { current = null; evidence.leave() }
    fun disconnect() { current = null; evidence.disconnect(); Achievements.invalidateActivityIntervals() }
}
