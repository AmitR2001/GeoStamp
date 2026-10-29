package com.example.geostamp.data.store

import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.model.PhotoRecordJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Keeps the list of photos in one JSON file inside the app's private storage.
 * Deliberately simple: no Room/KSP setup needed. It sits behind this small class,
 * so it can be swapped for Room later without touching the screens.
 *
 * If the file is ever unreadable it is renamed (never deleted) and a fresh list is started.
 */
class PhotoStore(private val file: File) {
    private val mutex = Mutex()

    suspend fun all(): List<PhotoRecord> = withContext(Dispatchers.IO) {
        mutex.withLock { readAll() }
    }

    suspend fun get(id: String): PhotoRecord? = all().firstOrNull { it.id == id }

    suspend fun upsert(record: PhotoRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readAll().toMutableList()
            val index = current.indexOfFirst { it.id == record.id }
            if (index >= 0) current[index] = record else current.add(record)
            writeAll(current)
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock { writeAll(readAll().filterNot { it.id == id }) }
    }

    private fun readAll(): List<PhotoRecord> {
        if (!file.exists()) return emptyList()
        return try {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("photos") ?: JSONArray()
            (0 until array.length()).map { PhotoRecordJson.fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            val backup = File(file.parentFile, "${file.name}.unreadable-${System.currentTimeMillis()}")
            file.renameTo(backup)
            emptyList()
        }
    }

    private fun writeAll(records: List<PhotoRecord>) {
        val root = JSONObject().apply {
            put("version", 1)
            put("photos", JSONArray().also { array -> records.forEach { array.put(PhotoRecordJson.toJson(it)) } })
        }
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(root.toString())
        if (!temp.renameTo(file)) {
            file.writeText(root.toString())
            temp.delete()
        }
    }
}
