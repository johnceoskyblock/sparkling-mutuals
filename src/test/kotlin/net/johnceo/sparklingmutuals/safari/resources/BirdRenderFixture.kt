package net.johnceo.sparklingmutuals.safari.resources

import java.util.WeakHashMap
import net.johnceo.sparklingmutuals.safari.SafariRun

/** A deterministic current rendered scene for existing completion/PB fixtures, not server spawn chat. */
private val birdScenes = WeakHashMap<SafariBirdLedger, MutableMap<String, Int>>()
fun SafariBirdLedger.renderBird(species: String) {
    val totals = birdScenes.getOrPut(this) { linkedMapOf() }
    totals.merge(species, if (species == "Macaw") 2 else 1, Int::plus)
    render({ 0 }, totals.flatMap { (name, amount) -> List(amount) { name } })
}
fun SafariRun.scanRenderedBirds(drops: Int = 0) {
    val scene = birdScenes[birds].orEmpty()
    scanBirdSources(drops, scene.flatMap { (name, amount) -> List((amount - count(name)).coerceAtLeast(0)) { name } })
}
