package com.telco.safetysdk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.telco.safetysdk.SafetySdk
import com.telco.safetysdk.sample.databinding.FragmentSignalsBinding

class SignalsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSignalsBinding.inflate(inflater, container, false)
        val sdk = SafetySdk.get(requireContext())
        val act = requireActivity() as MainActivity
        binding.btnPerms.setOnClickListener { act.requestSdkPermissions() }
        binding.btnStart.setOnClickListener {
            sdk.startDeviceMonitors()
            binding.snapshot.text = format(sdk)
        }
        binding.btnStop.setOnClickListener { sdk.stopDeviceMonitors() }
        binding.snapshot.text = format(sdk)
        return binding.root
    }

    private fun format(sdk: SafetySdk): String {
        val s = sdk.snapshot()
        val sims = s.sims.joinToString("\n") {
            "  slot ${it.slotIndex} ${it.carrierName} ${it.mccMnc} iccid…${it.iccidSuffix} competitor=${it.isCompetitor}"
        }
        return """
            battery=${s.batteryPct}% charging=${s.charging} screenOn=${s.screenOn} hostFg=${s.hostForeground}
            wifi=${s.wifiSsid} bssid=${s.wifiBssid} ispOuis=${s.matchedIspOuis}
            hotspot=${s.hotspotOn} signal=${s.signalDbm}dBm poor=${s.poorSignal}
            roaming=${s.roaming} abroad=${s.abroad} net=${s.networkCountry} sim=${s.simCountry}
            dataSub=${s.defaultDataSubId} simChanged=${s.simChanged} rootHeuristic=${s.rootedHeuristic}
            lastUnlock=${s.lastUnlockAt} lastWifi=${s.lastWifiChangeAt} heartbeat=${s.lastHeartbeatAt}
            security=${sdk.security.summary()}
            SIMs:
            $sims
        """.trimIndent()
    }
}
