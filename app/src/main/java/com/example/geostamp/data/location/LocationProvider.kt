package com.example.geostamp.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.geostamp.domain.location.LocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Asks Android for the current location, one time, only when a photo is taken.
 * Works offline (GPS). Returns Unavailable with a reason instead of guessing.
 */
class LocationProvider(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private fun hasFine() = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasCoarse() = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun hasPermission(): Boolean = hasFine() || hasCoarse()

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(timeoutMs: Long = 12_000): LocationResult {
        val lm = manager ?: return LocationResult.Unavailable(LocationResult.Reason.ERROR)
        val precise = hasFine()
        if (!precise && !hasCoarse()) {
            return LocationResult.Unavailable(LocationResult.Reason.PERMISSION_DENIED)
        }
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            return LocationResult.Unavailable(LocationResult.Reason.LOCATION_OFF)
        }

        val providers = mutableListOf<String>()
        if (precise && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER)
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER)
        if (providers.isEmpty()) return LocationResult.Unavailable(LocationResult.Reason.NO_FIX)

        val perProviderTimeout = timeoutMs / providers.size
        for (provider in providers) {
            val location = try {
                getFix(lm, provider, perProviderTimeout)
            } catch (e: SecurityException) {
                return LocationResult.Unavailable(LocationResult.Reason.PERMISSION_DENIED)
            }
            if (location != null) return toFix(location, precise)
        }

        // Last resort: a recent real fix that Android already has (never older than 2 minutes).
        val now = System.currentTimeMillis()
        val recent = providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.time <= MAX_LAST_KNOWN_AGE_MS }
            .maxByOrNull { it.time }
        return if (recent != null) toFix(recent, precise)
        else LocationResult.Unavailable(LocationResult.Reason.NO_FIX)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFix(lm: LocationManager, provider: String, timeoutMs: Long): Location? =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    lm, provider, signal, ContextCompat.getMainExecutor(appContext)
                ) { location -> if (cont.isActive) cont.resume(location) }
            }
        }

    private fun toFix(location: Location, precise: Boolean) = LocationResult.Fix(
        latitude = location.latitude,
        longitude = location.longitude,
        accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
        approximate = !precise,
        fixTimeMillis = location.time
    )

    private companion object {
        const val MAX_LAST_KNOWN_AGE_MS = 120_000L
    }
}
