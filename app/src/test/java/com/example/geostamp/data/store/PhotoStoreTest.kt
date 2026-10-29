package com.example.geostamp.data.store

import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.model.PhotoSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PhotoStoreTest {

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "geostamp-test-${System.nanoTime()}").apply { mkdirs() }

    private fun record(id: String, name: String = "$id.jpg") = PhotoRecord(
        id = id, createdAtMillis = 1L, source = PhotoSource.CAMERA,
        originalFileName = null, savedUri = null, savedPath = null, displayName = name
    )

    @Test
    fun empty_store_has_no_photos() = runBlocking {
        val store = PhotoStore(File(tempDir(), "photos.json"))
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun upsert_adds_then_updates() = runBlocking {
        val store = PhotoStore(File(tempDir(), "photos.json"))
        store.upsert(record("a"))
        store.upsert(record("b"))
        store.upsert(record("a", name = "renamed.jpg"))
        val all = store.all()
        assertEquals(2, all.size)
        assertEquals("renamed.jpg", store.get("a")!!.displayName)
    }

    @Test
    fun data_survives_a_new_store_instance() = runBlocking {
        val file = File(tempDir(), "photos.json")
        PhotoStore(file).upsert(record("a"))
        assertEquals(listOf("a"), PhotoStore(file).all().map { it.id })
    }

    @Test
    fun delete_removes_only_that_record() = runBlocking {
        val store = PhotoStore(File(tempDir(), "photos.json"))
        store.upsert(record("a"))
        store.upsert(record("b"))
        store.delete("a")
        assertEquals(listOf("b"), store.all().map { it.id })
        assertNull(store.get("a"))
    }

    @Test
    fun unreadable_file_is_kept_aside_and_a_fresh_list_starts() = runBlocking {
        val dir = tempDir()
        val file = File(dir, "photos.json")
        file.writeText("this is not json")
        val store = PhotoStore(file)
        assertTrue(store.all().isEmpty())
        assertFalse(file.exists())
        assertTrue(dir.listFiles()!!.any { it.name.startsWith("photos.json.unreadable-") })
        store.upsert(record("a"))
        assertEquals(1, store.all().size)
    }
}
