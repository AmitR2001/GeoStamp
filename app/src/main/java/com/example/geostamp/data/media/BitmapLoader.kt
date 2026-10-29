// geostamp-iteration: 5
package com.example.geostamp.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.geostamp.domain.imaging.ImageMath
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class LoadedBitmap(val bitmap: Bitmap, val sourceWidth: Int, val sourceHeight: Int)

/**
 * Loads photos safely: downsamples big images, applies the EXIF orientation so the picture
 * is upright, optionally rotates by a further multiple of 90 degrees, and turns out-of-memory
 * into a normal readable error. Call from a background thread.
 */
class BitmapLoader(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun loadFile(file: File, maxLongEdge: Int, extraRotation: Int = 0): LoadedBitmap =
        load(maxLongEdge, extraRotation) { FileInputStream(file) }

    fun loadUri(uri: Uri, maxLongEdge: Int, extraRotation: Int = 0): LoadedBitmap =
        load(maxLongEdge, extraRotation) { resolver.openInputStream(uri) }

    private fun load(maxLongEdge: Int, extraRotation: Int, openStream: () -> InputStream?): LoadedBitmap {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            (openStream() ?: throw IOException("Could not open the image.")).use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("This file is not a readable image.")
            }

            val orientation = openStream()?.use {
                runCatching {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val options = BitmapFactory.Options().apply {
                inSampleSize = ImageMath.sampleSize(bounds.outWidth, bounds.outHeight, maxLongEdge)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = (openStream() ?: throw IOException("Could not open the image.")).use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: throw IOException("The image could not be decoded.")

            val upright = applyOrientation(decoded, orientation)
            val turned = rotateBy(upright, extraRotation)
            val (w, h) = ImageMath.fitWithin(turned.width, turned.height, maxLongEdge)
            val fitted = if (w == turned.width && h == turned.height) turned
            else Bitmap.createScaledBitmap(turned, w, h, true).also { if (it !== turned) turned.recycle() }
            return LoadedBitmap(fitted, bounds.outWidth, bounds.outHeight)
        } catch (e: OutOfMemoryError) {
            throw IOException("This photo is too large to process on this device.")
        }
    }

    private fun rotateBy(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val matrix = Matrix().apply { setRotate(normalized.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
