package com.example.geostamp.data.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.example.geostamp.domain.maps.TileMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Makes the small square map picture for the "map card" stamp from OpenStreetMap tiles.
 * It downloads at most four 256-pixel tiles for a place, keeps them on the phone for two weeks,
 * and draws a pin and the required "(c) OpenStreetMap" credit. Returns null when offline.
 *
 * Requesting a tile tells OpenStreetMap roughly where the place is (to about 300 metres).
 */
class MapTileProvider(context: Context) {
    private val cacheRoot = File(context.applicationContext.cacheDir, "osm-tiles")

    suspend fun squareMap(latitude: Double, longitude: Double, zoom: Int = ZOOM, size: Int = SIZE): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                build(latitude, longitude, zoom, size)
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }

    private fun build(latitude: Double, longitude: Double, zoom: Int, size: Int): Bitmap {
        val window = TileMath.window(latitude, longitude, zoom, size)
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.rgb(233, 229, 220))
        for (tile in window.tiles) {
            val bytes = tileBytes(zoom, tile.x, tile.y)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IOException("A map tile could not be read.")
            canvas.drawBitmap(bitmap, tile.drawLeft.toFloat(), tile.drawTop.toFloat(), null)
            bitmap.recycle()
        }
        drawPin(canvas, size)
        drawCredit(canvas, size)
        return result
    }

    private fun tileBytes(zoom: Int, x: Int, y: Int): ByteArray {
        val file = File(cacheRoot, "$zoom/$x/$y.png")
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS) {
            return file.readBytes()
        }
        val connection = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("The map server answered ${connection.responseCode}.")
            }
            val bytes = connection.inputStream.use { it.readBytes() }
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            return bytes
        } finally {
            connection.disconnect()
        }
    }

    private fun drawPin(canvas: Canvas, size: Int) {
        val cx = size / 2f
        val tipY = size / 2f
        val headRadius = size * 0.065f
        val headY = tipY - headRadius * 2.2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(219, 68, 55) }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = size * 0.012f
        }
        val path = Path().apply {
            moveTo(cx - headRadius * 0.95f, headY + headRadius * 0.35f)
            lineTo(cx, tipY)
            lineTo(cx + headRadius * 0.95f, headY + headRadius * 0.35f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.drawCircle(cx, headY, headRadius, paint)
        canvas.drawCircle(cx, headY, headRadius, outline)
        canvas.drawCircle(cx, headY, headRadius * 0.4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    }

    /** OpenStreetMap's licence requires this credit to stay visible on the map picture. */
    private fun drawCredit(canvas: Canvas, size: Int) {
        val text = "\u00A9 OpenStreetMap"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = size * 0.058f
            typeface = Typeface.DEFAULT
        }
        val width = textPaint.measureText(text)
        val pad = size * 0.015f
        val left = size - width - pad * 2
        val top = size - textPaint.textSize - pad * 1.5f
        val background = Paint().apply { color = Color.argb(190, 255, 255, 255) }
        canvas.drawRect(left, top, size.toFloat(), size.toFloat(), background)
        canvas.drawText(text, left + pad, size - pad * 1.2f, textPaint)
    }

    companion object {
        const val ZOOM = 17
        const val SIZE = 256
        private const val TIMEOUT_MS = 6_000
        private const val MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000
        private const val USER_AGENT = "GeoStamp/1.0 (Android photo stamp app; personal use)"
    }
}
