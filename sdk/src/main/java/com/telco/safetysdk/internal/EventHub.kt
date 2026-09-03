package com.telco.safetysdk.internal

import com.telco.safetysdk.SdkEvent
import com.telco.safetysdk.SdkListener
import java.util.concurrent.CopyOnWriteArrayList

class EventHub {
    private val listeners = CopyOnWriteArrayList<SdkListener>()
    private val buffer = ArrayDeque<SdkEvent>()

    fun add(listener: SdkListener) { listeners.add(listener) }

    fun remove(listener: SdkListener) { listeners.remove(listener) }

    fun emit(event: SdkEvent) {
        synchronized(buffer) {
            buffer.addLast(event)
            while (buffer.size > 200) buffer.removeFirst()
        }
        listeners.forEach { it.onEvent(event) }
    }

    fun recent(): List<SdkEvent> = synchronized(buffer) { buffer.toList() }
}
