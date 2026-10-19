package net.johnceo.sparklingmutuals.safari.shots

/** A client invocation boundary, never a time-based guess or assertion of server acceptance. */
class ShotInputCycles(
    private val input: (ShotInputToken,ShotHand,ShotVariant,ShotUseRoute,Boolean)->Unit = { _,_,_,_,_ -> },
    private val continuation: (ShotInputToken,ShotUseResult,ShotUseRoute)->Unit = { _,_,_ -> }
) {
    private var serial=0L
    private var active=false
    private val tokens=mutableMapOf<Pair<ShotHand,ShotVariant>,ShotInputToken>()
    private val routes=mutableMapOf<ShotInputToken,ShotUseRoute>()
    fun begin() { end();active=true }
    fun observe(hand: ShotHand,variant: ShotVariant,route: ShotUseRoute): ShotInputToken {
        val key=hand to variant
        val previous=if(active) tokens[key] else null
        val token=previous ?: ShotInputToken("s${++serial}")
        routes[token]=route
        if (previous==null) { if(active) tokens[key]=token; input(token,hand,variant,route,!active) }
        return token
    }
    fun returned(token: ShotInputToken,result: ShotUseResult) {
        val route=routes.remove(token) ?: return
        continuation(token,result,route)
    }
    fun end() { active=false;tokens.clear();routes.clear() }
}
