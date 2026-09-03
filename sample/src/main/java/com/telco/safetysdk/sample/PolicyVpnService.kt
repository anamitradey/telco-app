package com.telco.safetysdk.sample

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.telco.safetysdk.SafetySdk
import java.util.Calendar

class PolicyVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        val sdk = SafetySdk.get(this)
        val cal = Calendar.getInstance()
        val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (sdk.parental.shouldBlockInternet(mins, driving = false)) {
            if (tun == null) {
                tun = Builder()
                    .setSession("Safety pause")
                    .addAddress("10.8.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .setMtu(1500)
                    .establish()
            }
        } else {
            tun?.close()
            tun = null
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tun?.close()
        tun = null
        super.onDestroy()
    }
}
