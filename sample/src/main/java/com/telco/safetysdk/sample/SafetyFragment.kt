package com.telco.safetysdk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.telco.safetysdk.SafetySdk
import com.telco.safetysdk.safety.CircleMember
import com.telco.safetysdk.safety.GeoZone
import com.telco.safetysdk.sample.databinding.FragmentSafetyBinding
import java.util.UUID

class SafetyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSafetyBinding.inflate(inflater, container, false)
        val sdk = SafetySdk.get(requireContext())
        fun refresh() {
            binding.safetyState.text =
                "members=${sdk.safety.members()}\n" +
                    "fences=${sdk.safety.geofences()}\n" +
                    "trusted=${sdk.safety.trustedNumbers()}\n" +
                    "lastFix=${sdk.lastLocation()}\n" +
                    "events=${sdk.wellbeingEvents().size}"
        }
        binding.btnLoc.setOnClickListener { sdk.startLocationMonitoring() }
        binding.btnSos.setOnClickListener { sdk.triggerSos("user SOS") }
        binding.btnMember.setOnClickListener {
            val name = binding.memberName.text?.toString().orEmpty().ifBlank { return@setOnClickListener }
            val phone = binding.memberPhone.text?.toString().orEmpty()
            sdk.safety.addMember(CircleMember(UUID.randomUUID().toString(), name, phone, consented = true))
            refresh()
        }
        binding.btnFence.setOnClickListener {
            val loc = sdk.lastLocation() ?: return@setOnClickListener
            val name = binding.zoneName.text?.toString().orEmpty().ifBlank { "Home" }
            sdk.safety.addGeofence(GeoZone(UUID.randomUUID().toString(), name, loc.first, loc.second, 200f))
            refresh()
        }
        binding.btnTrusted.setOnClickListener {
            val set = binding.trusted.text?.toString().orEmpty()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            sdk.safety.setTrustedNumbers(set)
            refresh()
        }
        refresh()
        return binding.root
    }
}
