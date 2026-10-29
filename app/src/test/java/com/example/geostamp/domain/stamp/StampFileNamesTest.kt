package com.example.geostamp.domain.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StampFileNamesTest {

    @Test
    fun name_uses_local_time_and_short_id() {
        val name = StampFileNames.forPhoto(
            Instant.parse("2026-10-13T08:30:00Z"), ZoneId.of("Asia/Kolkata"), "abcd1234-ffff-0000"
        )
        assertEquals("GeoStamp_20261013_140000_abcd1234.jpg", name)
    }

    @Test
    fun odd_ids_are_cleaned_and_empty_ids_get_a_default() {
        val instant = Instant.parse("2026-01-02T03:04:05Z")
        assertEquals("GeoStamp_20260102_030405_ab12.jpg", StampFileNames.forPhoto(instant, ZoneId.of("UTC"), "a/b 1*2"))
        assertEquals("GeoStamp_20260102_030405_photo.jpg", StampFileNames.forPhoto(instant, ZoneId.of("UTC"), "---"))
    }

    @Test
    fun new_ids_are_unique() {
        assertNotEquals(StampFileNames.newId(), StampFileNames.newId())
    }
}
