package com.example.geostamp.domain.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTest {
    @Test
    fun starts_empty_and_inactive() {
        val s = Selection()
        assertEquals(0, s.count)
        assertFalse(s.isActive)
    }

    @Test
    fun toggling_adds_then_removes() {
        val on = Selection().toggle("a").toggle("b")
        assertEquals(2, on.count)
        assertTrue(on.contains("a"))
        val off = on.toggle("a")
        assertFalse(off.contains("a"))
        assertTrue(off.isActive)
    }

    @Test
    fun unticking_the_last_photo_ends_selection_mode() {
        assertFalse(Selection().toggle("a").toggle("a").isActive)
    }

    @Test
    fun select_all_and_clear() {
        val all = Selection().selectAll(listOf("a", "b", "c"))
        assertEquals(3, all.count)
        assertEquals(0, all.clear().count)
    }

    @Test
    fun deleted_photos_drop_out_of_the_selection() {
        assertEquals(setOf("b", "c"), Selection(setOf("a", "b", "c")).retainOnly(listOf("b", "c", "d")).ids)
    }

    @Test
    fun selections_with_the_same_photos_are_equal() {
        assertEquals(Selection(setOf("a", "b")), Selection().toggle("b").toggle("a"))
    }
}
