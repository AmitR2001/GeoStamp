// geostamp-iteration: 7
package com.example.geostamp.domain.stamp

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StampJsonTest {

    @Test
    fun config_round_trips() {
        val config = StampConfig(
            textSizeFraction = 0.045f,
            foregroundColor = 0xFFFFEB3B.toInt(),
            backgroundColor = 0x80000000.toInt(),
            alignment = StampAlignment.END,
            paddingFraction = 0.03f,
            dateStyle = DateStyle.ISO,
            timeStyle = TimeStyle.H12,
            showTimeZone = false,
            showLabel = false,
            layoutStyle = StampLayoutStyle.CARD,
            showMap = false
        )
        assertEquals(config, StampJson.configFromJson(JSONObject(StampJson.configToJson(config).toString())))
    }

    @Test
    fun unknown_and_old_settings_fall_back_to_defaults() {
        val copy = StampJson.configFromJson(JSONObject("""{"alignment":"SIDEWAYS","showLocationSourceTag":true}"""))
        assertEquals(StampConfig(), copy)
    }

    @Test
    fun content_round_trips_with_everything_set() {
        val content = StampContent(
            address = "MG Road, Bengaluru",
            latitude = 12.9716,
            longitude = 77.5946,
            locationSource = LocationSource.GPS,
            capturedAt = Instant.parse("2026-10-13T08:30:00Z"),
            zoneId = ZoneId.of("Asia/Kolkata"),
            label = "Site visit",
            placeName = "Example High School"
        )
        assertEquals(content, StampJson.contentFromJson(JSONObject(StampJson.contentToJson(content).toString())))
    }

    @Test
    fun content_round_trips_with_nothing_optional() {
        val content = StampContent(capturedAt = Instant.parse("2026-01-01T00:00:00Z"), zoneId = ZoneId.of("UTC"))
        val copy = StampJson.contentFromJson(JSONObject(StampJson.contentToJson(content).toString()))
        assertEquals(content, copy)
        assertNull(copy.address)
        assertNull(copy.placeName)
        assertEquals(LocationSource.UNAVAILABLE, copy.locationSource)
    }
}
