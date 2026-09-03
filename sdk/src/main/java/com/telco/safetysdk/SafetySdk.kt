package com.telco.safetysdk

import android.content.Context
import com.telco.safetysdk.internal.EventHub
import com.telco.safetysdk.internal.Prefs
import com.telco.safetysdk.monitor.DeviceMonitor
import com.telco.safetysdk.monitor.LocationMonitor
import com.telco.safetysdk.parental.ParentalPolicy
import com.telco.safetysdk.safety.SafetyStore
import com.telco.safetysdk.security.DeviceSecurity

class SafetySdk private constructor(context: Context) {

    private val app = context.applicationContext
    private val prefs = Prefs(app)
    private val hub = EventHub()
    val safety = SafetyStore(prefs, hub)
    val parental = ParentalPolicy(prefs, hub)
    val security = DeviceSecurity(app)
    private val device = DeviceMonitor(app, prefs, hub, parental)
    private val location = LocationMonitor(app, prefs, hub, safety, parental)

    fun capabilities(): List<Capability> = CapabilityCatalog.all()

    fun addListener(listener: SdkListener) = hub.add(listener)

    fun removeListener(listener: SdkListener) = hub.remove(listener)

    fun startDeviceMonitors() = device.start()

    fun stopDeviceMonitors() = device.stop()

    fun snapshot(): DeviceSnapshot = device.snapshot()

    fun startLocationMonitoring() = location.start()

    fun stopLocationMonitoring() = location.stop()

    fun lastLocation(): Pair<Double, Double>? = location.lastLatLng()

    fun triggerSos(note: String = "") = location.triggerSos(note)

    fun wellbeingEvents(): List<SdkEvent> = hub.recent()

    companion object {
        @Volatile private var instance: SafetySdk? = null

        fun get(context: Context): SafetySdk =
            instance ?: synchronized(this) {
                instance ?: SafetySdk(context).also { instance = it }
            }
    }
}

data class DeviceSnapshot(
    val charging: Boolean,
    val batteryPct: Int,
    val screenOn: Boolean,
    val hostForeground: Boolean,
    val wifiSsid: String?,
    val wifiBssid: String?,
    val matchedIspOuis: List<String>,
    val hotspotOn: Boolean,
    val tethered: Boolean,
    val signalDbm: Int?,
    val poorSignal: Boolean,
    val roaming: Boolean,
    val networkCountry: String?,
    val simCountry: String?,
    val abroad: Boolean,
    val sims: List<SimInfo>,
    val defaultDataSubId: Int?,
    val lastUnlockAt: Long?,
    val lastWifiChangeAt: Long?,
    val lastHeartbeatAt: Long?,
    val simChanged: Boolean,
    val rootedHeuristic: Boolean
)

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val carrierName: String,
    val mccMnc: String,
    val iccidSuffix: String,
    val isCompetitor: Boolean
)
