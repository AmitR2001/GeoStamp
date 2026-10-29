package com.example.geostamp.domain.model

import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PhotoRecordJsonTest {

    @Test
    fun full_record_round_trips() {
        val record = PhotoRecord(
            id = "id-1",
            createdAtMillis = 1_700_000_000_000L,
            source = PhotoSource.CAMERA,
            originalFileName = "id-1.jpg",
            savedUri = "content://media/external/images/media/42",
            savedPath = "Pictures/GeoStamp/Camera/GeoStamp_x.jpg",
            displayName = "GeoStamp_x.jpg",
            album = "Trips",
            content = StampContent(
                address = "Somewhere",
                latitude = 1.5,
                longitude = -2.5,
                locationSource = LocationSource.MANUAL,
                capturedAt = Instant.parse("2026-10-13T08:30:00Z"),
                zoneId = ZoneId.of("Asia/Kolkata"),
                label = "Label"
            ),
            config = StampConfig(textSizeFraction = 0.04f),
            rotationDegrees = 90
        )
        val copy = PhotoRecordJson.fromJson(JSONObject(PhotoRecordJson.toJson(record).toString()))
        assertEquals(record, copy)
    }

    @Test
    fun minimal_record_round_trips() {
        val record = PhotoRecord(
            id = "id-2", createdAtMillis = 5L, source = PhotoSource.IMPORTED,
            originalFileName = null, savedUri = null, savedPath = null, displayName = "x.jpg"
        )
        val copy = PhotoRecordJson.fromJson(JSONObject(PhotoRecordJson.toJson(record).toString()))
        assertEquals(record, copy)
        assertNull(copy.content)
        assertNull(copy.config)
        assertNull(copy.album)
    }
}
