package com.example.geostamp.data.maps

import com.example.geostamp.domain.maps.RedirectSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Asks a Google Maps short link where it points to. Sends one plain request, reads only the
 * redirect address from the response headers, and never downloads a page or sends anything of yours.
 */
class HttpRedirectSource : RedirectSource {
    override suspend fun nextLocation(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) GeoStamp")
                val code = connection.responseCode
                if (code in 300..399) connection.getHeaderField("Location") else null
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            throw e
        } catch (e: SecurityException) {
            throw IOException("This app is not allowed to use the internet.")
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
    }
}
