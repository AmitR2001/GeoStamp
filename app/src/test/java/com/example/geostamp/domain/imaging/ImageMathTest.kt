package com.example.geostamp.domain.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageMathTest {

    @Test
    fun small_images_are_not_sampled() {
        assertEquals(1, ImageMath.sampleSize(1000, 800, 4096))
    }

    @Test
    fun big_images_are_sampled_but_stay_at_least_the_target_size() {
        val sample = ImageMath.sampleSize(8000, 6000, 4096)
        assertEquals(1, sample) // 8000 / 2 = 4000 < 4096, so no sampling keeps full quality
        assertEquals(2, ImageMath.sampleSize(12000, 9000, 4096))
        assertTrue(12000 / ImageMath.sampleSize(12000, 9000, 4096) >= 4096)
    }

    @Test
    fun portrait_uses_the_long_edge() {
        assertEquals(2, ImageMath.sampleSize(9000, 12000, 4096))
    }

    @Test
    fun fit_within_never_scales_up() {
        assertEquals(1000 to 800, ImageMath.fitWithin(1000, 800, 4096))
    }

    @Test
    fun fit_within_keeps_aspect_ratio() {
        assertEquals(4096 to 3072, ImageMath.fitWithin(8000, 6000, 4096))
        assertEquals(3072 to 4096, ImageMath.fitWithin(6000, 8000, 4096))
    }

    @Test
    fun fit_within_is_never_smaller_than_one_pixel() {
        assertEquals(1 to 4096, ImageMath.fitWithin(1, 100000, 4096).let { it.first to it.second })
    }

    @Test(expected = IllegalArgumentException::class)
    fun zero_size_is_rejected() {
        ImageMath.sampleSize(0, 10, 100)
    }
}
