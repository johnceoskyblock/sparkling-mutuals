package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.automation.GemzieGem
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/** Loaded podium blocks are authoritative; remember placements through unloading. */
class GemzieObjective {
    val placed = mutableSetOf<GemzieGem>()
    private val acquired = mutableSetOf<GemzieGem>()
    private val localPlaced = mutableSetOf<GemzieGem>()
    private val sharedPlaced = mutableSetOf<GemzieGem>()
    private val observedEmpty = mutableSetOf<GemzieGem>()
    var sharedOpen = false
        private set
    val locallyPlaced get() = localPlaced.toSet()
    fun shared(mask: Int,open: Boolean) {
        listOf(GemzieGem.LIME,GemzieGem.ORANGE,GemzieGem.PURPLE).forEachIndexed { i,gem -> if(mask and (1 shl i)!=0) sharedPlaced.add(gem) }
        placed.addAll(sharedPlaced - observedEmpty)
        sharedOpen=sharedOpen || open
    }
    val ready get() = (acquired + placed).size == 3
    val opened get() = placed.size == 3 || sharedOpen
    fun inventory(stacks: List<Pair<String,Int>>) {
        GemzieGem.entries.filterTo(acquired) { gem -> stacks.any { it.first == gem.label && it.second > 0 } }
    }
    fun lostOnDeath(items: Map<String,Int>) {
        acquired.removeAll { (items[it.label] ?: 0)>0 && it !in placed }
    }
    fun pickup(raw: String) {
        val type=FloorPickup.parse(raw)?.type ?: return
        GemzieGem.entries.firstOrNull { it.label == type.label }?.let(acquired::add)
    }
    fun chat(text: String) {
        GemzieGem.entries.firstOrNull { text.startsWith("You placed the ${it.label} on its podium!") }
            ?.let { observe(it, true, true) }
    }
    fun observe(gem: GemzieGem, matchingSupport: Boolean, head: Boolean, confirmedEmpty: Boolean = false) {
        if (!matchingSupport) return
        if (head) {
            observedEmpty.remove(gem)
            placed.add(gem);localPlaced.add(gem)
        } else if (confirmedEmpty) {
            observedEmpty.add(gem)
            placed.remove(gem);localPlaced.remove(gem)
        }
    }
    fun debug() = "local=$localPlaced received=$sharedPlaced confirmedEmpty=$observedEmpty sharedOpen=$sharedOpen"
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
            val player = client.player ?: return@forEach
            val nearby = player.distanceToSqr(gem.x + .5, gem.y + 1.5, gem.z + .5) <= 32.0 * 32.0
            val head = level.getBlockState(pos.above())
            if (!nearby) return@forEach
            // Like SafariUtils, confirm visible podium surfaces rather than hidden loaded blocks.
            val target = pos.above()
            val hit = level.clip(ClipContext(player.eyePosition, Vec3.atCenterOf(target),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
            val visible = hit.type == HitResult.Type.MISS || hit.blockPos == target
            if (visible) observe(gem,level.getBlockState(pos).`is`(support),head.`is`(Blocks.PLAYER_HEAD),head.isAir)
        }
    }
    fun podiumDebug(client: Minecraft) = GemzieGem.entries.joinToString { gem ->
        val pos = BlockPos(gem.x, gem.y, gem.z)
        val level = client.level
        "${gem.name}(loaded=${level?.hasChunkAt(pos)},base=${level?.getBlockState(pos)},top=${level?.getBlockState(pos.above())})"
    }
    fun placed(type: FloorDropType) = GemzieGem.entries.any { it in placed && it.label == type.label }
}
