// geostamp-github-ready
package com.example.geostamp.data.store

import com.example.geostamp.domain.places.PlaceJson
import com.example.geostamp.domain.places.SavedPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Keeps saved places in one JSON file in the app's private storage. The first time it runs the
 * file is created from the optional [seed] list (empty by default); after that the file is yours
 * (deleting a place never brings it back). An unreadable file is renamed, never deleted.
 */
class PlaceStore(
    private val file: File,
    private val seed: () -> List<SavedPlace> = { emptyList() }
) {
    private val mutex = Mutex()

    suspend fun all(): List<SavedPlace> = withContext(Dispatchers.IO) { mutex.withLock { readAll() } }

    suspend fun upsert(place: SavedPlace) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readAll().toMutableList()
            val index = current.indexOfFirst { it.id == place.id }
            if (index >= 0) current[index] = place else current.add(place)
            writeAll(current)
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock { writeAll(readAll().filterNot { it.id == id }) }
    }

    private fun readAll(): List<SavedPlace> {
        if (!file.exists()) {
            val seeds = seed()
            writeAll(seeds)
            return seeds
        }
        return try {
            val array = JSONObject(file.readText()).optJSONArray("places") ?: JSONArray()
            (0 until array.length()).map { PlaceJson.fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            file.renameTo(File(file.parentFile, "${file.name}.unreadable-${System.currentTimeMillis()}"))
            emptyList()
        }
    }

    private fun writeAll(places: List<SavedPlace>) {
        val root = JSONObject().apply {
            put("version", 1)
            put("places", JSONArray().also { array -> places.forEach { array.put(PlaceJson.toJson(it)) } })
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
