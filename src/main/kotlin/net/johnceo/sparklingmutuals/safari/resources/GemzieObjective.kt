package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.automation.GemzieGem
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

/** Loaded podium blocks are authoritative; remember placements through unloading. */
class GemzieObjective {
    val placed = mutableSetOf<GemzieGem>()
    private val acquired = mutableSetOf<GemzieGem>()
    val ready get() = (acquired + placed).size == 3
    val opened get() = placed.size == 3
    fun inventory(stacks: List<Pair<String,Int>>) {
        GemzieGem.entries.filterTo(acquired) { gem -> stacks.any { it.first == gem.label && it.second > 0 } }
    }
    fun pickup(raw: String) {
        val type=FloorPickup.parse(raw)?.type ?: return
        GemzieGem.entries.firstOrNull { it.label == type.label }?.let(acquired::add)
    }
    fun observe(gem: GemzieGem, matchingSupport: Boolean, head: Boolean) {
        if (matchingSupport && head) placed.add(gem)
    }
    fun tick(client: Minecraft) {
        val level=client.level ?: return
        GemzieGem.entries.forEach { gem ->
            val pos=BlockPos(gem.x,gem.y,gem.z)
            if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) return@forEach
            val support=when(gem) {
                GemzieGem.LIME -> Blocks.VERDANT_FROGLIGHT
                GemzieGem.ORANGE -> Blocks.OCHRE_FROGLIGHT
                GemzieGem.PURPLE -> Blocks.PEARLESCENT_FROGLIGHT
            }
            observe(gem,level.getBlockState(pos).`is`(support),level.getBlockState(pos.above()).`is`(Blocks.PLAYER_HEAD))
        }
    }
    fun placed(type: FloorDropType) = GemzieGem.entries.any { it in placed && it.label == type.label }
}
