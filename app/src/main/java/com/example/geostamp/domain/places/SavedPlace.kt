// geostamp-github-ready
package com.example.geostamp.domain.places

import com.example.geostamp.domain.stamp.StampFormatter
import org.json.JSONArray
import org.json.JSONObject

/** A place you use often, such as a school. Tags let a short word (like a project code) find a whole group. */
data class SavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val tags: List<String> = emptyList()
)

object PlaceJson {
    fun toJson(place: SavedPlace): JSONObject = JSONObject().apply {
        put("id", place.id)
        put("name", place.name)
        put("latitude", place.latitude)
        put("longitude", place.longitude)
        place.address?.let { put("address", it) }
        put("tags", JSONArray().also { array -> place.tags.forEach { array.put(it) } })
    }

    fun fromJson(json: JSONObject): SavedPlace {
        val tags = json.optJSONArray("tags")
        return SavedPlace(
            id = json.getString("id"),
            name = json.getString("name"),
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
            address = if (json.isNull("address")) null else json.optString("address"),
            tags = if (tags == null) emptyList() else (0 until tags.length()).map { tags.getString(it) }
        )
    }
}

/**
 * An optional list of places that comes with a build. It is read from a JSON text such as
 * {"places":[{"name":"Example School","latitude":12.3,"longitude":45.6,"tags":["Group"]}]}
 * (see docs/seed_places.example.json). The app itself contains no places.
 */
object SeedPlaces {
    fun parse(text: String): List<SavedPlace> {
        return try {
            val array = JSONObject(text).optJSONArray("places") ?: return emptyList()
            (0 until array.length()).mapNotNull { index ->
                val o = array.optJSONObject(index) ?: return@mapNotNull null
                val name = o.optString("name").trim()
                val lat = o.optDouble("latitude", Double.NaN)
                val lon = o.optDouble("longitude", Double.NaN)
                if (name.isEmpty() || !StampFormatter.isValidLatitude(lat) || !StampFormatter.isValidLongitude(lon)) {
                    return@mapNotNull null
                }
                val tags = o.optJSONArray("tags")
                SavedPlace(
                    id = o.optString("id").ifBlank { "seed-" + (index + 1) },
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    address = if (o.isNull("address")) null else o.optString("address").trim().ifEmpty { null },
                    tags = if (tags == null) emptyList() else PlaceSearch.parseTags((0 until tags.length()).joinToString(",") { tags.optString(it) })
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/** Finds saved places while the person types. Case does not matter; every typed word must match. */
object PlaceSearch {
    fun search(query: String, places: List<SavedPlace>, limit: Int = 6): List<SavedPlace> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val words = q.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
        return places
            .filter { place ->
                val haystack = (place.name + " " + place.tags.joinToString(" ") + " " + (place.address ?: "")).lowercase()
                words.all { it in haystack }
            }
            .sortedBy { score(q, it) }   // stable: places with the same score keep their saved order
            .take(limit)
    }

    private fun score(q: String, place: SavedPlace): Int {
        val name = place.name.lowercase()
        val tags = place.tags.map { it.lowercase() }
        return when {
            tags.any { it == q } -> 0
            name.startsWith(q) -> 1
            tags.any { it.startsWith(q) } -> 1
            name.contains(q) -> 2
            else -> 3
        }
    }

    /** A clean tag list from text typed as "Group, Town". */
    fun parseTags(text: String): List<String> =
        text.split(',', ';')
            .map { it.filter { c -> c.isLetterOrDigit() || c == ' ' || c == '-' || c == '_' }.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(8)
}
