package com.telco.safetysdk.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.telco.safetysdk.SdkEvent
import com.telco.safetysdk.geo.Geo
import com.telco.safetysdk.internal.EventHub
import com.telco.safetysdk.internal.Prefs
import com.telco.safetysdk.parental.ParentalPolicy
import com.telco.safetysdk.safety.SafetyStore
import java.util.concurrent.atomic.AtomicBoolean

class LocationMonitor(
    private val context: Context,
    private val prefs: Prefs,
    private val hub: EventHub,
    private val safety: SafetyStore,
    private val parental: ParentalPolicy
) {
    private val running = AtomicBoolean(false)
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var last: Location? = null
    private val inside = mutableSetOf<String>()
    private var driving = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            last = location
            prefs.putLong("heartbeat", System.currentTimeMillis())
            evaluate(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasLocation() || !running.compareAndSet(false, true)) return
        val minMs = 15_000L
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minMs, 20f, listener, Looper.getMainLooper())
        }
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minMs, 20f, listener, Looper.getMainLooper())
        }
        hub.emit(SdkEvent("network_location", "started", "gps+network location loop"))
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        lm.removeUpdates(listener)
    }

    fun lastLatLng(): Pair<Double, Double>? = last?.let { it.latitude to it.longitude }

    fun triggerSos(note: String) {
        val loc = lastLatLng()
        val members = safety.members().filter { it.consented }
        hub.emit(
            SdkEvent(
                "sos",
                "sos",
                note.ifBlank { "SOS" },
                mapOf(
                    "lat" to (loc?.first?.toString() ?: ""),
                    "lng" to (loc?.second?.toString() ?: ""),
                    "members" to members.joinToString { it.phone }
                )
            )
        )
    }

    private fun evaluate(location: Location) {
        val speedKmh = location.speed * 3.6
        val nowDriving = location.hasSpeed() && speedKmh >= 25
        if (nowDriving != driving) {
            driving = nowDriving
            hub.emit(
                SdkEvent(
                    "driving",
                    if (driving) "enter" else "exit",
                    "${speedKmh.toInt()} km/h — silence notifications if DND granted"
                )
            )
            if (parental.shouldSilenceNotifications(driving)) {
                hub.emit(SdkEvent("driving", "silence", "policy: mute while driving"))
            }
        }
        for (z in safety.geofences()) {
            val inZ = Geo.insideZone(location.latitude, location.longitude, z.lat, z.lng, z.radiusMeters)
            val was = inside.contains(z.id)
            if (inZ && !was) {
                inside.add(z.id)
                hub.emit(SdkEvent("geofence", "enter", z.name))
            } else if (!inZ && was) {
                inside.remove(z.id)
                hub.emit(SdkEvent("geofence", "exit", z.name))
            }
        }
        for (r in safety.routes()) {
            if (Geo.offRoute(location.latitude, location.longitude, r.points, r.corridorMeters)) {
                hub.emit(SdkEvent("geofence", "route_deviation", r.name))
            }
        }
        hub.emit(
            SdkEvent(
                "live_location",
                "fix",
                "${location.latitude}, ${location.longitude}",
                mapOf("provider" to (location.provider ?: ""), "speed" to speedKmh.toInt().toString())
            )
        )
    }

    private fun hasLocation() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
