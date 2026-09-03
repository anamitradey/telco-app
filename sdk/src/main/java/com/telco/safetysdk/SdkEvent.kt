package com.telco.safetysdk

data class SdkEvent(
    val capabilityId: String,
    val type: String,
    val message: String,
    val extras: Map<String, String> = emptyMap(),
    val atMillis: Long = System.currentTimeMillis()
)

fun interface SdkListener {
    fun onEvent(event: SdkEvent)
}
