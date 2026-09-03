package com.telco.safetysdk

enum class Feasibility {
    DEVICE_SDK,
    HOST_ROLE,
    CARRIER_ONLY,
    NOT_IN_THIRD_PARTY_SDK
}

data class Capability(
    val id: String,
    val category: String,
    val title: String,
    val requirement: String,
    val feasibility: Feasibility,
    val why: String,
    val implemented: Boolean
)
