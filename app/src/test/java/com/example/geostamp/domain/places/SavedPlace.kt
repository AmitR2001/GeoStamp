package com.example.geostamp.domain.places

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

/** The five school visits that come with the app. They share the tag AMR. */
object SeedPlaces {
    const val GROUP_TAG = "AMR"

    fun defaults(): List<SavedPlace> = listOf(
        SavedPlace("seed-ravindra", "Ravindra Vidyalaya", 19.291835, 73.204060, null, listOf(GROUP_TAG, "Titwala")),
        SavedPlace("seed-sharda", "Sharda Mandir", 19.246392, 73.122822, null, listOf(GROUP_TAG, "Kalyan")),
        SavedPlace("seed-saiprasad", "Example High School", 12.111111, 45.222222, null, listOf(GROUP_TAG, "Riverside")),
        SavedPlace("seed-yogeshwar", "Yogeshwar Vidyamandir", 19.209894, 72.948534, null, listOf(GROUP_TAG, "Lokmanya Nagar", "Sample City")),
        SavedPlace("seed-sanket", "Sample Primary School", 19.237210, 72.979284, null, listOf(GROUP_TAG, "Manpada", "Sample City"))
    )
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

    /** A clean tag list from text typed as "AMR, Riverside". */
    fun parseTags(text: String): List<String> =
        text.split(',', ';')
            .map { it.filter { c -> c.isLetterOrDigit() || c == ' ' || c == '-' || c == '_' }.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(8)
}
