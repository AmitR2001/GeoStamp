package com.example.geostamp.domain.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardLayoutTest {
    private val measurer = object : TextMeasurer {
        override fun width(text: String, textSizePx: Float) = text.length * textSizePx * 0.5f
    }
    private val text = CardText(
        title = "Example High School",
        bodyLines = listOf("Riverside, Sample City, Example State", "Lat 12.111111\u00B0 Long 45.222222\u00B0", "Thursday, 22/10/2026 11:39 AM"),
        noteLine = "Note : Visit 3"
    )

    private fun layout(hasMap: Boolean, width: Int = 1000, height: Int = 1400, t: CardText = text) =
        CardLayoutCalculator.compute(width, height, t, hasMap, StampConfig(layoutStyle = StampLayoutStyle.CARD), measurer)

    @Test
    fun the_card_sits_at_the_bottom_inside_a_margin() {
        val l = layout(hasMap = true)
        assertEquals(25f, l.left, 0.01f)
        assertEquals(975f, l.right, 0.01f)
        assertEquals(1375f, l.bottom, 0.01f)
        assertTrue(l.top > 0f && l.top < l.bottom)
    }

    @Test
    fun the_map_is_a_square_on_the_left_and_text_starts_after_it() {
        val l = layout(hasMap = true)
        val map = l.map
        assertNotNull(map)
        assertEquals(l.left + 25f, map!!.left, 0.01f)
        assertTrue(map.size >= 160f - 0.01f && map.size <= 300f + 0.01f)
        assertTrue(map.top >= l.top && map.top + map.size <= l.bottom + 0.01f)
        l.lines.forEach { assertTrue(it.x >= map.left + map.size) }
    }

    @Test
    fun without_a_map_the_text_uses_the_whole_card() {
        val l = layout(hasMap = false)
        assertNull(l.map)
        assertEquals(l.left + 25f, l.lines.first().x, 0.01f)
    }

    @Test
    fun the_title_is_bigger_and_bold() {
        val l = layout(hasMap = true)
        val title = l.lines.first()
        assertTrue(title.bold)
        assertTrue(title.textSizePx > l.lines.last().textSizePx)
        assertTrue(l.lines.drop(1).none { it.bold })
    }

    @Test
    fun lines_run_top_to_bottom_inside_the_card() {
        val l = layout(hasMap = true)
        val ys = l.lines.map { it.baselineY }
        assertEquals(ys.sorted(), ys)
        assertTrue(ys.first() > l.top)
        assertTrue(ys.last() < l.bottom)
    }

    @Test
    fun long_lines_wrap_to_fit_beside_the_map() {
        val long = CardText("Title", listOf("word ".repeat(40).trim()), null)
        val l = layout(hasMap = true, t = long)
        val map = l.map!!
        val textRight = l.right - 25f
        l.lines.forEach { assertTrue(it.x + measurer.width(it.text, it.textSizePx) <= textRight + 0.5f) }
        assertTrue(l.lines.size > 2)
        assertTrue(map.size <= 300f + 0.01f)
    }

    @Test
    fun a_tiny_image_still_gets_a_valid_layout() {
        val l = layout(hasMap = true, width = 100, height = 100)
        assertTrue(l.bottom <= 100f)
        assertTrue(l.top >= 0f)
    }
}
