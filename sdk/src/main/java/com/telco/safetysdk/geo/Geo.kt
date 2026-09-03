package com.telco.safetysdk.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    fun distanceToSegment(
        lat: Double,
        lon: Double,
        aLat: Double,
        aLon: Double,
        bLat: Double,
        bLon: Double
    ): Double {
        val dAB = haversineMeters(aLat, aLon, bLat, bLon)
        if (dAB < 1) return haversineMeters(lat, lon, aLat, aLon)
        val dA = haversineMeters(lat, lon, aLat, aLon)
        val dB = haversineMeters(lat, lon, bLat, bLon)
        if (dA * dA > dAB * dAB + dB * dB) return dB
        if (dB * dB > dAB * dAB + dA * dA) return dA
        val s = (dAB + dA + dB) / 2
        val area = sqrt((s * (s - dAB) * (s - dA) * (s - dB)).coerceAtLeast(0.0))
        return 2 * area / dAB
    }

    fun insideZone(lat: Double, lon: Double, zLat: Double, zLon: Double, radius: Float) =
        haversineMeters(lat, lon, zLat, zLon) <= radius

    fun offRoute(lat: Double, lon: Double, points: List<Pair<Double, Double>>, corridor: Float): Boolean {
        if (points.size < 2) return false
        var min = Double.MAX_VALUE
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            min = minOf(min, distanceToSegment(lat, lon, a.first, a.second, b.first, b.second))
        }
        return min > corridor
    }
}
