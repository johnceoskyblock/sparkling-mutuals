package net.johnceo.sparklingmutuals.safari.esp

import net.minecraft.core.BlockPos

/** A floor reward has three string displays in one block. Entity order carries no visual information. */
internal object VisibleFloorDrops {
    fun group(markers: List<EspDrop>): Map<BlockPos, List<Int>> =
        markers.groupBy({ BlockPos(it.x, it.y, it.z) }, EspDrop::id)
            .filterValues { ids -> ids.size == 3 }
}
