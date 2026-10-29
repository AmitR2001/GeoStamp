// geostamp-github-ready
package com.example.geostamp.domain.places

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedPlacesTest {
    @Test
    fun a_valid_list_is_read_with_ids_and_cleaned_tags() {
        val json = """{"places":[
            {"name":"First School","latitude":10.5,"longitude":20.25,"tags":["Group"," Town "]},
            {"id":"mine","name":"Second School","latitude":-5.0,"longitude":-6.0,"address":"Some Road"}
        ]}"""
        val result = SeedPlaces.parse(json)
        assertEquals(2, result.size)
        assertEquals("seed-1", result[0].id)
        assertEquals(listOf("Group", "Town"), result[0].tags)
        assertEquals("mine", result[1].id)
        assertEquals("Some Road", result[1].address)
        assertTrue(result[1].tags.isEmpty())
    }

    @Test
    fun bad_entries_are_skipped_but_good_ones_survive() {
        val json = """{"places":[
            {"name":"","latitude":1,"longitude":2},
            {"name":"No longitude","latitude":1},
            {"name":"Out of range","latitude":123,"longitude":2},
            {"name":"Fine","latitude":1.5,"longitude":2.5}
        ]}"""
        assertEquals(listOf("Fine"), SeedPlaces.parse(json).map { it.name })
    }

    @Test
    fun missing_or_broken_text_gives_an_empty_list() {
        assertTrue(SeedPlaces.parse("").isEmpty())
        assertTrue(SeedPlaces.parse("not json").isEmpty())
        assertTrue(SeedPlaces.parse("{}").isEmpty())
        assertNull(SeedPlaces.parse("""{"places":[{"name":"A","latitude":1,"longitude":2}]}""").first().address)
    }
}
