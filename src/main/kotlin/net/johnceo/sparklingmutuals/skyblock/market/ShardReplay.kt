package net.johnceo.sparklingmutuals.skyblock.market

import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.DAY
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.HOUR
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.STEP
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.WEEK
import net.johnceo.sparklingmutuals.skyblock.market.ShardHistoryParsing.percentile
import java.security.MessageDigest

internal data class ReplayWindow(val at: Long, val startAsk: Double, val target: Double, val held: Boolean,
    val touchMinutes: Double?, val endReturn: Double, val recentOnlyTarget: Double, val recentOnlyTouchMinutes: Double?)
internal data class ReplayMetrics(val total: Int, val holds: Int, val touches: Int, val medianMinutes: Double?, val downside: Int) {
    fun qualifies()=holds>=5 && touches.toDouble()/holds>=.6 && medianMinutes!=null && medianMinutes<=90 && downside.toDouble()/holds<=.1
}
internal data class EligibilityReport(val modelVersion: Int, val fingerprint: String, val training: ReplayMetrics,
    val holdout: ReplayMetrics, val windows: List<ReplayWindow>, val eligible: Boolean, val reason: String, val parameters: ModelParameters,
    val verifiedEvidence: Boolean, val usesKnownAvailability: Boolean, val policyVersion: Int, val validationEndAt: Long)

internal object ShardReplay {
    const val POLICY_VERSION=2
    const val VALIDATION_DAYS=3L
    /** Each decision sees only closed bins at that instant; a frozen target is assessed over a non-overlapping 3h window. */
    fun evaluate(samples: List<PriceSample>, splitAt: Long, parameters: ModelParameters=ModelParameters()): EligibilityReport {
        val sorted=samples.filter { it.ask.isFinite() && it.ask>0 }.sortedWith(compareBy({it.at},{it.resolutionMillis}))
        val fine=sorted.filter { it.resolutionMillis==STEP }
        val knownAvailability=sorted.isNotEmpty() && sorted.all {it.availableAt!=null}
        val verified=knownAvailability && sorted.all {it.basis==PriceBasis.VERIFIED_BEST_ASK && it.availableAt!!>=it.observedAt}
        val horizon=3*HOUR
        val validationEnd=splitAt+VALIDATION_DAYS*DAY
        val windows=mutableListOf<ReplayWindow>()
        if(fine.isNotEmpty()) {
            val first=(sorted.first().at+WEEK+horizon-1)/horizon*horizon
            val last=minOf(fine.last().at,validationEnd)-horizon
            var at=first
            while(at<=last) {
                if(at<splitAt && at+horizon>splitAt) {at+=horizon;continue}
                val history=ShardHistoryParsing.normalize(sorted,at,requireAvailability=knownAvailability)
                val target=ShardTargetModel.target(history,at,parameters)
                val initial=history.fine.lastOrNull()
                val future=ShardHistoryParsing.normalize(sorted,at+horizon).fine.filter {it.at>at}
                if(target.price!=null && initial!=null && at-initial.at<=900_000 && future.size>=29 && at+horizon-future.last().at<=900_000) {
                    val held=!target.falling && initial.ask<target.price*.995
                    val recentOnly=percentile(history.fine.filter {it.at>at-horizon}.map {it.ask},.6)
                    fun touch(price: Double)=future.firstOrNull {it.ask>=price}?.let {(it.at-at)/60_000.0}
                    windows.add(ReplayWindow(at,initial.ask,if(target.falling)initial.ask else target.price,held,
                        touch(target.price),future.last().ask/initial.ask-1,recentOnly,touch(recentOnly)))
                }
                at+=horizon
            }
        }
        val training=metrics(windows.filter {it.at+horizon<=splitAt})
        val holdout=metrics(windows.filter {it.at>=splitAt && it.at+horizon<=validationEnd})
        val enough=windows.size>=20 && windows.map {it.at/DAY}.distinct().size>=4
        val completeValidation=fine.lastOrNull()?.at?.let {it>=validationEnd}==true
        val eligible=verified && enough && completeValidation && training.qualifies() && holdout.qualifies()
        val digest=MessageDigest.getInstance("SHA-256")
        sorted.forEach {digest.update("${it.at},${it.ask},${it.resolutionMillis},${it.basis},${it.availableAt};".toByteArray(Charsets.UTF_8))}
        val fingerprint=digest.digest().joinToString("") {"%02x".format(it)}
        return EligibilityReport(ShardTargetModel.VERSION,fingerprint,training,holdout,windows.toList(),eligible,
            if(!verified)"Retrospective research only: historical price basis or actual availability unverified"
            else if(eligible)"Historical replay qualifies; target touches do not prove fills"
            else if(!completeValidation)"Three completed validation days are required"
            else if(!enough)"Insufficient complete replay windows" else "Training or holdout acceptance not met",parameters,verified,knownAvailability,POLICY_VERSION,validationEnd)
    }

    /** Candidates are fixed in advance. Only the training prefix can influence their selection. */
    fun selectTrainingParameters(samples: List<PriceSample>, splitAt: Long): ModelParameters? {
        val training=samples.filter {it.at<=splitAt}
        val weights=listOf(Triple(.6,.3,.1),Triple(.8,.15,.05),Triple(.95,.04,.01))
        val candidates=weights.flatMap {(hours,day,week)->listOf(.1,.2,.3,.4,.5,.6,.7,.8).map {ModelParameters(it,hours,day,week)}}
        return candidates.map {parameters -> parameters to evaluate(training,splitAt,parameters).training}
            .filter {it.second.qualifies()}
            .sortedWith(compareByDescending<Pair<ModelParameters,ReplayMetrics>> {it.second.touches.toDouble()/it.second.holds}
                .thenByDescending {it.second.holds}.thenBy {it.second.medianMinutes})
            .firstOrNull()?.first
    }

    internal fun metrics(rows: List<ReplayWindow>): ReplayMetrics {
        val held=rows.filter {it.held}
        val times=held.mapNotNull {it.touchMinutes}
        return ReplayMetrics(rows.size,held.size,times.size,if(times.isEmpty())null else ShardHistoryParsing.median(times),held.count {it.endReturn<-.05})
    }
}
