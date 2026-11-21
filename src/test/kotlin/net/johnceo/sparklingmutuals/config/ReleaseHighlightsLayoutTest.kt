package net.johnceo.sparklingmutuals.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReleaseHighlightsLayoutTest {
    @Test fun `compact screens stack features and keep the settings button outside the scroll area`() {
        val layout = ReleaseHighlightsLayout(320, 180, rangeFitsHeader = false)
        val content = layout.content(listOf(listOf(65, 85), listOf(70)), 12)
        val cards = content.sections.flatMap { it.cards }
        assertTrue(layout.contentBottom - layout.contentTop >= 50)
        assertTrue(layout.contentBottom < layout.buttonY)
        assertTrue(layout.buttonY + 22 < layout.bottom)
        assertTrue(cards.all { it.left == layout.contentLeft && it.right == layout.contentRight })
        assertTrue(cards.zipWithNext().all { (first, next) -> first.top + first.height < next.top })
        assertTrue(cards.last().top + cards.last().height <= content.height)
    }

    @Test fun `wide screens align card rows and retain release boundaries`() {
        for ((width, height) in listOf(640 to 360, 960 to 540)) {
            val layout = ReleaseHighlightsLayout(width, height, rangeFitsHeader = true)
            val content = layout.content(listOf(listOf(60, 75, 51), listOf(63, 44)), 12)
            val (first, second, third) = content.sections.first().cards
            assertEquals(first.top, second.top)
            assertEquals(75, first.height)
            assertTrue(first.right < second.left && second.right <= layout.contentRight)
            assertTrue(third.top > first.top + first.height)
            assertTrue(content.sections.last().labelTop > third.top + third.height)
            assertTrue(layout.trackX > layout.contentRight)
        }
    }
}
