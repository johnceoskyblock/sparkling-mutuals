package net.johnceo.sparklingmutuals.sync

/** Last server-confirmed fairness for the same party, retained while a new visit reconciles. */
internal class HotspotWinHistory {
    private var roster = emptySet<String>()
    private var wins = emptyMap<String, Int>()

    fun counts(members: Set<String>): Map<String, Int> {
        if (roster != members) {
            roster = members.toSet()
            wins = emptyMap()
        }
        return wins
    }

    fun remember(members: Set<String>, counts: Map<String, Int>) {
        roster = members.toSet()
        wins = counts.filterKeys { it in members }
    }
}
