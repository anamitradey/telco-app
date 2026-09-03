package com.telco.safetysdk.parental

import com.telco.safetysdk.SdkEvent
import com.telco.safetysdk.internal.EventHub
import com.telco.safetysdk.internal.Prefs

data class ChildProfile(val id: String, val name: String, val simLabel: String)

class ParentalPolicy(private val prefs: Prefs, private val hub: EventHub) {

    var internetPause: Boolean
        get() = prefs.getBool("pause")
        set(value) {
            prefs.putBool("pause", value)
            hub.emit(SdkEvent("internet_pause", "pause", value.toString()))
        }

    var studyMode: Boolean
        get() = prefs.getBool("study")
        set(value) {
            prefs.putBool("study", value)
            hub.emit(SdkEvent("internet_pause", "study", value.toString()))
        }

    var bedtimeMode: Boolean
        get() = prefs.getBool("bedtime")
        set(value) {
            prefs.putBool("bedtime", value)
            hub.emit(SdkEvent("internet_pause", "bedtime", value.toString()))
        }

    var dailyLimitMb: Int
        get() = prefs.getInt("limit_mb", 0)
        set(value) = prefs.putInt("limit_mb", value)

    var usedMbToday: Int
        get() = prefs.getInt("used_mb", 0)
        set(value) = prefs.putInt("used_mb", value)

    var studyStartMin: Int
        get() = prefs.getInt("study_start", 16 * 60)
        set(value) = prefs.putInt("study_start", value)

    var studyEndMin: Int
        get() = prefs.getInt("study_end", 18 * 60)
        set(value) = prefs.putInt("study_end", value)

    var bedtimeStartMin: Int
        get() = prefs.getInt("bed_start", 22 * 60)
        set(value) = prefs.putInt("bed_start", value)

    var bedtimeEndMin: Int
        get() = prefs.getInt("bed_end", 6 * 60)
        set(value) = prefs.putInt("bed_end", value)

    var blockListHosts: Set<String>
        get() = prefs.getStringSet("block_hosts").ifEmpty {
            setOf("example-malware.test", "adult.example")
        }
        set(value) = prefs.putStringSet("block_hosts", value)

    fun profiles(): List<ChildProfile> {
        val raw = prefs.getString("profiles") ?: return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }.map {
            val p = it.split("|")
            ChildProfile(p[0], p.getOrElse(1) { p[0] }, p.getOrElse(2) { "" })
        }
    }

    fun upsertProfile(profile: ChildProfile) {
        val next = profiles().filterNot { it.id == profile.id } + profile
        prefs.putString("profiles", next.joinToString("\n") { "${it.id}|${it.name}|${it.simLabel}" })
        hub.emit(SdkEvent("family_dashboard", "profile", profile.name))
    }

    fun shouldBlockInternet(nowMinutes: Int, driving: Boolean): Boolean {
        if (internetPause) return true
        if (dailyLimitMb > 0 && usedMbToday >= dailyLimitMb) return true
        if (studyMode && inWindow(nowMinutes, studyStartMin, studyEndMin)) return true
        if (bedtimeMode && inWindow(nowMinutes, bedtimeStartMin, bedtimeEndMin)) return true
        if (driving && prefs.getBool("driving_block_data", false)) return true
        return false
    }

    fun shouldSilenceNotifications(driving: Boolean) = driving

    fun hostBlocked(host: String): Boolean {
        val h = host.lowercase()
        return blockListHosts.any { h == it || h.endsWith(".$it") }
    }

    companion object {
        fun inWindow(now: Int, start: Int, end: Int): Boolean {
            return if (start <= end) now in start until end else now >= start || now < end
        }
    }
}
