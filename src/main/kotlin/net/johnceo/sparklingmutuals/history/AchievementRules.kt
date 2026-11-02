package net.johnceo.sparklingmutuals.history

import java.time.Instant
import java.time.ZoneId
import net.johnceo.sparklingmutuals.safari.SafariRoster

/** Read-only milestone calculations shared by achievements and the dex selector. */
object AchievementRules {
    fun doubleTrouble(catches: List<String>,self: String): Boolean = self.isNotBlank() && catches
        .filter { ':' in it && it.substringBefore(':').equals(self,true) }
        .mapNotNull { SafariRoster.named(it.substringAfter(':'))?.name }
        .groupingBy { it }.eachCount().values.any { it>=2 }
    data class Streak(val current: Int,val longest: Int)
    fun duplicateStreak(entries: List<SparklingEntry>): Streak {
        var current=0;var longest=0
        entries.sortedBy { it.at }.filter { SafariRoster.named(it.critter)!=null }.forEach {
            current=if(it.unique) 0 else current+1;longest=maxOf(longest,current)
        }
        return Streak(current,longest)
    }
    fun dexCounts(entries: List<SparklingEntry>,discoveries: Set<String> = emptySet()): Map<String,Int> {
        val counts=entries.mapNotNull { SafariRoster.named(it.critter)?.name }.groupingBy { it }.eachCount()
        return SafariRoster.all.associate { it.name to maxOf(counts[it.name] ?: 0,if(it.name in discoveries) 1 else 0) }
    }
    fun dex(counts: Map<String,Int>,required: Int)=counts.filterValues { it>=required }.keys
    fun fullDexCount(counts: Map<String,Int>)=SafariRoster.all.minOf { counts[it.name] ?: 0 }
    fun macawDrought(runs: List<RunRecord>): Streak {
        var current=0;var longest=0
        runs.sortedBy { it.start }.forEach {
            current=when {
                it.macaws>0 || (it.catches["Macaw"] ?: 0)>0 || (it.macawsSeen ?: 0)>0 -> 0
                it.allBirdFoodUsed==true && it.macawsSeen==0 -> current+1
                else -> 0
            }
            longest=maxOf(longest,current)
        }
        return Streak(current,longest)
    }
    fun mostDailyRuns(runs: List<RunRecord>,zone: ZoneId=ZoneId.systemDefault())=runs
        .filter { it.length>120_000 && HistoryQueries.keepRunLength(it.length) }
        .groupingBy { Instant.ofEpochMilli(it.start+it.length).atZone(zone).toLocalDate() }.eachCount().values.maxOrNull() ?: 0

    /** Gaps are run positions, not ticket uses; a second catch in the same run has gap zero. */
    fun personalSparklingGaps(runs: List<RunRecord>,own: List<SparklingEntry>,live: Pair<Long,Long>?=null): List<Int> {
        val windows=(runs.map { it.start to (it.start+it.length.coerceAtLeast(0)) }+listOfNotNull(live))
            .groupBy { it.first }.map { (start,values) -> start to values.maxOf { it.second } }.sortedBy { it.first }
        val positions=own.sortedBy { it.at }.map { entry -> windows.indexOfLast { entry.at in it.first..it.second }.takeIf { it>=0 } }
        return positions.zipWithNext().mapNotNull { (a,b) -> if(a==null || b==null) null else b-a }
    }
}

enum class SparklingDexType(val label: String,val required: Int) {
    SPARKLING("Sparkling dex",1),DUPEDEX("Dupedex",2),TRIPDEX("Tripdex",3),QUADDEX("Quaddex",4),
    QUINDEX("Quindex",5),SEXDEX("Sexdex (nice)",6),SEPTDEX("Septdex",7),OCTODEX("Octodex",8),
    NUEVADEX("Nuevadex",9),DECADEX("Decadex",10)
}

/** Pair a drop line with its broadcast; separate rapid drops of the same family still count. */
internal class DyeDropProofs {
    private val unmatched=mutableListOf<Pair<Boolean,Long>>()
    fun accept(text: String,now: Long): Boolean {
        unmatched.removeAll { now-it.second>5000 }
        val broadcast=text.startsWith("WOW!",true)
        val paired=unmatched.indexOfFirst { it.first!=broadcast }
        if(paired>=0) { unmatched.removeAt(paired);return false }
        unmatched.add(broadcast to now);return true
    }
}
