package com.example.geostamp.domain.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ExifFormatTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private val instant = Instant.parse("2026-10-13T08:30:00Z")

    @Test fun date_time_is_written_in_camera_format() = assertEquals("2026:10:13 14:00:00", ExifFormat.dateTime(instant, india))
    @Test fun positive_offset() = assertEquals("+05:30", ExifFormat.offset(instant, india))
    @Test fun negative_offset() = assertEquals("-04:00", ExifFormat.offset(instant, ZoneId.of("America/New_York")))
    @Test fun utc_offset() = assertEquals("+00:00", ExifFormat.offset(instant, ZoneId.of("UTC")))

    @Test
    fun written_dates_can_be_read_back_unchanged() {
        val text = ExifFormat.dateTime(instant, india)
        val offset = ExifFormat.offset(instant, india)
        val read = ExifDates.parse(text, offset, ZoneId.of("UTC"))!!
        assertEquals(instant, read.instant)
    }

    @Test
    fun changes_know_what_they_change() {
        assertTrue(ExifChanges().isEmpty)
        assertTrue(ExifChanges(removeLocation = true).changesLocation)
        assertTrue(ExifChanges(latitude = 1.0, longitude = 2.0).changesLocation)
        assertFalse(ExifChanges(latitude = 1.0).changesLocation)
        assertTrue(ExifChanges(capturedAt = instant, zone = india).changesDate)
        assertFalse(ExifChanges(capturedAt = instant).changesDate)
    }
}
