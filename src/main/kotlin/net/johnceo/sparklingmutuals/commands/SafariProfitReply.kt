package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.history.BazaarPrices
import net.johnceo.sparklingmutuals.history.ProfitPeriods
import net.johnceo.sparklingmutuals.history.ProfitTotals
import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.SafariProfitPeriod

internal object SafariProfitReply {
    const val PREFIX = "[SM] Safari profit ("

    fun current(period: SafariProfitPeriod): String {
        val runs = ProfitPeriods.currentRuns(RunHistory.runs, period, RunHistory.sessionStarts.lastOrNull(), System.currentTimeMillis())
        return format(period, runs, ConfigManager.runCoinsInstaSell, RunHistory.priceChangePending)
    }

    fun format(period: SafariProfitPeriod, runs: List<RunRecord>, instaSell: Boolean, priceChangePending: Boolean = false): String {
        val basis = if (instaSell) "Insta-sell" else "Sell offer"
        val prefix = "$PREFIX${period.label}, $basis): "
        if (runs.isEmpty()) return prefix + "No recorded runs."
        val priced = runs.filter { it.coins >= 0 }
        if (priceChangePending || priced.isEmpty()) return prefix + "Bazaar prices unavailable."
        val totals = ProfitTotals.of(priced)
        val rate = if (totals.millis > 0) " | ${BazaarPrices.format(totals.perHour)} coins/h" else ""
        val runCount = if (priced.size == runs.size) "${totals.runs}" else "${totals.runs}/${runs.size}"
        return prefix + "${BazaarPrices.format(totals.coins)} coins$rate | $runCount ${if (runs.size == 1) "run" else "runs"}"
    }
}
