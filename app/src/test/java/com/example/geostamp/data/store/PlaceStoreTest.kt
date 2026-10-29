// geostamp-github-ready
package com.example.geostamp.data.store

import com.example.geostamp.domain.places.SavedPlace
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlaceStoreTest {
    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "geostamp-places-${System.nanoTime()}").apply { mkdirs() }

    private val samples = listOf(
        SavedPlace("s1", "Sample One", 1.0, 2.0, null, listOf("DEMO")),
        SavedPlace("s2", "Sample Two", 3.0, 4.0, null, listOf("DEMO"))
    )

    @Test
    fun a_store_without_a_seed_list_starts_empty() = runBlocking {
        assertTrue(PlaceStore(File(tempDir(), "places.json")).all().isEmpty())
    }

    @Test
    fun the_first_run_starts_with_the_seed_list() = runBlocking {
        val store = PlaceStore(File(tempDir(), "places.json")) { samples }
        assertEquals(listOf("Sample One", "Sample Two"), store.all().map { it.name })
    }

    @Test
    fun places_can_be_added_changed_and_deleted_and_stay_deleted() = runBlocking {
        val file = File(tempDir(), "places.json")
        val store = PlaceStore(file) { samples }
        store.upsert(SavedPlace("mine", "My School", 1.0, 2.0))
        store.upsert(SavedPlace("mine", "My School 2", 1.0, 2.0, "Road 5"))
        assertEquals("My School 2", store.all().first { it.id == "mine" }.name)
        store.delete("s1")
        store.delete("mine")
        // A second store on the same file must not bring the seed list back.
        val reopened = PlaceStore(file) { samples }.all()
        assertEquals(listOf("s2"), reopened.map { it.id })
        assertFalse(reopened.any { it.id == "s1" })
    }

    @Test
    fun an_unreadable_file_is_kept_aside() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "places.json")
        file.writeText("garbage")
        assertTrue(PlaceStore(file) { samples }.all().isEmpty())
        assertTrue(dir.listFiles()!!.any { it.name.startsWith("places.json.unreadable-") })
    }
}
