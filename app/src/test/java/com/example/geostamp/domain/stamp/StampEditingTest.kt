package com.example.geostamp.domain.stamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StampEditingTest {

    private val india = ZoneId.of("Asia/Kolkata")
    private val gps = StampContent(
        address = "MG Road, Bengaluru",
        latitude = 12.9716,
        longitude = 77.5946,
        locationSource = LocationSource.GPS,
        capturedAt = Instant.parse("2026-10-13T08:30:00Z"),
        zoneId = india
    )

    @Test
    fun fields_show_the_wall_clock_time_in_the_content_zone() {
        val fields = StampEditing.fieldsFrom(gps)
        assertEquals("2026-10-13", fields.date)
        assertEquals("14:00:00", fields.time)
        assertEquals("Asia/Kolkata", fields.zone)
        assertEquals("12.971600", fields.latitude)
        assertEquals("77.594600", fields.longitude)
    }

    @Test
    fun untouched_fields_keep_gps_source_and_same_instant() {
        val result = StampEditing.apply(StampEditing.fieldsFrom(gps), gps)
        assertFalse(result.errors.hasErrors)
        assertEquals(LocationSource.GPS, result.content.locationSource)
        assertEquals(gps.capturedAt, result.content.capturedAt)
        assertEquals(gps.address, result.content.address)
    }

    @Test
    fun changing_the_address_makes_the_location_manual() {
        val fields = StampEditing.fieldsFrom(gps).copy(address = "My office")
        val result = StampEditing.apply(fields, gps)
        assertEquals(LocationSource.MANUAL, result.content.locationSource)
        assertEquals("My office", result.content.address)
    }

    @Test
    fun changing_a_coordinate_makes_the_location_manual() {
        val fields = StampEditing.fieldsFrom(gps).copy(latitude = "13.0")
        val result = StampEditing.apply(fields, gps)
        assertEquals(LocationSource.MANUAL, result.content.locationSource)
        assertEquals(13.0, result.content.latitude!!, 0.0000001)
    }

    @Test
    fun clearing_everything_gives_unavailable() {
        val fields = StampEditing.fieldsFrom(gps).copy(address = "", latitude = "", longitude = "")
        val result = StampEditing.apply(fields, gps)
        assertEquals(LocationSource.UNAVAILABLE, result.content.locationSource)
        assertNull(result.content.latitude)
        assertNull(result.content.address)
    }

    @Test
    fun editing_an_unavailable_stamp_by_typing_an_address_is_manual() {
        val empty = StampContent(capturedAt = gps.capturedAt, zoneId = india)
        val result = StampEditing.apply(StampEditing.fieldsFrom(empty).copy(address = "Home"), empty)
        assertEquals(LocationSource.MANUAL, result.content.locationSource)
    }

    @Test
    fun untouched_unavailable_stamp_stays_unavailable() {
        val empty = StampContent(capturedAt = gps.capturedAt, zoneId = india)
        val result = StampEditing.apply(StampEditing.fieldsFrom(empty), empty)
        assertEquals(LocationSource.UNAVAILABLE, result.content.locationSource)
    }

    @Test
    fun out_of_range_coordinates_are_errors_and_not_used() {
        val fields = StampEditing.fieldsFrom(gps).copy(latitude = "123", longitude = "200")
        val result = StampEditing.apply(fields, gps)
        assertNotNull(result.errors.latitude)
        assertNotNull(result.errors.longitude)
        assertNull(result.content.latitude)
        assertNull(result.content.longitude)
        assertTrue(result.errors.hasErrors)
    }

    @Test
    fun only_one_coordinate_is_an_error() {
        val fields = StampEditing.fieldsFrom(gps).copy(longitude = "")
        val result = StampEditing.apply(fields, gps)
        assertNotNull(result.errors.longitude)
        assertNull(result.content.latitude)
    }

    @Test
    fun decimal_comma_is_accepted() {
        assertEquals(12.5, StampEditing.parseLatitude("12,5")!!, 0.0000001)
    }

    @Test
    fun garbage_coordinates_are_rejected() {
        assertNull(StampEditing.parseLatitude("abc"))
        assertNull(StampEditing.parseLatitude("NaN"))
        assertNull(StampEditing.parseLongitude("181"))
    }

    @Test
    fun date_and_time_edits_change_the_instant() {
        val fields = StampEditing.fieldsFrom(gps).copy(date = "2026-12-25", time = "09:15:30")
        val result = StampEditing.apply(fields, gps)
        // 09:15:30 in India (UTC+05:30) is 03:45:30 UTC
        assertEquals(Instant.parse("2026-12-25T03:45:30Z"), result.content.capturedAt)
    }

    @Test
    fun time_without_seconds_is_accepted() {
        val fields = StampEditing.fieldsFrom(gps).copy(time = "07:05")
        val result = StampEditing.apply(fields, gps)
        assertFalse(result.errors.hasErrors)
        assertEquals(Instant.parse("2026-10-13T01:35:00Z"), result.content.capturedAt)
    }

    @Test
    fun bad_date_and_time_report_errors_and_keep_the_original_instant() {
        val fields = StampEditing.fieldsFrom(gps).copy(date = "13/10/2026", time = "25:00:00")
        val result = StampEditing.apply(fields, gps)
        assertNotNull(result.errors.date)
        assertNotNull(result.errors.time)
        assertEquals(gps.capturedAt, result.content.capturedAt)
    }

    @Test
    fun changing_the_zone_reinterprets_the_typed_wall_clock_time() {
        val fields = StampEditing.fieldsFrom(gps).copy(zone = "UTC")
        val result = StampEditing.apply(fields, gps)
        // typed 14:00:00 is now read as UTC instead of India time
        assertEquals(Instant.parse("2026-10-13T14:00:00Z"), result.content.capturedAt)
        assertEquals(ZoneId.of("UTC"), result.content.zoneId)
    }

    @Test
    fun invalid_zone_is_an_error_and_falls_back_to_the_original_zone() {
        val fields = StampEditing.fieldsFrom(gps).copy(zone = "Mars/Olympus")
        val result = StampEditing.apply(fields, gps)
        assertNotNull(result.errors.zone)
        assertEquals(india, result.content.zoneId)
    }

    @Test
    fun blank_label_becomes_null_and_text_is_trimmed() {
        val fields = StampEditing.fieldsFrom(gps).copy(label = "   ", address = "  Home  ")
        val result = StampEditing.apply(fields, gps)
        assertNull(result.content.label)
        assertEquals("Home", result.content.address)
    }
}
