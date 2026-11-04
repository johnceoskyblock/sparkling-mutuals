package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.automation.GemzieGem
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

/** Loaded podium blocks are authoritative; remember placements through unloading. */
class GemzieObjective {
    val placed = mutableSetOf<GemzieGem>()
    private val acquired = mutableSetOf<GemzieGem>()
    private val localPlaced = mutableSetOf<GemzieGem>()
    var sharedOpen = false
        private set
    val locallyPlaced get() = localPlaced.toSet()
    fun shared(mask: Int,open: Boolean) {
        listOf(GemzieGem.LIME,GemzieGem.ORANGE,GemzieGem.PURPLE).forEachIndexed { i,gem -> if(mask and (1 shl i)!=0) placed.add(gem) }
        sharedOpen=sharedOpen || open
    }
    val ready get() = (acquired + placed).size == 3
    val opened get() = placed.size == 3 || sharedOpen
    fun inventory(stacks: List<Pair<String,Int>>) {
        GemzieGem.entries.filterTo(acquired) { gem -> stacks.any { it.first == gem.label && it.second > 0 } }
    }
    fun pickup(raw: String) {
        val type=FloorPickup.parse(raw)?.type ?: return
        GemzieGem.entries.firstOrNull { it.label == type.label }?.let(acquired::add)
    }
    fun observe(gem: GemzieGem, matchingSupport: Boolean, head: Boolean) {
        if (matchingSupport && head) { placed.add(gem);localPlaced.add(gem) }
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
