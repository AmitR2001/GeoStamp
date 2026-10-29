// geostamp-iteration: 7
package com.example.geostamp.domain.stamp

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

internal inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default

/** Saves and loads stamp settings and stamp content as JSON (stored on the device only). */
object StampJson {

    fun configToJson(config: StampConfig): JSONObject = JSONObject().apply {
        put("textSizeFraction", config.textSizeFraction.toDouble())
        put("foregroundColor", config.foregroundColor)
        put("backgroundColor", config.backgroundColor)
        put("alignment", config.alignment.name)
        put("paddingFraction", config.paddingFraction.toDouble())
        put("dateStyle", config.dateStyle.name)
        put("timeStyle", config.timeStyle.name)
        put("showTimeZone", config.showTimeZone)
        put("showLabel", config.showLabel)
        put("layoutStyle", config.layoutStyle.name)
        put("showMap", config.showMap)
    }

    fun configFromJson(json: JSONObject): StampConfig {
        val d = StampConfig()
        return StampConfig(
            textSizeFraction = json.optDouble("textSizeFraction", d.textSizeFraction.toDouble()).toFloat(),
            foregroundColor = json.optInt("foregroundColor", d.foregroundColor),
            backgroundColor = json.optInt("backgroundColor", d.backgroundColor),
            alignment = enumOrDefault(json.optString("alignment"), d.alignment),
            paddingFraction = json.optDouble("paddingFraction", d.paddingFraction.toDouble()).toFloat(),
            dateStyle = enumOrDefault(json.optString("dateStyle"), d.dateStyle),
            timeStyle = enumOrDefault(json.optString("timeStyle"), d.timeStyle),
            showTimeZone = json.optBoolean("showTimeZone", d.showTimeZone),
            showLabel = json.optBoolean("showLabel", d.showLabel),
            layoutStyle = enumOrDefault(json.optString("layoutStyle"), d.layoutStyle),
            showMap = json.optBoolean("showMap", d.showMap)
        )
    }

    fun contentToJson(content: StampContent): JSONObject = JSONObject().apply {
        content.address?.let { put("address", it) }
        content.latitude?.let { put("latitude", it) }
        content.longitude?.let { put("longitude", it) }
        put("locationSource", content.locationSource.name)
        put("capturedAtMillis", content.capturedAt.toEpochMilli())
        put("zoneId", content.zoneId.id)
        content.label?.let { put("label", it) }
        content.placeName?.let { put("placeName", it) }
    }

    fun contentFromJson(json: JSONObject): StampContent {
        val zone = runCatching { ZoneId.of(json.optString("zoneId")) }.getOrDefault(ZoneId.systemDefault())
        return StampContent(
            address = if (json.isNull("address")) null else json.optString("address"),
            latitude = if (json.isNull("latitude")) null else json.getDouble("latitude"),
            longitude = if (json.isNull("longitude")) null else json.getDouble("longitude"),
            locationSource = enumOrDefault(json.optString("locationSource"), LocationSource.UNAVAILABLE),
            capturedAt = Instant.ofEpochMilli(json.optLong("capturedAtMillis", 0L)),
            zoneId = zone,
            label = if (json.isNull("label")) null else json.optString("label"),
            placeName = if (json.isNull("placeName")) null else json.optString("placeName")
        )
    }
}
