package com.example.geostamp.domain.batch

import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.domain.stamp.StampContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BatchStampingTest {
    private val india = ZoneId.of("Asia/Kolkata")

    private fun photo(minute: Int, address: String? = "Old address", lat: Double? = 1.0, lon: Double? = 2.0) = StampContent(
        address = address, latitude = lat, longitude = lon, locationSource = LocationSource.GPS,
        capturedAt = Instant.parse("2026-10-22T06:" + minute.toString().padStart(2, '0') + ":00Z"), zoneId = india, label = "old note"
    )

    @Test
    fun a_chosen_place_is_applied_to_every_photo_and_each_keeps_its_own_time() {
        val options = BatchEditing.validate(
            BatchFields(placeName = "Example High School", latitude = "12.111111", longitude = "45.222222")
        ).options!!
        val first = BatchEditing.apply(photo(1), options)
        val second = BatchEditing.apply(photo(2), options)
        assertEquals("Example High School", first.placeName)
        assertEquals(12.111111, first.latitude!!, 0.0000001)
        assertEquals(LocationSource.MANUAL, first.locationSource)
        assertEquals(Instant.parse("2026-10-22T06:01:00Z"), first.capturedAt)
        assertEquals(Instant.parse("2026-10-22T06:02:00Z"), second.capturedAt)
    }

    @Test
    fun a_new_location_clears_the_old_address_so_they_never_disagree() {
        val options = BatchEditing.validate(BatchFields(latitude = "19.2", longitude = "73.0")).options!!
        assertNull(BatchEditing.apply(photo(1), options).address)
        val withAddress = BatchEditing.validate(BatchFields(latitude = "19.2", longitude = "73.0", address = "New road")).options!!
        assertEquals("New road", BatchEditing.apply(photo(1), withAddress).address)
    }

    @Test
    fun without_coordinates_each_photo_keeps_its_own_location() {
        val options = BatchEditing.validate(BatchFields(placeName = "A School", label = "Visit 1")).options!!
        val result = BatchEditing.apply(photo(1), options)
        assertEquals(1.0, result.latitude!!, 0.0)
        assertEquals("Old address", result.address)
        assertEquals(LocationSource.GPS, result.locationSource)
        assertEquals("A School", result.placeName)
        assertEquals("Visit 1", result.label)
    }

    @Test
    fun one_date_for_all_is_optional_and_checked() {
        val on = BatchEditing.validate(BatchFields(setDateTime = true, date = "2026-10-22", time = "11:39:00", zone = "Asia/Kolkata")).options!!
        assertTrue(on.hasDate)
        assertEquals(Instant.parse("2026-10-22T06:09:00Z"), BatchEditing.apply(photo(1), on).capturedAt)
        val off = BatchEditing.validate(BatchFields(setDateTime = false, date = "garbage")).options!!
        assertFalse(off.hasDate)
        assertTrue(off.isEmpty)
    }

    @Test
    fun bad_values_are_reported_and_nothing_is_applied() {
        val result = BatchEditing.validate(
            BatchFields(latitude = "200", longitude = "", setDateTime = true, date = "x", time = "y", zone = "Mars/Base")
        )
        assertNull(result.options)
        assertNotNull(result.errors.latitude)
        assertNotNull(result.errors.longitude)
        assertNotNull(result.errors.date)
        assertNotNull(result.errors.time)
        assertNotNull(result.errors.zone)
    }

    @Test
    fun blank_text_means_leave_it_alone() {
        val options = BatchEditing.validate(BatchFields(placeName = "  ", label = "")).options!!
        assertTrue(options.isEmpty)
        val base = photo(1)
        assertEquals(base, BatchEditing.apply(base, options))
    }
}
