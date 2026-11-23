package net.johnceo.sparklingmutuals.safari.esp

import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VisibleFloorDropsTest {
    @Test fun `groups three displays by block regardless of consecutive IDs`() {
        val markers = listOf(EspDrop(32, 4, 51, 3), EspDrop(31, 4, 51, 4), EspDrop(7, 4, 51, 3), EspDrop(54, 4, 51, 3))
        assertEquals(mapOf(BlockPos(4, 51, 3) to listOf(32, 7, 54)), VisibleFloorDrops.group(markers))
        assertEquals(emptyMap<BlockPos, List<Int>>(), VisibleFloorDrops.group(markers.dropLast(1)))
    }
}
