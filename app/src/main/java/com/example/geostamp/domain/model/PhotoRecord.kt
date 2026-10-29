package com.example.geostamp.domain.model

import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampJson
import com.example.geostamp.domain.stamp.enumOrDefault
import org.json.JSONObject

enum class PhotoSource { CAMERA, IMPORTED, EDITED }

/**
 * One photo GeoStamp knows about. The original picture is kept in app-private storage
 * (originalFileName) so the stamp can be edited later without recompressing the photo again.
 */
data class PhotoRecord(
    val id: String,
    val createdAtMillis: Long,
    val source: PhotoSource,
    val originalFileName: String?,
    val savedUri: String?,
    val savedPath: String?,
    val displayName: String,
    val album: String? = null,
    val content: StampContent? = null,
    val config: StampConfig? = null,
    val rotationDegrees: Int = 0
)

object PhotoRecordJson {
    fun toJson(record: PhotoRecord): JSONObject = JSONObject().apply {
        put("id", record.id)
        put("createdAtMillis", record.createdAtMillis)
        put("source", record.source.name)
        record.originalFileName?.let { put("originalFileName", it) }
        record.savedUri?.let { put("savedUri", it) }
        record.savedPath?.let { put("savedPath", it) }
        put("displayName", record.displayName)
        record.album?.let { put("album", it) }
        record.content?.let { put("content", StampJson.contentToJson(it)) }
        record.config?.let { put("config", StampJson.configToJson(it)) }
        put("rotationDegrees", record.rotationDegrees)
    }

    fun fromJson(json: JSONObject): PhotoRecord = PhotoRecord(
        id = json.getString("id"),
        createdAtMillis = json.optLong("createdAtMillis", 0L),
        source = enumOrDefault(json.optString("source"), PhotoSource.CAMERA),
        originalFileName = if (json.isNull("originalFileName")) null else json.optString("originalFileName"),
        savedUri = if (json.isNull("savedUri")) null else json.optString("savedUri"),
        savedPath = if (json.isNull("savedPath")) null else json.optString("savedPath"),
        displayName = json.optString("displayName", ""),
        album = if (json.isNull("album")) null else json.optString("album"),
        content = json.optJSONObject("content")?.let { StampJson.contentFromJson(it) },
        config = json.optJSONObject("config")?.let { StampJson.configFromJson(it) },
        rotationDegrees = json.optInt("rotationDegrees", 0)
    )
}
