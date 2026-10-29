// geostamp-iteration: 7
package com.example.geostamp.domain.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class StampFormatterTest {

    // 2026-10-13 08:30:00 UTC = 14:00:00 in India (UTC+05:30)
    private val instant = Instant.parse("2026-10-13T08:30:00Z")
    private val india = ZoneId.of("Asia/Kolkata")

    private fun content(
        source: LocationSource = LocationSource.GPS,
        address: String? = "MG Road, Bengaluru",
        lat: Double? = 12.9716,
        lon: Double? = 77.5946,
        label: String? = null,
        zone: ZoneId = india,
        place: String? = null
    ) = StampContent(address, lat, lon, source, instant, zone, label, place)

    private fun format(c: StampContent, cfg: StampConfig = StampConfig()) =
        StampFormatter.format(c, cfg, Locale.US)

    @Test
    fun stamp_has_address_coordinates_and_datetime_without_any_source_tag() {
        assertEquals(
            listOf("MG Road, Bengaluru", "12.97160° N, 77.59460° E", "13 Oct 2026 14:00:00 UTC+05:30"),
            format(content())
        )
    }

    @Test
    fun typed_locations_look_exactly_like_gps_ones() {
        assertEquals(format(content(source = LocationSource.GPS)), format(content(source = LocationSource.MANUAL)))
    }

    @Test
    fun the_stamp_text_never_mentions_gps_or_manual() {
        for (source in LocationSource.values()) {
            val text = format(content(source = source)).joinToString(" ")
            assertFalse(text.contains("(GPS)"))
            assertFalse(text.contains("(manual)"))
        }
    }

    @Test
    fun place_name_is_written_before_the_address() {
        val lines = format(content(place = "Example High School", address = "Riverside, Sample City, Example State 400001, India"))
        assertEquals("Example High School, Riverside, Sample City, Example State 400001, India", lines[0])
    }

    @Test
    fun place_name_alone_is_shown_when_there_is_no_address() {
        val lines = format(content(place = "Sample Primary School", address = null))
        assertEquals("Sample Primary School", lines[0])
    }

    @Test
    fun blank_place_name_is_ignored() {
        assertEquals("MG Road, Bengaluru", format(content(place = "   "))[0])
    }

    @Test
    fun southern_and_western_hemispheres() {
        assertEquals("33.86880° S, 70.66930° W", format(content(lat = -33.8688, lon = -70.6693))[1])
    }

    @Test
    fun unavailable_location_never_invents_coordinates_or_address() {
        val lines = format(content(source = LocationSource.UNAVAILABLE, address = "Somewhere", lat = 1.0, lon = 2.0))
        assertEquals(2, lines.size)
        assertEquals("Location unavailable", lines[0])
    }

    @Test
    fun out_of_range_or_missing_coordinates_show_unavailable() {
        assertTrue(format(content(lat = 123.0, lon = 10.0)).contains("Location unavailable"))
        assertTrue(format(content(lat = null, lon = null)).contains("Location unavailable"))
    }

    @Test
    fun label_appears_first_and_can_be_hidden() {
        assertEquals("Site visit", format(content(label = "Site visit")).first())
        assertFalse(format(content(label = "Site visit"), StampConfig(showLabel = false)).contains("Site visit"))
    }

    @Test
    fun blank_label_and_address_are_skipped() {
        assertEquals(2, format(content(label = "   ", address = "  ")).size)
    }

    @Test
    fun twelve_hour_time_and_iso_date() {
        val cfg = StampConfig(dateStyle = DateStyle.ISO, timeStyle = TimeStyle.H12, showTimeZone = false)
        assertEquals("2026-10-13 02:00:00 PM", format(content(), cfg).last())
    }

    @Test
    fun timezones_change_the_shown_time_not_the_instant() {
        assertEquals("13 Oct 2026 08:30:00 UTC+00:00", format(content(zone = ZoneId.of("UTC"))).last())
        assertEquals("13 Oct 2026 04:30:00 UTC-04:00", format(content(zone = ZoneId.of("America/New_York"))).last())
    }
}
