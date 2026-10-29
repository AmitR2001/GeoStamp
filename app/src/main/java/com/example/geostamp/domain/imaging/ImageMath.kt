package com.example.geostamp.domain.imaging

import kotlin.math.max
import kotlin.math.roundToInt

/** Size calculations for loading big photos without running out of memory. */
object ImageMath {

    /** Power-of-two sample size so the decoded long edge is still at least maxLongEdge. */
    fun sampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
        require(width > 0 && height > 0 && maxLongEdge > 0) { "Sizes must be positive" }
        val longEdge = max(width, height)
        var sample = 1
        while (longEdge / (sample * 2) >= maxLongEdge) sample *= 2
        return sample
    }

    /** Scales down to fit maxLongEdge, never scales up. Result is at least 1x1. */
    fun fitWithin(width: Int, height: Int, maxLongEdge: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxLongEdge > 0) { "Sizes must be positive" }
        val longEdge = max(width, height)
        if (longEdge <= maxLongEdge) return width to height
        val scale = maxLongEdge.toDouble() / longEdge
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }
}
