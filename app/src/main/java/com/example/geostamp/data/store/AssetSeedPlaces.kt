// geostamp-github-ready
package com.example.geostamp.data.store

import android.content.Context
import com.example.geostamp.domain.places.SavedPlace
import com.example.geostamp.domain.places.SeedPlaces
import java.io.IOException

/**
 * Reads the optional file app/src/main/assets/seed_places.json (kept out of version control).
 * If the file is not there, the app simply starts with no saved places.
 */
object AssetSeedPlaces {
    fun load(context: Context): List<SavedPlace> =
        try {
            context.assets.open("seed_places.json").use { SeedPlaces.parse(it.readBytes().toString(Charsets.UTF_8)) }
        } catch (e: IOException) {
            emptyList()
        }
}
