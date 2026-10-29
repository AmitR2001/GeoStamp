package com.example.geostamp.domain.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExifEditingTest {

    private fun validate(
        changeLocation: Boolean = false, lat: String = "", lon: String = "",
        changeDate: Boolean = false, date: String = "", time: String = "", zone: String = ""
    ) = ExifEditing.validate(changeLocation, lat, lon, changeDate, date, time, zone)

    @Test
    fun nothing_switched_on_means_nothing_to_write() {
        val result = validate(lat = "1", lon = "2", date = "2026-01-01", time = "10:00:00", zone = "UTC")
        assertNull(result.changes)
        assertFalse(result.errors.any)
    }

    @Test
    fun valid_location_is_accepted_and_date_is_left_alone() {
        val changes = validate(changeLocation = true, lat = "12.9716", lon = "77.5946").changes!!
        assertEquals(12.9716, changes.latitude!!, 0.000001)
        assertTrue(changes.changesLocation)
        assertFalse(changes.changesDate)
    }

    @Test
    fun bad_location_values_are_errors() {
        val result = validate(changeLocation = true, lat = "100", lon = "abc")
        assertNull(result.changes)
        assertNotNull(result.errors.latitude)
        assertNotNull(result.errors.longitude)
    }

    @Test
    fun empty_location_fields_are_errors_when_the_switch_is_on() {
        val result = validate(changeLocation = true)
        assertNull(result.changes)
        assertTrue(result.errors.any)
    }

    @Test
    fun valid_date_becomes_an_instant_in_the_chosen_zone() {
        val changes = validate(changeDate = true, date = "2026-10-13", time = "14:00:00", zone = "Asia/Kolkata").changes!!
        assertEquals(Instant.parse("2026-10-13T08:30:00Z"), changes.capturedAt)
        assertTrue(changes.changesDate)
        assertFalse(changes.changesLocation)
    }

    @Test
    fun bad_date_parts_report_their_own_errors() {
        val result = validate(changeDate = true, date = "13-10-2026", time = "25:00", zone = "Mars/Base")
        assertNull(result.changes)
        assertNotNull(result.errors.date)
        assertNotNull(result.errors.time)
        assertNotNull(result.errors.zone)
    }

    @Test
    fun both_sections_can_be_changed_together() {
        val changes = validate(
            changeLocation = true, lat = "1.5", lon = "2.5",
            changeDate = true, date = "2026-01-02", time = "03:04:05", zone = "UTC"
        ).changes!!
        assertTrue(changes.changesLocation)
        assertTrue(changes.changesDate)
    }
}
