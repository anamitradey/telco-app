package com.telco.safetysdk.security

import android.content.Context
import android.os.Build
import java.io.File

class DeviceSecurity(private val context: Context) {

    fun rootedHeuristic(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        if (paths.any { File(it).exists() }) return true
        return Build.TAGS?.contains("test-keys") == true
    }

    fun adbEnabled(): Boolean {
        return android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.ADB_ENABLED,
            0
        ) == 1
    }

    fun summary(): String {
        return "rootHeuristic=${rootedHeuristic()} adb=${adbEnabled()} emulator=${isEmulator()}"
    }

    fun isEmulator(): Boolean {
        val f = Build.FINGERPRINT
        return f.startsWith("generic") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
    }
}
