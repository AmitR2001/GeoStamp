package com.example.geostamp.domain.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifSummaryTest {

    private fun find(entries: List<ExifEntry>, label: String) = entries.firstOrNull { it.label == label }?.value

    @Test
    fun known_tags_get_readable_labels() {
        val entries = ExifSummary.build(
            mapOf("Make" to "Google", "Model" to "Pixel 8", "FNumber" to "1.7", "ExposureTime" to "0.01", "Orientation" to "6"),
            null, null
        )
        assertEquals("Google", find(entries, "Camera maker"))
        assertEquals("Pixel 8", find(entries, "Camera model"))
        assertEquals("f/1.7", find(entries, "Aperture"))
        assertEquals("0.01 s", find(entries, "Exposure time"))
        assertEquals("Rotated 90\u00B0 clockwise", find(entries, "Orientation"))
    }

    @Test
    fun blank_and_missing_values_are_skipped() {
        val entries = ExifSummary.build(mapOf("Make" to "  ", "Model" to null), null, null)
        assertFalse(entries.any { it.label == "Camera maker" })
        assertFalse(entries.any { it.label == "Camera model" })
    }

    @Test
    fun gps_is_always_reported_either_way() {
        assertEquals("None in this file", find(ExifSummary.build(emptyMap(), null, null), "GPS location"))
        assertEquals("12.971600, 77.594600", find(ExifSummary.build(emptyMap(), 12.9716, 77.5946), "GPS location"))
    }

    @Test
    fun size_needs_both_numbers() {
        assertEquals("4000 x 3000 px", find(ExifSummary.build(mapOf("ImageWidth" to "4000", "ImageLength" to "3000"), null, null), "Size in metadata"))
        assertTrue(ExifSummary.build(mapOf("ImageWidth" to "4000"), null, null).none { it.label == "Size in metadata" })
    }

    @Test
    fun unknown_orientation_codes_are_shown_as_codes() {
        assertEquals("Code 7", ExifSummary.orientationText("7"))
        assertEquals("Normal", ExifSummary.orientationText("1"))
    }
}
