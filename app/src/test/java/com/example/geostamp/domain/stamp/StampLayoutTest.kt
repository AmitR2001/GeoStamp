package com.example.geostamp.domain.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StampLayoutTest {

    /** Every character is half as wide as the text size. Simple and predictable. */
    private val measurer = object : TextMeasurer {
        override fun width(text: String, textSizePx: Float) = text.length * textSizePx * 0.5f
    }

    private fun layout(
        lines: List<String>,
        width: Int = 1000,
        height: Int = 800,
        config: StampConfig = StampConfig()
    ) = StampLayoutCalculator.compute(width, height, lines, config, measurer)

    @Test
    fun text_size_scales_with_image_width() {
        assertEquals(32f, layout(listOf("a"), width = 1000).textSizePx, 0.01f)
        assertEquals(64f, layout(listOf("a"), width = 2000).textSizePx, 0.01f)
    }

    @Test
    fun text_size_never_goes_below_minimum() {
        assertEquals(StampLayoutCalculator.MIN_TEXT_SIZE_PX, layout(listOf("a"), width = 100).textSizePx, 0.01f)
    }

    @Test
    fun box_sits_at_the_bottom_and_spans_full_width() {
        val result = layout(listOf("one", "two", "three"))
        // text 32, line height 40, padding 25 -> 3 * 40 + 50 = 170
        assertEquals(170f, result.boxHeight, 0.01f)
        assertEquals(800f, result.boxBottom, 0.01f)
        assertEquals(630f, result.boxTop, 0.01f)
        assertEquals(0f, result.boxLeft, 0.01f)
        assertEquals(1000f, result.boxRight, 0.01f)
    }

    @Test
    fun start_alignment_uses_padding_as_x() {
        val result = layout(listOf("hello"), config = StampConfig(alignment = StampAlignment.START))
        assertEquals(25f, result.lines.single().x, 0.01f)
    }

    @Test
    fun end_alignment_touches_right_padding() {
        val result = layout(listOf("hello"), config = StampConfig(alignment = StampAlignment.END))
        val line = result.lines.single()
        assertEquals(1000f - 25f, line.x + measurer.width(line.text, result.textSizePx), 0.01f)
    }

    @Test
    fun center_alignment_is_centered() {
        val result = layout(listOf("hello"), config = StampConfig(alignment = StampAlignment.CENTER))
        val line = result.lines.single()
        val textWidth = measurer.width(line.text, result.textSizePx)
        assertEquals((1000f - textWidth) / 2f, line.x, 0.01f)
    }

    @Test
    fun long_line_wraps_without_losing_words() {
        val words = List(20) { "abcdefghi" }
        val result = layout(listOf(words.joinToString(" ")))
        assertTrue(result.lines.size > 1)
        val maxWidth = 1000f - 2f * 25f
        result.lines.forEach { assertTrue(measurer.width(it.text, result.textSizePx) <= maxWidth) }
        assertEquals(words, result.lines.flatMap { it.text.split(' ') })
    }

    @Test
    fun a_single_huge_word_is_split_by_character() {
        val word = "x".repeat(200)
        val result = layout(listOf(word))
        assertEquals(4, result.lines.size)
        assertEquals(word, result.lines.joinToString("") { it.text })
    }

    @Test
    fun blank_lines_are_skipped_and_empty_input_gives_empty_box() {
        val result = layout(listOf("", "   "))
        assertTrue(result.lines.isEmpty())
        assertEquals(0f, result.boxHeight, 0.01f)
    }

    @Test
    fun box_height_is_capped_at_image_height() {
        val result = layout(List(10) { "line" }, height = 100)
        assertEquals(0f, result.boxTop, 0.01f)
        assertEquals(100f, result.boxHeight, 0.01f)
    }

    @Test
    fun lines_are_placed_top_to_bottom() {
        val result = layout(listOf("a", "b", "c"))
        val ys = result.lines.map { it.baselineY }
        assertEquals(ys.sorted(), ys)
        assertTrue(ys.first() > result.boxTop)
        assertTrue(ys.last() < result.boxBottom)
    }
}
