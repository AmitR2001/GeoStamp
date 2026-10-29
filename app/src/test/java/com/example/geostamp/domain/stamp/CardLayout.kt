package com.example.geostamp.domain.stamp

import kotlin.math.max
import kotlin.math.min

class CardLine(val text: String, val x: Float, val baselineY: Float, val textSizePx: Float, val bold: Boolean)
class MapRect(val left: Float, val top: Float, val size: Float)

/** Everything needed to draw the map card, in image pixels. */
class CardLayout(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val cornerRadius: Float,
    val map: MapRect?,
    val lines: List<CardLine>
)

/** Places the card, the square map picture and the text lines. Pure Kotlin, unit tested. */
object CardLayoutCalculator {
    private const val LINE_FACTOR = 1.28f
    private const val TITLE_FACTOR = 1.3f

    private class Item(val text: String, val size: Float, val bold: Boolean)

    fun compute(
        imageWidth: Int,
        imageHeight: Int,
        text: CardText,
        hasMap: Boolean,
        config: StampConfig,
        measurer: TextMeasurer
    ): CardLayout {
        require(imageWidth > 0 && imageHeight > 0) { "Image size must be positive" }
        val width = imageWidth.toFloat()
        val height = imageHeight.toFloat()
        val margin = width * 0.025f
        val pad = max(0f, width * config.paddingFraction)
        val bodySize = max(StampLayoutCalculator.MIN_TEXT_SIZE_PX, width * config.textSizeFraction)
        val titleSize = bodySize * TITLE_FACTOR
        val cardLeft = margin
        val cardRight = width - margin
        val innerWidth = max(1f, cardRight - cardLeft - 2f * pad)
        val minMap = width * 0.16f
        val maxMap = width * 0.30f

        var mapSize = if (hasMap) width * 0.22f else 0f
        var items = wrap(text, bodySize, titleSize, textWidth(innerWidth, mapSize, pad, hasMap), measurer)
        if (hasMap) {
            repeat(2) {
                mapSize = textHeight(items).coerceIn(minMap, maxMap)
                items = wrap(text, bodySize, titleSize, textWidth(innerWidth, mapSize, pad, hasMap), measurer)
            }
        }

        val blockHeight = textHeight(items)
        val contentHeight = max(blockHeight, if (hasMap) mapSize else 0f)
        val cardHeight = min(height - 2f * margin, contentHeight + 2f * pad)
        val cardBottom = height - margin
        val cardTop = cardBottom - cardHeight

        val mapRect = if (hasMap) MapRect(cardLeft + pad, cardTop + (cardHeight - mapSize) / 2f, mapSize) else null
        val textLeft = cardLeft + pad + (if (hasMap) mapSize + pad else 0f)
        var y = cardTop + (cardHeight - blockHeight) / 2f
        val placed = items.map { item ->
            val lineHeight = item.size * LINE_FACTOR
            val line = CardLine(item.text, textLeft, y + lineHeight / 2f + item.size * 0.35f, item.size, item.bold)
            y += lineHeight
            line
        }
        return CardLayout(cardLeft, cardTop, cardRight, cardBottom, width * 0.022f, mapRect, placed)
    }

    private fun textWidth(innerWidth: Float, mapSize: Float, pad: Float, hasMap: Boolean): Float =
        max(1f, innerWidth - (if (hasMap) mapSize + pad else 0f))

    private fun textHeight(items: List<Item>): Float = items.sumOf { (it.size * LINE_FACTOR).toDouble() }.toFloat()

    private fun wrap(text: CardText, bodySize: Float, titleSize: Float, maxWidth: Float, measurer: TextMeasurer): List<Item> {
        val items = mutableListOf<Item>()
        // Bold text is a little wider than the measured plain text, so the title gets some spare room.
        StampLayoutCalculator.wrap(text.title, titleSize, maxWidth * 0.94f, measurer).forEach { items += Item(it, titleSize, true) }
        text.bodyLines.forEach { line ->
            StampLayoutCalculator.wrap(line, bodySize, maxWidth, measurer).forEach { items += Item(it, bodySize, false) }
        }
        text.noteLine?.let { note ->
            StampLayoutCalculator.wrap(note, bodySize, maxWidth, measurer).forEach { items += Item(it, bodySize, false) }
        }
        return items
    }
}
