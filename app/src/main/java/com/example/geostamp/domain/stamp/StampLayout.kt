package com.example.geostamp.domain.stamp

import kotlin.math.max
import kotlin.math.min

/** Measures text width. Android uses Paint; unit tests use a simple fake. */
interface TextMeasurer {
    fun width(text: String, textSizePx: Float): Float
}

data class PlacedLine(val text: String, val x: Float, val baselineY: Float)

/** Where everything goes, in image pixels. The box is a full-width bar at the bottom. */
data class StampLayout(
    val textSizePx: Float,
    val boxLeft: Float,
    val boxTop: Float,
    val boxRight: Float,
    val boxBottom: Float,
    val lines: List<PlacedLine>
) {
    val boxHeight: Float get() = boxBottom - boxTop
}

object StampLayoutCalculator {
    const val MIN_TEXT_SIZE_PX = 8f
    private const val LINE_HEIGHT_FACTOR = 1.25f

    fun compute(
        imageWidth: Int,
        imageHeight: Int,
        lines: List<String>,
        config: StampConfig,
        measurer: TextMeasurer
    ): StampLayout {
        require(imageWidth > 0 && imageHeight > 0) { "Image size must be positive" }
        val width = imageWidth.toFloat()
        val height = imageHeight.toFloat()
        val textSize = max(MIN_TEXT_SIZE_PX, width * config.textSizeFraction)
        val padding = max(0f, width * config.paddingFraction)
        val maxLineWidth = max(1f, width - 2f * padding)

        val wrapped = lines.flatMap { wrap(it, textSize, maxLineWidth, measurer) }
        if (wrapped.isEmpty()) {
            return StampLayout(textSize, 0f, height, width, height, emptyList())
        }

        val lineHeight = textSize * LINE_HEIGHT_FACTOR
        val boxHeight = min(height, wrapped.size * lineHeight + 2f * padding)
        val boxTop = height - boxHeight

        val placed = wrapped.mapIndexed { index, text ->
            val textWidth = measurer.width(text, textSize)
            val x = when (config.alignment) {
                StampAlignment.START -> padding
                StampAlignment.CENTER -> (width - textWidth) / 2f
                StampAlignment.END -> width - padding - textWidth
            }
            val lineTop = boxTop + padding + index * lineHeight
            // Roughly centres the glyphs inside the line height.
            PlacedLine(text, x, lineTop + lineHeight / 2f + textSize * 0.35f)
        }
        return StampLayout(textSize, 0f, boxTop, width, height, placed)
    }

    /** Word-wraps one line to maxWidth. A single word that is too wide is split by character. */
    internal fun wrap(
        text: String,
        textSize: Float,
        maxWidth: Float,
        measurer: TextMeasurer
    ): List<String> {
        val words = text.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (measurer.width(candidate, textSize) <= maxWidth) {
                current = candidate
                continue
            }
            if (current.isNotEmpty()) {
                result += current
                current = ""
            }
            if (measurer.width(word, textSize) <= maxWidth) {
                current = word
            } else {
                var chunk = StringBuilder()
                var i = 0
                while (i < word.length) {
                    val codePoint = word.codePointAt(i)
                    val piece = String(Character.toChars(codePoint))
                    i += Character.charCount(codePoint)
                    if (chunk.isNotEmpty() && measurer.width(chunk.toString() + piece, textSize) > maxWidth) {
                        result += chunk.toString()
                        chunk = StringBuilder()
                    }
                    chunk.append(piece)
                }
                current = chunk.toString()
            }
        }
        if (current.isNotEmpty()) result += current
        return result
    }
}
