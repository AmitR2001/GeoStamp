// geostamp-iteration: 7
package com.example.geostamp.data.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.geostamp.domain.stamp.CardLayoutCalculator
import com.example.geostamp.domain.stamp.StampCardFormatter
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampFormatter
import com.example.geostamp.domain.stamp.StampLayoutCalculator
import com.example.geostamp.domain.stamp.StampLayoutStyle
import com.example.geostamp.domain.stamp.TextMeasurer
import java.util.Locale

/**
 * Draws a stamp onto a COPY of the source bitmap. The source is never modified and
 * no EXIF data is touched (this class only works with pixels).
 */
object StampRenderer {

    /** Draws the stamp in the layout the settings ask for. [map] is only used by the card layout. */
    fun renderStamp(
        source: Bitmap,
        content: StampContent,
        config: StampConfig,
        map: Bitmap? = null,
        locale: Locale = Locale.getDefault()
    ): Bitmap =
        if (config.layoutStyle == StampLayoutStyle.CARD) {
            renderCard(source, content, config, map, locale)
        } else {
            render(source, StampFormatter.format(content, config, locale), config)
        }

    /** The bar layout: lines of text on a bar across the bottom. */
    fun render(source: Bitmap, lines: List<String>, config: StampConfig): Bitmap {
        val output = copyOf(source)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.foregroundColor
            typeface = Typeface.DEFAULT
        }
        val layout = StampLayoutCalculator.compute(
            output.width, output.height, lines, config, PaintTextMeasurer(textPaint)
        )
        if (layout.lines.isEmpty()) return output

        val backgroundPaint = Paint().apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
        }
        val canvas = Canvas(output)
        canvas.drawRect(layout.boxLeft, layout.boxTop, layout.boxRight, layout.boxBottom, backgroundPaint)
        textPaint.textSize = layout.textSizePx
        for (line in layout.lines) {
            canvas.drawText(line.text, line.x, line.baselineY, textPaint)
        }
        return output
    }

    private fun renderCard(
        source: Bitmap,
        content: StampContent,
        config: StampConfig,
        map: Bitmap?,
        locale: Locale
    ): Bitmap {
        val output = copyOf(source)
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.foregroundColor
            typeface = Typeface.DEFAULT
        }
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.foregroundColor
            typeface = Typeface.DEFAULT_BOLD
        }
        val usableMap = if (config.showMap) map else null
        val text = StampCardFormatter.format(content, config, locale)
        val layout = CardLayoutCalculator.compute(
            output.width, output.height, text, usableMap != null, config, PaintTextMeasurer(regular)
        )

        val canvas = Canvas(output)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(layout.left, layout.top, layout.right, layout.bottom),
            layout.cornerRadius, layout.cornerRadius, background
        )

        val mapRect = layout.map
        if (usableMap != null && mapRect != null) {
            val target = RectF(mapRect.left, mapRect.top, mapRect.left + mapRect.size, mapRect.top + mapRect.size)
            val radius = layout.cornerRadius * 0.7f
            canvas.save()
            val clip = Path().apply { addRoundRect(target, radius, radius, Path.Direction.CW) }
            canvas.clipPath(clip)
            canvas.drawBitmap(usableMap, null, target, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        }

        for (line in layout.lines) {
            val paint = if (line.bold) bold else regular
            paint.textSize = line.textSizePx
            canvas.drawText(line.text, line.x, line.baselineY, paint)
        }
        return output
    }

    private fun copyOf(source: Bitmap): Bitmap =
        source.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IllegalStateException("Could not copy the image (probably not enough memory).")
}

private class PaintTextMeasurer(private val paint: Paint) : TextMeasurer {
    override fun width(text: String, textSizePx: Float): Float {
        paint.textSize = textSizePx
        return paint.measureText(text)
    }
}
