package com.example.geostamp.domain.album

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumNamesTest {

    @Test
    fun album_names_are_cleaned() {
        assertEquals("Trip to Goa", AlbumNames.sanitizeAlbum("  Trip   to Goa  "))
        assertEquals("Site-1_final", AlbumNames.sanitizeAlbum("Site-1_final"))
    }

    @Test
    fun path_characters_are_removed_so_an_album_cannot_escape_its_folder() {
        assertEquals("etcpasswd", AlbumNames.sanitizeAlbum("../etc/passwd"))
        assertEquals("a b", AlbumNames.sanitizeAlbum("a\\b".replace('\\', ' ')))
    }

    @Test
    fun empty_or_symbol_only_album_names_are_rejected() {
        assertNull(AlbumNames.sanitizeAlbum("   "))
        assertNull(AlbumNames.sanitizeAlbum("***///"))
    }

    @Test
    fun long_album_names_are_cut() {
        val result = AlbumNames.sanitizeAlbum("x".repeat(100))!!
        assertEquals(AlbumNames.MAX_LENGTH, result.length)
    }

    @Test
    fun same_ignores_case_and_null() {
        assertTrue(AlbumNames.same("Trips", "trips"))
        assertFalse(AlbumNames.same("Trips", "Work"))
        assertFalse(AlbumNames.same(null, "Trips"))
        assertFalse(AlbumNames.same(null, null))
    }

    @Test
    fun distinct_sorted_removes_duplicates_ignoring_case() {
        val result = AlbumNames.distinctSorted(listOf("work", null, "Trips", "Work", "alpha"))
        assertEquals(listOf("alpha", "Trips", "work"), result)
    }

    @Test
    fun file_names_get_a_jpg_ending() {
        assertEquals("My photo.jpg", AlbumNames.sanitizeFileName("My photo"))
        assertEquals("My photo.jpg", AlbumNames.sanitizeFileName("My photo.jpg"))
        assertEquals("Site (2).jpg", AlbumNames.sanitizeFileName("Site (2).JPG"))
    }

    @Test
    fun file_names_cannot_contain_slashes_or_be_empty() {
        assertEquals("abc.jpg", AlbumNames.sanitizeFileName("a/b\\c"))
        assertNull(AlbumNames.sanitizeFileName("   "))
        assertNull(AlbumNames.sanitizeFileName("///"))
        assertNull(AlbumNames.sanitizeFileName(".jpg"))
    }
}
