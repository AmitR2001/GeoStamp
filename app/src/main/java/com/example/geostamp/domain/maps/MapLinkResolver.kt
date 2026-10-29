package com.example.geostamp.domain.maps

import java.io.IOException
import java.net.URL

sealed interface MapLookup {
    class Found(val location: MapLocation, val usedInternet: Boolean) : MapLookup
    class Failed(val message: String) : MapLookup
}

/** Returns the Location header of a redirect, or null when the address does not redirect. */
interface RedirectSource {
    @Throws(IOException::class)
    suspend fun nextLocation(url: String): String?
}

/**
 * Turns pasted or shared text into coordinates. Text that already contains coordinates never
 * touches the internet. A Google Maps short link needs a few redirect requests, each one only to a
 * trusted Google address; no photo or personal data is sent.
 */
class MapLinkResolver(private val redirects: RedirectSource) {

    suspend fun resolve(text: String): MapLookup {
        MapLinkParser.parse(text)?.let { return MapLookup.Found(it, false) }

        val shortLink = MapLinkParser.shortLink(text)
            ?: return MapLookup.Failed(
                "No coordinates found. Paste coordinates like 12.9716, 77.5946 or a Google Maps link."
            )

        var current = shortLink
        for (hop in 1..MAX_HOPS) {
            if (!MapLinkParser.isTrustedMapUrl(current)) {
                return MapLookup.Failed("The link leads somewhere other than Google Maps, so it was not opened.")
            }
            val next = try {
                redirects.nextLocation(current)
            } catch (e: IOException) {
                return MapLookup.Failed(
                    "The short link could not be opened (is there an internet connection?). " +
                        "Open it in Maps and copy the coordinates instead."
                )
            } ?: break

            val absolute = absolute(next, current)
            MapLinkParser.parse(absolute)?.let { return MapLookup.Found(it, true) }
            current = MapLinkParser.unwrapConsent(absolute)
            MapLinkParser.parse(current)?.let { return MapLookup.Found(it, true) }
        }
        return MapLookup.Failed(
            "That short link did not contain a location. Open it in Maps, long-press the place and copy the coordinates."
        )
    }

    private fun absolute(location: String, base: String): String =
        try {
            URL(URL(base), location).toString()
        } catch (e: Exception) {
            location
        }

    private companion object {
        const val MAX_HOPS = 6
    }
}
