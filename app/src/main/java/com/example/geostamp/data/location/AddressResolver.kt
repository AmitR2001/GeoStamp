package com.example.geostamp.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Best-effort street address for a coordinate using Android's built-in Geocoder.
 * Needs a network on most phones. Returns null when offline or when nothing is found,
 * and never makes up an address.
 */
class AddressResolver(context: Context) {
    private val appContext = context.applicationContext

    suspend fun resolve(latitude: Double, longitude: Double, locale: Locale = Locale.getDefault()): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(appContext, locale)
        return try {
            withTimeoutOrNull(TIMEOUT_MS) {
                if (Build.VERSION.SDK_INT >= 33) {
                    geocodeAsync(geocoder, latitude, longitude)
                } else {
                    withContext(Dispatchers.IO) { geocodeBlocking(geocoder, latitude, longitude) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun geocodeAsync(geocoder: Geocoder, latitude: Double, longitude: Double): String? =
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (cont.isActive) cont.resume(format(addresses.firstOrNull()))
                }

                override fun onError(errorMessage: String?) {
                    if (cont.isActive) cont.resume(null)
                }
            })
        }

    @Suppress("DEPRECATION")
    private fun geocodeBlocking(geocoder: Geocoder, latitude: Double, longitude: Double): String? =
        format(geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull())

    private fun format(address: Address?): String? {
        if (address == null) return null
        val line = if (address.maxAddressLineIndex >= 0) address.getAddressLine(0) else null
        val text = line?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(address.featureName, address.thoroughfare, address.locality, address.adminArea, address.countryName)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
        return text.ifBlank { null }
    }

    private companion object {
        const val TIMEOUT_MS = 8_000L
    }
}
