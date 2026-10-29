package com.example.geostamp.domain.maps

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlin.math.cos

/** One 256x256 map tile and where it is drawn inside the square picture. */
class TileRef(val x: Int, val y: Int, val drawLeft: Int, val drawTop: Int)

class TileWindow(val zoom: Int, val size: Int, val tiles: List<TileRef>)

/**
 * Standard "slippy map" tile maths (used by OpenStreetMap). Picks the few tiles that cover a
 * small square picture centred on a coordinate. Pure Kotlin, unit tested.
 */
object TileMath {
    const val TILE = 256
    private const val MAX_LAT = 85.0511

    /** Position of a coordinate on the whole-world map at this zoom, in pixels. */
    fun worldPixels(latitude: Double, longitude: Double, zoom: Int): Pair<Double, Double> {
        val lat = latitude.coerceIn(-MAX_LAT, MAX_LAT)
        val scale = TILE * (1 shl zoom).toDouble()
        val x = (longitude + 180.0) / 360.0 * scale
        val rad = Math.toRadians(lat)
        val y = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * scale
        return x to y
    }

    fun window(latitude: Double, longitude: Double, zoom: Int, size: Int): TileWindow {
        require(zoom in 0..19 && size > 0) { "Bad zoom or size" }
        val (px, py) = worldPixels(latitude, longitude, zoom)
        // Rounding removes tiny floating-point noise so a point exactly on a tile edge is not split in two.
        val left = Math.round((px - size / 2.0) * 1_000_000.0) / 1_000_000.0
        val top = Math.round((py - size / 2.0) * 1_000_000.0) / 1_000_000.0
        val count = 1 shl zoom
        val firstX = floor(left / TILE).toInt()
        val lastX = floor((left + size - 1) / TILE).toInt()
        val firstY = floor(top / TILE).toInt()
        val lastY = floor((top + size - 1) / TILE).toInt()
        val tiles = mutableListOf<TileRef>()
        for (ty in firstY..lastY) {
            if (ty < 0 || ty >= count) continue
            for (tx in firstX..lastX) {
                val wrappedX = ((tx % count) + count) % count
                tiles.add(TileRef(wrappedX, ty, (tx * TILE - left).roundToInt(), (ty * TILE - top).roundToInt()))
            }
        }
        return TileWindow(zoom, size, tiles)
    }
}
