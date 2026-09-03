package com.telco.safetysdk

import com.telco.safetysdk.geo.Geo
import com.telco.safetysdk.parental.ParentalPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyLogicTest {
    @Test
    fun bedtimeWrapsMidnight() {
        assertTrue(ParentalPolicy.inWindow(23 * 60, 22 * 60, 6 * 60))
        assertTrue(ParentalPolicy.inWindow(1 * 60, 22 * 60, 6 * 60))
        assertFalse(ParentalPolicy.inWindow(12 * 60, 22 * 60, 6 * 60))
    }

    @Test
    fun studyWindow() {
        assertTrue(ParentalPolicy.inWindow(17 * 60, 16 * 60, 18 * 60))
        assertFalse(ParentalPolicy.inWindow(15 * 60, 16 * 60, 18 * 60))
    }

    @Test
    fun geofenceAndRoute() {
        assertTrue(Geo.insideZone(0.0, 0.0, 0.0, 0.0, 50f))
        assertFalse(Geo.insideZone(0.01, 0.0, 0.0, 0.0, 50f))
        val route = listOf(0.0 to 0.0, 0.0 to 0.001)
        assertFalse(Geo.offRoute(0.0, 0.0004, route, 80f))
        assertTrue(Geo.offRoute(0.02, 0.0, route, 80f))
    }

    @Test
    fun catalogMarksRestrictedItems() {
        val all = CapabilityCatalog.all()
        assertEquals(false, all.first { it.id == "fraud_otp" }.implemented)
        assertEquals(true, all.first { it.id == "sos" }.implemented)
        assertEquals(Feasibility.CARRIER_ONLY, all.first { it.id == "premium_block" }.feasibility)
    }
}
