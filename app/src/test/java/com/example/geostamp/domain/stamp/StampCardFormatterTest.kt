package com.example.geostamp.domain.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class StampCardFormatterTest {
    private val india = ZoneId.of("Asia/Kolkata")
    // Thursday 22 October 2026, 11:39 in India
    private val instant = Instant.parse("2026-10-22T06:09:00Z")

    private fun content(
        place: String? = null,
        address: String? = "XX12+34Y, Old Town, Sample City, Example State 400001, India",
        source: LocationSource = LocationSource.GPS,
        label: String? = null
    ) = StampContent(address, 12.345678, 45.6789, source, instant, india, label, place)

    private fun format(c: StampContent, cfg: StampConfig = StampConfig()) = StampCardFormatter.format(c, cfg, Locale.US)

    @Test
    fun matches_the_reference_layout() {
        val card = format(content(label = "Captured by GeoStamp"))
        assertEquals("Sample City, Example State, India", card.title)
        assertEquals(
            listOf(
                "XX12+34Y, Old Town, Sample City, Example State 400001, India",
                "Lat 12.345678\u00B0 Long 45.678900\u00B0",
                "Thursday, 22/10/2026 11:39 AM GMT +05:30"
            ),
            card.bodyLines
        )
        assertEquals("Note : Captured by GeoStamp", card.noteLine)
    }

    @Test
    fun place_name_becomes_the_title() {
        assertEquals("Example High School", format(content(place = "Example High School")).title)
    }

    @Test
    fun time_zone_can_be_hidden_and_note_can_be_hidden() {
        val card = format(content(label = "x"), StampConfig(showTimeZone = false, showLabel = false))
        assertEquals("Thursday, 22/10/2026 11:39 AM", card.bodyLines.last())
        assertNull(card.noteLine)
    }

    @Test
    fun no_location_gives_a_plain_title_and_an_honest_line() {
        val card = format(content(source = LocationSource.UNAVAILABLE))
        assertEquals("GeoStamp", card.title)
        assertTrue(card.bodyLines.contains("Location unavailable"))
        assertFalse(card.bodyLines.any { it.startsWith("Lat") })
    }

    @Test
    fun the_card_text_never_mentions_gps_or_manual() {
        val text = (format(content()).bodyLines + format(content()).title).joinToString(" ")
        assertFalse(text.contains("(GPS)"))
        assertFalse(text.contains("(manual)"))
    }

    @Test
    fun address_titles_drop_numbers_and_keep_the_last_three_parts() {
        assertEquals("Sample City, Example State, India", AddressTitles.fromAddress("Riverside, Sample City, Example State 400001, India"))
        assertEquals("Mumbai", AddressTitles.fromAddress("Mumbai"))
        assertNull(AddressTitles.fromAddress("  "))
        assertNull(AddressTitles.fromAddress(null))
    }
}
