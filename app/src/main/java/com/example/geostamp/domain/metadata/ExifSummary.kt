package com.example.geostamp.domain.metadata

import java.util.Locale

class ExifEntry(val label: String, val value: String)

/** Turns raw EXIF tag values into a short, readable list. Pure Kotlin, unit tested. */
object ExifSummary {
    val TAGS = listOf(
        "DateTimeOriginal", "DateTime", "OffsetTimeOriginal", "Make", "Model", "LensModel",
        "ExposureTime", "FNumber", "PhotographicSensitivity", "FocalLength",
        "Software", "Orientation", "ImageWidth", "ImageLength"
    )

    fun build(raw: Map<String, String?>, latitude: Double?, longitude: Double?): List<ExifEntry> {
        val entries = mutableListOf<ExifEntry>()
        fun add(label: String, tag: String, transform: (String) -> String = { it }) {
            val value = raw[tag]?.trim()
            if (!value.isNullOrEmpty()) entries.add(ExifEntry(label, transform(value)))
        }
        add("Date taken", "DateTimeOriginal")
        add("Date of last change", "DateTime")
        add("Time offset", "OffsetTimeOriginal")
        add("Camera maker", "Make")
        add("Camera model", "Model")
        add("Lens", "LensModel")
        add("Exposure time", "ExposureTime") { "$it s" }
        add("Aperture", "FNumber") { "f/$it" }
        add("ISO", "PhotographicSensitivity")
        add("Focal length", "FocalLength") { "$it mm" }
        add("Software", "Software")
        add("Orientation", "Orientation") { orientationText(it) }
        val width = raw["ImageWidth"]?.trim()
        val height = raw["ImageLength"]?.trim()
        if (!width.isNullOrEmpty() && !height.isNullOrEmpty()) {
            entries.add(ExifEntry("Size in metadata", "$width x $height px"))
        }
        entries.add(
            ExifEntry(
                "GPS location",
                if (latitude != null && longitude != null)
                    String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
                else "None in this file"
            )
        )
        return entries
    }

    fun orientationText(code: String): String = when (code.trim()) {
        "1" -> "Normal"
        "3" -> "Rotated 180\u00B0"
        "6" -> "Rotated 90\u00B0 clockwise"
        "8" -> "Rotated 270\u00B0 clockwise"
        "2" -> "Mirrored"
        "4" -> "Mirrored vertically"
        else -> "Code $code"
    }
}
