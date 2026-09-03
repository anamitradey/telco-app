package com.telco.safetysdk.safety

import com.telco.safetysdk.SdkEvent
import com.telco.safetysdk.internal.EventHub
import com.telco.safetysdk.internal.Prefs
import org.json.JSONArray
import org.json.JSONObject

data class CircleMember(
    val id: String,
    val name: String,
    val phone: String,
    val consented: Boolean
)

data class GeoZone(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float
)

data class SafeRoute(
    val id: String,
    val name: String,
    val points: List<Pair<Double, Double>>,
    val corridorMeters: Float
)

class SafetyStore(private val prefs: Prefs, private val hub: EventHub) {

    fun members(): List<CircleMember> = readArray("members") { o ->
        CircleMember(
            o.getString("id"),
            o.getString("name"),
            o.getString("phone"),
            o.optBoolean("consented", false)
        )
    }

    fun addMember(member: CircleMember) {
        val next = members().filterNot { it.id == member.id } + member
        writeMembers(next)
        hub.emit(SdkEvent("safety_circle", "member_added", "${member.name} (consent=${member.consented})"))
    }

    fun removeMember(id: String) {
        writeMembers(members().filterNot { it.id == id })
    }

    fun geofences(): List<GeoZone> = readArray("geofences") { o ->
        GeoZone(
            o.getString("id"),
            o.getString("name"),
            o.getDouble("lat"),
            o.getDouble("lng"),
            o.getDouble("radius").toFloat()
        )
    }

    fun addGeofence(zone: GeoZone) {
        writeGeofences(geofences().filterNot { it.id == zone.id } + zone)
        hub.emit(SdkEvent("geofence", "zone_added", zone.name))
    }

    fun removeGeofence(id: String) {
        writeGeofences(geofences().filterNot { it.id == id })
    }

    fun routes(): List<SafeRoute> = readArray("routes") { o ->
        val pts = o.getJSONArray("points")
        val points = (0 until pts.length()).map { i ->
            val p = pts.getJSONObject(i)
            p.getDouble("lat") to p.getDouble("lng")
        }
        SafeRoute(o.getString("id"), o.getString("name"), points, o.getDouble("corridor").toFloat())
    }

    fun addRoute(route: SafeRoute) {
        val arr = JSONArray()
        (routes().filterNot { it.id == route.id } + route).forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("corridor", r.corridorMeters.toDouble())
                put("points", JSONArray().also { pa ->
                    r.points.forEach { (lat, lng) ->
                        pa.put(JSONObject().put("lat", lat).put("lng", lng))
                    }
                })
            })
        }
        prefs.putString("routes", arr.toString())
        hub.emit(SdkEvent("geofence", "route_added", route.name))
    }

    fun trustedNumbers(): Set<String> = prefs.getStringSet("trusted")

    fun setTrustedNumbers(numbers: Set<String>) {
        prefs.putStringSet("trusted", numbers)
        hub.emit(SdkEvent("trusted_contacts", "updated", "${numbers.size} numbers"))
    }

    fun competitorMccMnc(): Set<String> = prefs.getStringSet("competitor_mccmnc")

    fun setCompetitorMccMnc(ids: Set<String>) = prefs.putStringSet("competitor_mccmnc", ids)

    fun ispOuis(): Set<String> = prefs.getStringSet("isp_ouis")

    fun setIspOuis(ouis: Set<String>) = prefs.putStringSet("isp_ouis", ouis)

    private fun writeMembers(list: List<CircleMember>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().put("id", m.id).put("name", m.name).put("phone", m.phone).put("consented", m.consented))
        }
        prefs.putString("members", arr.toString())
    }

    private fun writeGeofences(list: List<GeoZone>) {
        val arr = JSONArray()
        list.forEach { z ->
            arr.put(
                JSONObject()
                    .put("id", z.id)
                    .put("name", z.name)
                    .put("lat", z.lat)
                    .put("lng", z.lng)
                    .put("radius", z.radiusMeters.toDouble())
            )
        }
        prefs.putString("geofences", arr.toString())
    }

    private fun <T> readArray(key: String, map: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { map(arr.getJSONObject(it)) }
    }
}
