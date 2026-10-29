package com.example.geostamp.data.media

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import com.example.geostamp.domain.model.PhotoRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Small pictures for the gallery grid, kept in memory. Returns null when a photo cannot be read. */
class ThumbnailCache(private val loader: BitmapLoader, private val originalsDir: File) {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun get(record: PhotoRecord, edge: Int = 360): Bitmap? {
        val key = record.id + "|" + record.savedUri + "|" + record.savedPath
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val uri = record.savedUri
                val original = record.originalFileName
                when {
                    uri != null -> loader.loadUri(Uri.parse(uri), edge).bitmap
                    original != null -> loader.loadFile(File(originalsDir, original), edge).bitmap
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }

    fun invalidate(id: String) {
        for (key in cache.snapshot().keys) {
            if (key.startsWith("$id|")) cache.remove(key)
        }
    }
}
