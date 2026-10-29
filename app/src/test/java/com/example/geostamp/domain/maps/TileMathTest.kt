package com.example.geostamp.domain.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileMathTest {

    @Test
    fun the_world_centre_at_zoom_0_is_the_middle_of_the_one_tile() {
        val (x, y) = TileMath.worldPixels(0.0, 0.0, 0)
        assertEquals(128.0, x, 0.0001)
        assertEquals(128.0, y, 0.0001)
    }

    @Test
    fun zoom_doubles_the_world() {
        val (x, y) = TileMath.worldPixels(0.0, 0.0, 1)
        assertEquals(256.0, x, 0.0001)
        assertEquals(256.0, y, 0.0001)
    }

    @Test
    fun north_is_up() {
        val north = TileMath.worldPixels(60.0, 0.0, 3).second
        val south = TileMath.worldPixels(-60.0, 0.0, 3).second
        assertTrue(north < south)
    }

    @Test
    fun the_far_east_is_at_the_right_edge() {
        assertEquals(256.0 * 4, TileMath.worldPixels(0.0, 180.0, 2).first, 0.0001)
    }

    @Test
    fun a_picture_centred_on_a_tile_centre_needs_exactly_one_tile() {
        // longitude/latitude of the centre of tile (1, 1) at zoom 1 is (90, -66.51326...)
        val w = TileMath.window(-66.51326044, 90.0, 1, 256)
        assertEquals(1, w.tiles.size)
        assertEquals(0, w.tiles[0].drawLeft)
        assertEquals(0, w.tiles[0].drawTop)
    }

    @Test
    fun a_normal_place_needs_between_one_and_four_tiles_and_the_pin_is_covered() {
        val w = TileMath.window(12.111111, 45.222222, 17, 256)
        assertTrue(w.tiles.size in 1..4)
        // the picture centre (128,128) must lie inside one of the drawn tiles
        assertTrue(w.tiles.any { 128 >= it.drawLeft && 128 < it.drawLeft + 256 && 128 >= it.drawTop && 128 < it.drawTop + 256 })
        // and the drawn tiles together cover the whole picture
        assertTrue(w.tiles.minOf { it.drawLeft } <= 0)
        assertTrue(w.tiles.minOf { it.drawTop } <= 0)
        assertTrue(w.tiles.maxOf { it.drawLeft + 256 } >= 256)
        assertTrue(w.tiles.maxOf { it.drawTop + 256 } >= 256)
    }

    @Test
    fun tile_numbers_stay_inside_the_world() {
        val w = TileMath.window(10.0, 179.9999, 5, 256)
        assertTrue(w.tiles.all { it.x in 0 until 32 && it.y in 0 until 32 })
    }

    @Test
    fun poles_are_clamped_instead_of_failing() {
        val w = TileMath.window(90.0, 0.0, 4, 256)
        assertTrue(w.tiles.isNotEmpty())
    }
}
