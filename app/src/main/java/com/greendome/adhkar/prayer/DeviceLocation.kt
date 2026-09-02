package com.greendome.adhkar.prayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object DeviceLocation {
    fun hasFinePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun hasPermission(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    fun lastKnown(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    suspend fun requestCurrent(
        context: Context,
        timeoutMs: Long = 12_000L,
        maxAgeMs: Long = 15 * 60_000L,
        preferGps: Boolean = false,
    ): Location? {
        if (!hasPermission(context)) return null
        if (maxAgeMs > 0L) {
            lastKnown(context)?.takeIf { System.currentTimeMillis() - it.time < maxAgeMs }?.let {
                return it
            }
        }
        val manager = context.getSystemService(LocationManager::class.java) ?: return lastKnown(context)
        val gpsReady = manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && hasFinePermission(context)
        val provider = when {
            preferGps && gpsReady -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            gpsReady -> LocationManager.GPS_PROVIDER
            else -> return lastKnown(context)
        }
        return suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (cont.isActive) {
                        manager.removeUpdates(this)
                        handler.removeCallbacksAndMessages(null)
                        cont.resume(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit
            }
            val timeout = Runnable {
                manager.removeUpdates(listener)
                if (cont.isActive) cont.resume(lastKnown(context))
            }
            cont.invokeOnCancellation {
                manager.removeUpdates(listener)
                handler.removeCallbacks(timeout)
            }
            runCatching {
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                handler.postDelayed(timeout, timeoutMs)
            }.onFailure {
                if (cont.isActive) cont.resume(lastKnown(context))
            }
        }
    }
}
