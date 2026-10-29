package com.example.geostamp.domain.maps

import java.net.URI
import java.net.URLDecoder

class MapLocation(val latitude: Double, val longitude: Double, val name: String? = null)

/**
 * Finds coordinates in text copied or shared from Google Maps (or typed by hand).
 * Understands full Google Maps links, geo: links, "12.97, 77.59", "12.97 N, 77.59 E" and
 * degrees-minutes-seconds like 12°58'18.0"N 77°35'40.6"E. Pure Kotlin, unit tested.
 */
object MapLinkParser {
    private const val NUM = "-?\\d{1,3}(?:\\.\\d+)?"
    private val URL_REGEX = Regex("(https?://[^\\s<>\"]+|geo:[^\\s<>\"]+)", RegexOption.IGNORE_CASE)
    private val PLACE_PIN = Regex("!3d($NUM)!4d($NUM)")
    private val AT_PAIR = Regex("@($NUM),\\s*($NUM)")
    private val GEO = Regex("geo:\\s*($NUM),\\s*($NUM)", RegexOption.IGNORE_CASE)
    private val QUERY_PAIR = Regex("^\\s*(?:loc:)?($NUM)\\s*,\\s*($NUM)\\s*(?:\\((.*)\\))?\\s*$")
    private val PLACE_NAME = Regex("/place/([^/@?]+)")
    private val SHORT_LINK = Regex("https?://(?:maps\\.app\\.goo\\.gl|goo\\.gl/maps)[^\\s<>\"]*", RegexOption.IGNORE_CASE)
    private val DMS = Regex(
        "(\\d{1,3})\\s*°\\s*(\\d{1,2})\\s*['′]\\s*(\\d{1,2}(?:\\.\\d+)?)\\s*[\"″”]?\\s*([NSEWnsew])"
    )
    private val PREFIXED = Regex("(?<![A-Za-z])([NSEWnsew])(?![A-Za-z])\\s*($NUM)")
    private val SUFFIXED = Regex("($NUM)\\s*([NSEWnsew])(?![A-Za-z])")
    private val HEMISPHERE = Regex("(?<![A-Za-z])[NSEWnsew](?![A-Za-z])")
    private val NUMBER = Regex(NUM)

    fun parse(text: String): MapLocation? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        for (match in URL_REGEX.findAll(trimmed)) {
            parseUrl(cleanUrl(match.value))?.let { return it }
        }
        return parsePlain(trimmed)
    }

    /** A Google Maps short link (maps.app.goo.gl/...) found in the text, if any. */
    fun shortLink(text: String): String? =
        SHORT_LINK.find(text)?.value?.let { cleanUrl(it) }

    /** Removes punctuation that belongs to the sentence, not the link. Keeps balanced brackets. */
    private fun cleanUrl(raw: String): String {
        var url = raw.trimEnd('.', ',', ']', '>', ';')
        while (url.endsWith(")") && url.count { it == ')' } > url.count { it == '(' }) url = url.dropLast(1)
        return url
    }

    /** Only https links to Google or goo.gl are ever opened to follow a short link. */
    fun isTrustedMapUrl(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return false
        }
        if (uri.scheme?.lowercase() != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return host == "goo.gl" || host.endsWith(".goo.gl") ||
            host == "google.com" || host.endsWith(".google.com") ||
            Regex("(^|\\.)google\\.[a-z]{2,3}(\\.[a-z]{2})?$").containsMatchIn(host)
    }

    /** Google sometimes inserts a consent page; its continue= parameter holds the real address. */
    fun unwrapConsent(url: String): String {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return url
        }
        if (uri.host?.lowercase() != "consent.google.com") return url
        val query = uri.rawQuery ?: return url
        for (pair in query.split('&')) {
            if (pair.startsWith("continue=")) return safeDecode(pair.removePrefix("continue="))
        }
        return url
    }

    private fun parseUrl(url: String): MapLocation? {
        val decoded = safeDecode(url)
        val name = PLACE_NAME.find(decoded)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotEmpty() && !looksLikeCoordinates(it) }

        GEO.find(decoded)?.let { m -> location(m.groupValues[1], m.groupValues[2], name)?.let { return it } }
        PLACE_PIN.find(decoded)?.let { m -> location(m.groupValues[1], m.groupValues[2], name)?.let { return it } }

        val query = decoded.substringAfter('?', "").substringBefore('#')
        if (query.isNotEmpty()) {
            for (pair in query.split('&')) {
                val key = pair.substringBefore('=').lowercase()
                if (key !in QUERY_KEYS) continue
                val m = QUERY_PAIR.find(pair.substringAfter('=', "")) ?: continue
                val fromQuery = m.groupValues[3].trim().ifEmpty { null } ?: name
                location(m.groupValues[1], m.groupValues[2], fromQuery)?.let { return it }
            }
        }
        AT_PAIR.find(decoded)?.let { m -> location(m.groupValues[1], m.groupValues[2], name)?.let { return it } }
        return null
    }

    private fun parsePlain(text: String): MapLocation? {
        val dms = DMS.findAll(text).toList()
        if (dms.size == 2) {
            var lat: Double? = null
            var lon: Double? = null
            for (m in dms) {
                val degrees = m.groupValues[1].toDouble() + m.groupValues[2].toDouble() / 60 + m.groupValues[3].toDouble() / 3600
                when (m.groupValues[4].uppercase()) {
                    "N" -> lat = degrees
                    "S" -> lat = -degrees
                    "E" -> lon = degrees
                    "W" -> lon = -degrees
                }
            }
            if (lat != null && lon != null) return check(lat, lon, null)
            return null
        }

        val cleaned = text.replace('\u00B0', ' ')
        val prefixed = PREFIXED.findAll(cleaned).toList()
        if (prefixed.size == 2) {
            return fromHemispheres(prefixed.map { it.groupValues[1] to it.groupValues[2] })
        }
        val suffixed = SUFFIXED.findAll(cleaned).toList()
        if (suffixed.size == 2) {
            return fromHemispheres(suffixed.map { it.groupValues[2] to it.groupValues[1] })
        }
        if (HEMISPHERE.containsMatchIn(cleaned)) return null  // letters present but not a clean pair

        val numbers = NUMBER.findAll(cleaned).map { it.value.toDoubleOrNull() }.toList()
        if (numbers.size != 2 || numbers.any { it == null }) return null
        return check(numbers[0]!!, numbers[1]!!, null)
    }

    private fun fromHemispheres(pairs: List<Pair<String, String>>): MapLocation? {
        var lat: Double? = null
        var lon: Double? = null
        for ((letter, number) in pairs) {
            val value = number.toDoubleOrNull() ?: return null
            when (letter.uppercase()) {
                "N" -> lat = Math.abs(value)
                "S" -> lat = -Math.abs(value)
                "E" -> lon = Math.abs(value)
                "W" -> lon = -Math.abs(value)
            }
        }
        if (lat == null || lon == null) return null
        return check(lat, lon, null)
    }

    private fun location(latText: String, lonText: String, name: String?): MapLocation? {
        val lat = latText.toDoubleOrNull() ?: return null
        val lon = lonText.toDoubleOrNull() ?: return null
        return check(lat, lon, name)
    }

    private fun check(lat: Double, lon: Double, name: String?): MapLocation? {
        if (lat.isNaN() || lon.isNaN()) return null
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
        if (lat == 0.0 && lon == 0.0) return null
        return MapLocation(lat, lon, name)
    }

    private fun looksLikeCoordinates(text: String) = Regex("^$NUM\\s*,\\s*$NUM$").matches(text.trim())

    private fun safeDecode(text: String): String =
        try {
            URLDecoder.decode(text, "UTF-8")
        } catch (e: IllegalArgumentException) {
            text
        }

    private val QUERY_KEYS = setOf("q", "query", "ll", "center", "destination", "daddr", "saddr")
}
