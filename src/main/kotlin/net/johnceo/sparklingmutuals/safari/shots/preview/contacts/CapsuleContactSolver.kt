package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
object CapsuleContactSolver {
    const val TIE = 1e-7
    fun trace(curve:ContactCurve,targets:List<ContactTarget>,coverage:ContactCoverage,scene:(ContactNode,ContactNode)->ContactSegmentScene):ContactTrace {
        val warnings=coverage.reasons.filterTo(linkedSetOf()){it in ContactWarnings.allowed}
        if(!coverage.complete && warnings.isEmpty()) warnings.add("scan_pending")
        if(curve.nodes.isEmpty()) return ContactTrace(emptyList(),null,curve.stop,warnings.toSet())
        val nodes=ArrayList<ContactNode>(); nodes.add(curve.nodes.first())
        // A known contact remains useful with partial targets, but cannot establish the nearest contact.
        // Unknown terrain still ends traversal before any candidate beyond it.
        fun finish(primary:ContactCandidate?,stop:ContactStop)=ContactTrace(nodes.toList(),primary,
            if(primary!=null || coverage.complete || stop in setOf(ContactStop.FLUID,ContactStop.UNLOADED,ContactStop.INVALID,ContactStop.WORK_LIMIT)) stop else ContactStop.TARGET_COVERAGE,warnings.toSet())
        for((a,b) in curve.nodes.zipWithNext()) {
            val environment=scene(a,b)
            var best=environment.terrain?.takeIf{it.position.finite() && it.parameter.isFinite() && it.parameter>=a.parameter-TIE && it.parameter<=b.parameter+TIE}
            for(target in targets.take(64)) {
                val f=ContactIntersections.entry(a.position,b.position,target.bounds) ?: continue
                val candidate=ContactCandidate(ContactKind.CRITTER,a.parameter+(b.parameter-a.parameter)*f,
                    a.position.lerp(b.position,f),null,target.key,target.proxy)
                if(best==null || candidate.parameter<best.parameter-TIE) best=candidate
                else if(kotlin.math.abs(candidate.parameter-best.parameter)<=TIE) warnings.add("contact_tie")
            }
            val boundary=environment.boundary?.takeIf { it.fraction.isFinite() && it.fraction in 0.0..1.0 }
            val unknownParameter=boundary?.let{a.parameter+(b.parameter-a.parameter)*it.fraction}
            if(boundary!=null && (best==null || unknownParameter!!<=best.parameter+TIE)) {
                if(unknownParameter!!>nodes.last().parameter) nodes.add(ContactNode(unknownParameter,a.position.lerp(b.position,boundary.fraction),a.observedRegion && b.observedRegion))
                return finish(null,boundary.reason)
            }
            if(best!=null) {
                if(best.parameter>nodes.last().parameter) nodes.add(ContactNode(best.parameter,best.position,a.observedRegion && b.observedRegion))
                return finish(best,ContactStop.CONTACT)
            }
            nodes.add(b)
        }
        return finish(null,curve.stop)
    }
}
