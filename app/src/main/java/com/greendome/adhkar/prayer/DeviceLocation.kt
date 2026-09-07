package com.greendome.adhkar.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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

    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun lastKnown(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
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
        if (!isEnabled(context)) return lastKnown(context)?.takeIf { maxAgeMs <= 0L || ageMs(it) < maxAgeMs }
        if (maxAgeMs > 0L) {
            lastKnown(context)?.takeIf { ageMs(it) < maxAgeMs }?.let { return it }
            fusedLast(context, maxAgeMs)?.let { return it }
        }
        requestFused(context, timeoutMs, preferGps)?.let { return it }
        requestLegacy(context, timeoutMs, preferGps)?.let { return it }
        return lastKnown(context)
    }

    private fun ageMs(location: Location): Long =
        (System.currentTimeMillis() - location.time).coerceAtLeast(0L)

    private fun playServicesReady(context: Context): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
            ConnectionResult.SUCCESS

    @SuppressLint("MissingPermission")
    private suspend fun fusedLast(context: Context, maxAgeMs: Long): Location? {
        if (!playServicesReady(context) || !hasPermission(context)) return null
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val location = withTimeoutOrNull(1_500L) {
            suspendCancellableCoroutine { cont ->
                fused.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }
        return location?.takeIf { maxAgeMs <= 0L || ageMs(it) < maxAgeMs }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFused(
        context: Context,
        timeoutMs: Long,
        preferGps: Boolean,
    ): Location? {
        if (!playServicesReady(context) || !hasPermission(context)) return null
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val priority = if (preferGps && hasFinePermission(context)) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val cancel = CancellationTokenSource()
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { cancel.cancel() }
                fused.getCurrentLocation(priority, cancel.token)
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLegacy(
        context: Context,
        timeoutMs: Long,
        preferGps: Boolean,
    ): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val gpsReady = manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && hasFinePermission(context)
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                runCatching { manager.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false)
            ) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (preferGps && gpsReady) add(LocationManager.GPS_PROVIDER)
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (!preferGps && gpsReady && LocationManager.GPS_PROVIDER !in this) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (preferGps && gpsReady && LocationManager.GPS_PROVIDER !in this) {
                add(LocationManager.GPS_PROVIDER)
            }
        }.distinct()
        if (providers.isEmpty()) return lastKnown(context)
        return suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (cont.isActive) {
                        providers.forEach { runCatching { manager.removeUpdates(this) } }
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
                providers.forEach { runCatching { manager.removeUpdates(listener) } }
                if (cont.isActive) cont.resume(lastKnown(context))
            }
            cont.invokeOnCancellation {
                providers.forEach { runCatching { manager.removeUpdates(listener) } }
                handler.removeCallbacks(timeout)
            }
            var started = false
            providers.forEach { provider ->
                runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    started = true
                }
            }
            if (!started) {
                if (cont.isActive) cont.resume(lastKnown(context))
                return@suspendCancellableCoroutine
            }
            handler.postDelayed(timeout, timeoutMs)
        }
    }

    sealed class SettingsState {
        data object Enabled : SettingsState()
        data class ResolutionRequired(val sender: IntentSender) : SettingsState()
        data object Unavailable : SettingsState()
    }

    suspend fun checkSettings(
        context: Context,
        preferGps: Boolean = false,
    ): SettingsState {
        if (isEnabled(context)) return SettingsState.Enabled
        if (!playServicesReady(context)) return SettingsState.Unavailable
        val priority = if (preferGps && hasFinePermission(context)) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val request = LocationRequest.Builder(priority, 5_000L).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .setAlwaysShow(true)
            .build()
        return suspendCancellableCoroutine { cont ->
            LocationServices.getSettingsClient(context)
                .checkLocationSettings(settingsRequest)
                .addOnSuccessListener {
                    if (cont.isActive) cont.resume(SettingsState.Enabled)
                }
                .addOnFailureListener { error ->
                    if (!cont.isActive) return@addOnFailureListener
                    if (error is ResolvableApiException) {
                        cont.resume(SettingsState.ResolutionRequired(error.resolution.intentSender))
                    } else {
                        cont.resume(SettingsState.Unavailable)
                    }
                }
        }
    }
}
