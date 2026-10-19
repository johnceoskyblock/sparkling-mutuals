package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariRun

/** Unit fixtures that already assume complete initial knowledge must now state that assumption.
 * No grants, floor slots, rendered models or captures are supplied here. Real roster/source
 * acquisition and unknown evidence are exercised separately by the issue68 integration tests. */
fun SafariRun.knownStartingEvidence(): SafariRun = apply {
    resources.contributions.roster(emptySet())
    resourceCoverage(true)
    gimmiegoldCheck.sourceEvidence(true, 0)
}
