package com.example.geostamp.domain.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ExifDatesTest {

    private val india = ZoneId.of("Asia/Kolkata")

    @Test
    fun date_with_offset_uses_that_offset() {
        val result = ExifDates.parse("2026:10:13 14:00:00", "+05:30", ZoneId.of("UTC"))!!
        assertEquals(Instant.parse("2026-10-13T08:30:00Z"), result.instant)
        assertEquals(ZoneOffset.of("+05:30"), result.zone)
    }

    @Test
    fun date_without_offset_uses_the_fallback_zone() {
        val result = ExifDates.parse("2026:10:13 14:00:00", null, india)!!
        assertEquals(Instant.parse("2026-10-13T08:30:00Z"), result.instant)
        assertEquals(india, result.zone)
    }

    @Test
    fun bad_offset_falls_back() {
        val result = ExifDates.parse("2026:10:13 14:00:00", "nonsense", india)!!
        assertEquals(india, result.zone)
    }

    @Test
    fun blank_and_garbage_dates_give_null() {
        assertNull(ExifDates.parse(null, null, india))
        assertNull(ExifDates.parse("   ", null, india))
        assertNull(ExifDates.parse("yesterday", null, india))
        assertNull(ExifDates.parse("2026-10-13 14:00:00", null, india))
    }

    @Test
    fun cameras_that_write_zero_dates_give_null() {
        assertNull(ExifDates.parse("0000:00:00 00:00:00", null, india))
    }

    @Test
    fun surrounding_spaces_are_ignored() {
        assertNotNull(ExifDates.parse(" 2026:10:13 14:00:00 ", " +05:30 ", india))
    }
}
