package net.johnceo.sparklingmutuals.config

/** Screen geometry in logical pixels. Text wrapping is supplied by Minecraft's active font. */
internal class ReleaseHighlightsLayout(width: Int, height: Int, rangeFitsHeader: Boolean) {
    val compact = height < 244
    private val panelWidth = (width - 24).coerceIn(1, 580)
    private val panelHeight = (height - if (compact) 16 else 24).coerceIn(1, 420)
    val left = (width - panelWidth) / 2
    val right = left + panelWidth
    val top = (height - panelHeight) / 2
    val bottom = top + panelHeight
    val rangeInline = rangeFitsHeader
    val headerBottom = top + when {
        compact -> if (rangeInline) 48 else 62
        rangeInline -> 58
        else -> 74
    }
    val footerTop = bottom - if (compact) 34 else 44
    val contentLeft = left + 16
    val contentRight = right - 24
    val contentTop = headerBottom + if (compact) 6 else 8
    val contentBottom = footerTop - if (compact) 6 else 8
    val buttonRight = right - 16
    val buttonY = footerTop + if (compact) 6 else 11
    val trackX = right - 12

    private val gap = 8
    private val columns = if (contentRight - contentLeft >= 408) 2 else 1
    private val columnWidth = (contentRight - contentLeft - gap * (columns - 1)) / columns
    val textWidth = (columnWidth - 28).coerceAtLeast(1)

    data class Card(val index: Int, val left: Int, val right: Int, val top: Int, val height: Int)
    data class Section(val labelTop: Int, val cards: List<Card>)
    data class Content(val sections: List<Section>, val height: Int)

    fun content(releases: List<List<Int>>, labelHeight: Int): Content {
        var y = 0
        val sections = releases.map { heights ->
            val labelTop = y
            y += labelHeight + 10
            val cards = heights.withIndex().chunked(columns).flatMap { row ->
                val height = row.maxOf { it.value }
                val positioned = row.mapIndexed { column, item ->
                    val x = contentLeft + column * (columnWidth + gap)
                    Card(item.index, x, x + columnWidth, y, height)
                }
                y += height + gap
                positioned
            }
            y += 8
            Section(labelTop, cards)
        }
        return Content(sections, (y - 8).coerceAtLeast(0))
    }
}
