package me.mudkip.moememos.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** One foreground fix from Android's providers, including on devices without Play services. */
suspend fun currentDeviceLocation(context: Context): Location? = withContext(Dispatchers.Main.immediate) {
    if (!hasLocationPermission(context)) return@withContext null
    val manager = context.getSystemService(LocationManager::class.java) ?: return@withContext null
    val precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val providers = buildList {
        // Android 12+ also permits approximate fixes from GPS, useful on GPS-only devices.
        if (precise || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }.filter { manager.isProviderEnabled(it) }
    if (providers.isEmpty()) return@withContext null

    withTimeoutOrNull(30_000) {
        suspendCancellableCoroutine { continuation ->
            val signals = providers.map { CancellationSignal() }
            continuation.invokeOnCancellation { signals.forEach { it.cancel() } }
            var remaining = providers.size
            providers.forEachIndexed { index, provider ->
                if (continuation.isActive) {
                    try {
                        LocationManagerCompat.getCurrentLocation(
                            manager, provider, signals[index], ContextCompat.getMainExecutor(context)
                        ) { location ->
                            if (continuation.isActive) {
                                remaining--
                                if (location != null || remaining == 0) {
                                    signals.forEach { it.cancel() }
                                    continuation.resume(location)
                                }
                            }
                        }
                    } catch (_: SecurityException) {
                        remaining--
                        if (continuation.isActive && remaining == 0) continuation.resume(null)
                    } catch (_: IllegalArgumentException) {
                        remaining--
                        if (continuation.isActive && remaining == 0) continuation.resume(null)
                    }
                }
            }
        }
    }
}
