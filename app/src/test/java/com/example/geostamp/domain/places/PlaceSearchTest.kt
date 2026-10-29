// geostamp-github-ready
package com.example.geostamp.domain.places

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSearchTest {
    private val places = listOf(
        SavedPlace("p1", "North Primary School", 10.1, 20.1, null, listOf("DEMO", "Northtown")),
        SavedPlace("p2", "Hillside Academy", 10.2, 20.2, null, listOf("DEMO", "Hilltown")),
        SavedPlace("p3", "River High School", 10.3, 20.3, null, listOf("DEMO", "Rivertown", "Eastside")),
        SavedPlace("p4", "Station Road School", 10.4, 20.4, null, listOf("DEMO", "Eastside")),
        SavedPlace("p5", "Garden Vidya School", 10.5, 20.5, null, listOf("DEMO", "Gardenville"))
    )

    @Test
    fun the_group_code_finds_every_place_in_saved_order_in_any_case() {
        val expected = places.map { it.name }
        assertEquals(expected, PlaceSearch.search("demo", places).map { it.name })
        assertEquals(expected, PlaceSearch.search("DEMO", places).map { it.name })
        assertEquals(expected, PlaceSearch.search("  Demo ", places).map { it.name })
    }

    @Test
    fun names_and_tags_are_found_by_part_of_a_word() {
        assertEquals(listOf("Hillside Academy"), PlaceSearch.search("hillside", places).map { it.name })
        assertTrue("Garden Vidya School" in PlaceSearch.search("gar", places).map { it.name })
        assertEquals(listOf("North Primary School"), PlaceSearch.search("northt", places).map { it.name })
    }

    @Test
    fun place_tags_find_places() {
        assertEquals(2, PlaceSearch.search("eastside", places).size)
        assertEquals(listOf("Garden Vidya School"), PlaceSearch.search("gardenville", places).map { it.name })
    }

    @Test
    fun every_typed_word_must_match() {
        assertEquals(listOf("Garden Vidya School"), PlaceSearch.search("demo gardenville", places).map { it.name })
        assertTrue(PlaceSearch.search("demo nowhere", places).isEmpty())
    }

    @Test
    fun empty_query_and_no_match_give_nothing() {
        assertTrue(PlaceSearch.search("", places).isEmpty())
        assertTrue(PlaceSearch.search("   ", places).isEmpty())
        assertTrue(PlaceSearch.search("zzz", places).isEmpty())
    }

    @Test
    fun results_can_be_limited() {
        assertEquals(2, PlaceSearch.search("demo", places, limit = 2).size)
    }

    @Test
    fun an_address_that_was_learned_can_be_searched() {
        val withAddress = places.map { if (it.id == "p2") it.copy(address = "Hill Road West") else it }
        assertEquals(listOf("Hillside Academy"), PlaceSearch.search("west", withAddress).map { it.name })
    }

    @Test
    fun tags_are_cleaned_when_typed() {
        assertEquals(listOf("Group", "Town", "Old Quarter"), PlaceSearch.parseTags("Group, Town; group, Old Quarter!!"))
        assertTrue(PlaceSearch.parseTags("  ,, ").isEmpty())
    }

    @Test
    fun places_round_trip_through_json() {
        val place = SavedPlace("id1", "Test School", 1.5, -2.5, "Some Road", listOf("A", "X"))
        assertEquals(place, PlaceJson.fromJson(JSONObject(PlaceJson.toJson(place).toString())))
        val bare = SavedPlace("id2", "Bare", 3.0, 4.0)
        val copy = PlaceJson.fromJson(JSONObject(PlaceJson.toJson(bare).toString()))
        assertEquals(bare, copy)
        assertNull(copy.address)
    }
}
