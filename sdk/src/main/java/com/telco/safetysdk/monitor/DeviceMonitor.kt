package com.telco.safetysdk.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telco.safetysdk.DeviceSnapshot
import com.telco.safetysdk.SdkEvent
import com.telco.safetysdk.SimInfo
import com.telco.safetysdk.internal.EventHub
import com.telco.safetysdk.internal.Prefs
import com.telco.safetysdk.parental.ParentalPolicy
import com.telco.safetysdk.security.DeviceSecurity
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

class DeviceMonitor(
    private val context: Context,
    private val prefs: Prefs,
    private val hub: EventHub,
    private val parental: ParentalPolicy
) {
    private val running = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private val security = DeviceSecurity(context)
    private var hostForeground = true
    private var signalDbm: Int? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var lastPoor: Boolean? = null
    private var lastAbroad: Boolean? = null
    private var lastRoaming: Boolean? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val pct = batteryPct(intent)
            val charging = charging(intent)
            prefs.putLong("heartbeat", System.currentTimeMillis())
            if (pct <= 15) {
                hub.emit(SdkEvent("battery_off", "battery_low", "$pct%", mapOf("pct" to pct.toString())))
            }
            if (charging && !screenOn()) {
                hub.emit(SdkEvent("battery", "charging_idle", "charging and screen off"))
            }
        }
    }

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            prefs.putLong("last_unlock", System.currentTimeMillis())
            hub.emit(SdkEvent("screen_unlock", "unlock", "ACTION_USER_PRESENT"))
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            prefs.putLong("last_wifi", System.currentTimeMillis())
            val snap = snapshot()
            hub.emit(
                SdkEvent(
                    "wifi_fingerprint",
                    "wifi",
                    snap.wifiSsid ?: "none",
                    mapOf("bssid" to (snap.wifiBssid ?: ""), "isp" to snap.matchedIspOuis.joinToString())
                )
            )
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            hostForeground = true
            hub.emit(SdkEvent("app_foreground", "foreground", "host app in foreground"))
        }

        override fun onStop(owner: LifecycleOwner) {
            hostForeground = false
            hub.emit(SdkEvent("app_foreground", "background", "host app in background"))
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running.get()) return
            prefs.putLong("heartbeat", System.currentTimeMillis())
            val snap = snapshot()
            if (snap.poorSignal && lastPoor != true) {
                hub.emit(SdkEvent("signal", "poor", "${snap.signalDbm} dBm — suppress rich media"))
            }
            lastPoor = snap.poorSignal
            if (snap.abroad && lastAbroad != true) {
                hub.emit(SdkEvent("abroad", "entered", "network=${snap.networkCountry} sim=${snap.simCountry}"))
            }
            lastAbroad = snap.abroad
            if (snap.roaming && lastRoaming != true) {
                hub.emit(SdkEvent("roaming", "roaming", "network roaming detected — cannot disable attach from SDK"))
            }
            lastRoaming = snap.roaming
            if (snap.simChanged) {
                hub.emit(SdkEvent("sim_change", "iccid_changed", "SIM subscription fingerprint changed"))
            }
            val inactiveMs = 30 * 60 * 1000L
            val now = System.currentTimeMillis()
            val noUnlock = snap.lastUnlockAt?.let { now - it > inactiveMs } ?: true
            val noWifi = snap.lastWifiChangeAt?.let { now - it > inactiveMs } ?: true
            if (noUnlock && noWifi) {
                hub.emit(
                    SdkEvent(
                        "broken_routine",
                        "inactive",
                        "no unlock + no Wi-Fi change (STB/wearable not available to this SDK)"
                    )
                )
            }
            val cal = Calendar.getInstance()
            val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            if (parental.shouldBlockInternet(mins, driving = false)) {
                hub.emit(SdkEvent("internet_pause", "policy_block", "local policy says block data"))
            }
            handler.postDelayed(this, 60_000)
        }
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            context,
            unlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            context,
            wifiReceiver,
            IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        listenSignal()
        rememberSims()
        handler.post(tick)
        hub.emit(SdkEvent("battery", "monitors_started", "device monitors on"))
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        runCatching { context.unregisterReceiver(batteryReceiver) }
        runCatching { context.unregisterReceiver(unlockReceiver) }
        runCatching { context.unregisterReceiver(wifiReceiver) }
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        handler.removeCallbacks(tick)
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyCallback?.let {
            if (Build.VERSION.SDK_INT >= 31) tm.unregisterTelephonyCallback(it)
        }
        telephonyCallback = null
    }

    @SuppressLint("MissingPermission")
    fun snapshot(): DeviceSnapshot {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        val noNet = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (noNet) {
            // emit sparingly via last flag in prefs
        }
        val ssid = connectedSsid(wifi)
        val bssid = connectedBssid(wifi)
        val ouis = matchOuis(bssid)
        val hotspot = wifiApEnabled()
        val tethered = hotspot
        val roaming = hasPhoneState() && tm.isNetworkRoaming
        val netCountry = runCatching { tm.networkCountryIso }.getOrNull()
        val simCountry = runCatching { tm.simCountryIso }.getOrNull()
        val abroad = !netCountry.isNullOrBlank() && !simCountry.isNullOrBlank() &&
            netCountry.lowercase() != simCountry.lowercase()
        val sims = sims()
        val fp = sims.joinToString { it.iccidSuffix + it.mccMnc }
        val prev = prefs.getString("sim_fp")
        val changed = prev != null && prev != fp && fp.isNotBlank()
        if (fp.isNotBlank()) prefs.putString("sim_fp", fp)
        val dbm = signalDbm ?: currentDbm(tm)
        return DeviceSnapshot(
            charging = charging(battery),
            batteryPct = batteryPct(battery),
            screenOn = screenOn(),
            hostForeground = hostForeground,
            wifiSsid = ssid,
            wifiBssid = bssid,
            matchedIspOuis = ouis,
            hotspotOn = hotspot,
            tethered = tethered,
            signalDbm = dbm,
            poorSignal = dbm != null && dbm < -110,
            roaming = roaming,
            networkCountry = netCountry,
            simCountry = simCountry,
            abroad = abroad,
            sims = sims,
            defaultDataSubId = defaultDataSub(),
            lastUnlockAt = prefs.getLong("last_unlock").takeIf { it > 0 },
            lastWifiChangeAt = prefs.getLong("last_wifi").takeIf { it > 0 },
            lastHeartbeatAt = prefs.getLong("heartbeat").takeIf { it > 0 },
            simChanged = changed,
            rootedHeuristic = security.rootedHeuristic()
        )
    }

    @SuppressLint("MissingPermission")
    private fun listenSignal() {
        if (Build.VERSION.SDK_INT < 31 || !hasPhoneState()) return
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cb = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                signalDbm = signalStrength.cellSignalStrengths.minOfOrNull { it.dbm }
            }
        }
        telephonyCallback = cb
        tm.registerTelephonyCallback(context.mainExecutor, cb)
    }

    private fun currentDbm(tm: TelephonyManager): Int? {
        if (Build.VERSION.SDK_INT < 28 || !hasPhoneState()) return null
        return runCatching { tm.signalStrength?.cellSignalStrengths?.minOfOrNull { it.dbm } }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun sims(): List<SimInfo> {
        if (!hasPhoneState()) return emptyList()
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val competitors = prefs.getStringSet("competitor_mccmnc")
        val list = sm.activeSubscriptionInfoList ?: return emptyList()
        return list.map { info ->
            val mcc = info.mccString ?: ""
            val mnc = info.mncString ?: ""
            val mccmnc = mcc + mnc
            SimInfo(
                subscriptionId = info.subscriptionId,
                slotIndex = info.simSlotIndex,
                carrierName = info.carrierName?.toString() ?: "",
                mccMnc = mccmnc,
                iccidSuffix = info.iccId.takeLast(4).ifBlank { "sub${info.subscriptionId}" },
                isCompetitor = mccmnc.isNotEmpty() && competitors.contains(mccmnc)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun defaultDataSub(): Int? {
        if (!hasPhoneState()) return null
        return SubscriptionManager.getDefaultDataSubscriptionId().takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
    }

    private fun rememberSims() {
        val fp = sims().joinToString { it.iccidSuffix + it.mccMnc }
        if (fp.isNotBlank() && prefs.getString("sim_fp") == null) prefs.putString("sim_fp", fp)
    }

    private fun connectedSsid(wifi: WifiManager): String? {
        if (!hasWifiInfo()) return null
        @Suppress("DEPRECATION")
        val ssid = wifi.connectionInfo?.ssid?.trim('"')
        return ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }

    private fun connectedBssid(wifi: WifiManager): String? {
        if (!hasWifiInfo()) return null
        @Suppress("DEPRECATION")
        return wifi.connectionInfo?.bssid?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
    }

    private fun matchOuis(bssid: String?): List<String> {
        if (bssid == null) return emptyList()
        val prefix = bssid.take(8).uppercase()
        return prefs.getStringSet("isp_ouis").filter { prefix.startsWith(it.uppercase()) }
    }

    private fun wifiApEnabled(): Boolean {
        val sticky = context.registerReceiver(null, android.content.IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"))
        val state = sticky?.getIntExtra("wifi_state", 11) ?: 11
        return state == 13 // WIFI_AP_STATE_ENABLED
    }

    private fun hasPhoneState() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasWifiInfo(): Boolean {
        val loc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val nearby = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        return loc || nearby
    }

    private fun screenOn(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isInteractive
    }

    private fun batteryPct(intent: Intent?): Int {
        if (intent == null) return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return if (scale <= 0) -1 else (level * 100) / scale
    }

    private fun charging(intent: Intent?): Boolean {
        if (intent == null) return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}
