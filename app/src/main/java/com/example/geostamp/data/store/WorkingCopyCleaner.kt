package com.example.geostamp.data.store

import com.example.geostamp.domain.cleanup.WorkingCopyRules
import com.example.geostamp.domain.cleanup.WorkingFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CleanupScan(val fileCount: Int, val totalBytes: Long)

/**
 * Finds and removes leftover private working copies: pictures that were taken or imported but
 * never saved. Files used by a saved photo, and very recent files, are never touched.
 */
class WorkingCopyCleaner(private val store: PhotoStore, private val originalsDir: File) {

    suspend fun scan(minAgeMillis: Long): CleanupScan = withContext(Dispatchers.IO) {
        val files = cleanable(minAgeMillis)
        CleanupScan(files.size, files.sumOf { it.sizeBytes })
    }

    suspend fun clean(minAgeMillis: Long): Int = withContext(Dispatchers.IO) {
        var removed = 0
        for (file in cleanable(minAgeMillis)) {
            if (File(originalsDir, file.name).delete()) removed++
        }
        removed
    }

    private suspend fun cleanable(minAgeMillis: Long): List<WorkingFile> {
        val referenced = store.all().mapNotNull { it.originalFileName }.toSet()
        val files = originalsDir.listFiles()?.filter { it.isFile }?.map {
            WorkingFile(it.name, it.lastModified(), it.length())
        } ?: emptyList()
        return WorkingCopyRules.selectCleanable(files, referenced, System.currentTimeMillis(), minAgeMillis)
    }
}
