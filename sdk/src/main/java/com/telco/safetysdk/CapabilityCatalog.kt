package com.telco.safetysdk

/**
 * What a Play-distributed third-party SDK can do with public Android APIs.
 * Carrier-privilege / default-dialer / Device-Owner features stay catalogued, not faked.
 */
object CapabilityCatalog {

    fun all(): List<Capability> = listOf(
        cap(
            "hotspot", "Device signals", "Hotspot / tethering detection",
            "Monitor tethering + SIM usage patterns",
            Feasibility.DEVICE_SDK,
            "Can detect Wi-Fi AP / USB tethering via ConnectivityManager and WifiManager. " +
                "Cannot see which other devices are on the hotspot, or map that to a competitor SIM on another phone.",
            implemented = true
        ),
        cap(
            "wifi_fingerprint", "Device signals", "Wi-Fi SSID fingerprinting",
            "Scan SSID + router MAC OUI for provider identification",
            Feasibility.DEVICE_SDK,
            "SSID/BSSID of the connected network and throttled scans are possible with location + NEARBY_WIFI_DEVICES. " +
                "Device MAC is randomized; OUI matching is a host-supplied prefix list, not a hidden ISP database. " +
                "Play policy requires a disclosed, consented purpose — not silent competitor-home profiling.",
            implemented = true
        ),
        cap(
            "dual_sim", "Device signals", "Dual SIM analysis",
            "SubscriptionManager SIM 1 vs SIM 2 data attribution",
            Feasibility.DEVICE_SDK,
            "Active subscriptions, MCC/MNC, carrier name, default data SIM: yes with READ_PHONE_STATE. " +
                "Per-SIM traffic bytes need PACKAGE_USAGE_STATS (settings toggle), not a silent SDK hook.",
            implemented = true
        ),
        cap(
            "screen_unlock", "Device signals", "Screen unlock (ACTION_USER_PRESENT)",
            "Broadcast on unlock only",
            Feasibility.DEVICE_SDK,
            "Dynamic BroadcastReceiver works while the host process is alive. Manifest receivers for this action are unreliable on modern Android.",
            implemented = true
        ),
        cap(
            "battery", "Device signals", "Battery and charging",
            "BatteryManager; rich media when charging + idle",
            Feasibility.DEVICE_SDK,
            "Sticky ACTION_BATTERY_CHANGED needs no runtime permission. Idle is inferred from screen-off + charging.",
            implemented = true
        ),
        cap(
            "app_foreground", "Device signals", "App foreground",
            "ActivityLifecycleCallbacks; in-app nudge at peak attention",
            Feasibility.DEVICE_SDK,
            "Lifecycle callbacks only see the host app. Other apps need UsageStatsManager + the special usage-access grant.",
            implemented = true
        ),
        cap(
            "signal", "Device signals", "Signal strength",
            "TelephonyManager polling; suppress on poor signal",
            Feasibility.DEVICE_SDK,
            "TelephonyCallback.SignalStrengthsListener works with READ_PHONE_STATE.",
            implemented = true
        ),
        cap(
            "safety_circle", "Safety Circle & SOS", "Safety Circle members (consent)",
            "Add family member with consent",
            Feasibility.DEVICE_SDK,
            "Local membership + consent flag. Pushing alerts to another phone needs the host's backend.",
            implemented = true
        ),
        cap(
            "location_unreachable", "Safety Circle & SOS", "Unreachable / no-network zone",
            "Location — unreachable / no-network",
            Feasibility.DEVICE_SDK,
            "Detects no validated internet + last known location. 'Unreachable' while the phone is off requires a server missed-heartbeat, not this process.",
            implemented = true
        ),
        cap(
            "battery_off", "Safety Circle & SOS", "Battery low / phone off",
            "Battery low / phone switched off",
            Feasibility.DEVICE_SDK,
            "Battery-low is local. Power-off cannot be observed on the powered-off device; expose lastHeartbeatAt for the host to treat as stale.",
            implemented = true
        ),
        cap(
            "no_activity", "Safety Circle & SOS", "No activity",
            "No unlock or Wi-Fi activity",
            Feasibility.DEVICE_SDK,
            "Tracks last unlock and last Wi-Fi change in-process / prefs.",
            implemented = true
        ),
        cap(
            "broken_routine", "Safety Circle & SOS", "Broken routine",
            "No unlock + no Wi-Fi + no STB + no wearable",
            Feasibility.DEVICE_SDK,
            "Unlock + Wi-Fi inactivity: yes. Set-top box and wearable motion are not available to a phone SDK unless those devices talk to the host app.",
            implemented = true
        ),
        cap(
            "network_location", "Safety Circle & SOS", "Network-signal location",
            "Cell/Wi-Fi location in addition to GPS, consent-based",
            Feasibility.DEVICE_SDK,
            "Fused/system location already blends GPS + network. Cell-ID to lat/lng without Play/location APIs is not exposed to third-party apps.",
            implemented = true
        ),
        cap(
            "abroad", "Safety Circle & SOS", "Landing abroad",
            "Automatic update to family on landing abroad",
            Feasibility.DEVICE_SDK,
            "Compares SIM country vs network country / roaming. Notifying family is a host callback.",
            implemented = true
        ),
        cap(
            "sos", "Safety Circle & SOS", "Emergency SOS",
            "Child sends SOS with live location; linked to parents",
            Feasibility.DEVICE_SDK,
            "User-initiated SOS with location + member list. System power-button SOS is OEM and not injectable.",
            implemented = true
        ),
        cap(
            "live_location", "Parental Control", "Location tracking",
            "Parent views child's live location",
            Feasibility.DEVICE_SDK,
            "Child device can publish location. Parent view is a host UI + transport, not a hidden OS API.",
            implemented = true
        ),
        cap(
            "geofence", "Parental Control", "Geofencing",
            "Alert on entering/leaving school, home, Safe Routes deviation",
            Feasibility.DEVICE_SDK,
            "In-process geofences + route corridor check. OS GeofencingClient needs Play Services; this SDK stays dependency-free with a location loop while monitoring.",
            implemented = true
        ),
        cap(
            "sim_change", "Child/Elder Protect", "SIM swap / porting notice",
            "Parental approval for SIM swap / number porting",
            Feasibility.DEVICE_SDK,
            "Can detect ICCID/subscription change and raise an alert. Cannot approve or block a carrier port — that is BSS/MNO.",
            implemented = true
        ),
        cap(
            "spam_block", "Child/Elder Protect", "Block high-risk spam callers",
            "Block high-risk SPAM callers",
            Feasibility.HOST_ROLE,
            "CallScreeningService only if the host holds ROLE_CALL_SCREENING or is the default dialer. A library cannot grant that role.",
            implemented = false
        ),
        cap(
            "trusted_contacts", "Parental Control", "Trusted contacts",
            "Calls/SMS only from approved contacts",
            Feasibility.HOST_ROLE,
            "Store the allow-list here. Enforcing it requires default-dialer / SMS role or carrier IMS.",
            implemented = true
        ),
        cap(
            "fraud_otp", "Fraud Detection", "Repeated bank OTPs / long unknown calls",
            "Repeated bank OTPs / long-duration unknown calls",
            Feasibility.NOT_IN_THIRD_PARTY_SDK,
            "RECEIVE_SMS and READ_CALL_LOG are Play-restricted for ordinary apps/SDKs. Default SMS/dialer only.",
            implemented = false
        ),
        cap(
            "fraud_combo", "Fraud Detection", "Fraud exposure combo",
            "Long call + OTP + payment + remote-access/app-install",
            Feasibility.NOT_IN_THIRD_PARTY_SDK,
            "Same SMS/call-log policy. Remote-access detection via Accessibility is Play-restricted. PACKAGE_ADDED is limited.",
            implemented = false
        ),
        cap(
            "content_filter", "Child/Elder Protect", "Content filtering / Safe Browsing",
            "Block adult/phishing/malware sites",
            Feasibility.HOST_ROLE,
            "Needs VpnService (host) or a DNS/proxy the user installs. Not silent SDK traffic inspection.",
            implemented = true
        ),
        cap(
            "internet_pause", "Parental Control", "Internet Pause / Study / Bedtime / screen-time",
            "Pause internet, study mode, bedtime, daily data cap",
            Feasibility.HOST_ROLE,
            "Local policy engine is implemented. Enforcing 'no internet' uses the sample VpnService. True app-category blocking of other packages needs Device Owner / MDM.",
            implemented = true
        ),
        cap(
            "app_category", "Parental Control", "App category control",
            "Restrict gaming/gambling/dating apps",
            Feasibility.HOST_ROLE,
            "QUERY_ALL_PACKAGES is Play-restricted. Device Owner can hide apps. A third-party SDK can only classify packages the host can see.",
            implemented = false
        ),
        cap(
            "premium_block", "Parental Control", "Premium service blocking",
            "International calls, VAS subscriptions",
            Feasibility.CARRIER_ONLY,
            "Call barring / VAS is on the operator BSS. The device can only detect roaming and warn.",
            implemented = false
        ),
        cap(
            "roaming", "Parental Control", "Roaming control",
            "Prevent accidental roaming charges",
            Feasibility.DEVICE_SDK,
            "Detect roaming and fire a policy event. Cannot disable the radio's roaming attach without carrier / privileged APIs.",
            implemented = true
        ),
        cap(
            "device_security", "Parental Control", "Device security",
            "Root, malicious APKs, unsafe config",
            Feasibility.DEVICE_SDK,
            "Root/emulator/adb heuristics: yes. Full malware scan of arbitrary APKs is not a phone SDK job.",
            implemented = true
        ),
        cap(
            "family_dashboard", "Parental Control", "Family dashboard",
            "Manage multiple child SIMs from one parent account",
            Feasibility.DEVICE_SDK,
            "Local multi-profile model. Account sync and SIM provisioning are host/MNO.",
            implemented = true
        ),
        cap(
            "wellbeing", "Parental Control", "Digital wellbeing report",
            "Weekly data usage, online time, safety events",
            Feasibility.DEVICE_SDK,
            "SDK event log + TrafficStats for this UID. Cross-app screen time needs usage-access.",
            implemented = true
        ),
        cap(
            "driving", "Parental Control", "Teen driving mode",
            "Silence notifications, auto-reply while driving",
            Feasibility.DEVICE_SDK,
            "Speed-from-location heuristic (no Play Services). Notification interruption needs PolicyAccess / DND permission. Auto-SMS needs SMS role.",
            implemented = true
        )
    )

    private fun cap(
        id: String,
        category: String,
        title: String,
        requirement: String,
        feasibility: Feasibility,
        why: String,
        implemented: Boolean
    ) = Capability(id, category, title, requirement, feasibility, why, implemented)
}
