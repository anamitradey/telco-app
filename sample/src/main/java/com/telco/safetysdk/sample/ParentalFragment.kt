package com.telco.safetysdk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.telco.safetysdk.SafetySdk
import com.telco.safetysdk.parental.ChildProfile
import com.telco.safetysdk.sample.databinding.FragmentParentalBinding
import java.util.UUID

class ParentalFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentParentalBinding.inflate(inflater, container, false)
        val sdk = SafetySdk.get(requireContext())
        val act = requireActivity() as MainActivity
        fun refresh() {
            binding.swPause.isChecked = sdk.parental.internetPause
            binding.swStudy.isChecked = sdk.parental.studyMode
            binding.swBed.isChecked = sdk.parental.bedtimeMode
            binding.capMb.setText(sdk.parental.dailyLimitMb.toString())
            binding.parentalState.text =
                "profiles=${sdk.parental.profiles()}\n" +
                    "blockHosts=${sdk.parental.blockListHosts}\n" +
                    "usedMb=${sdk.parental.usedMbToday} cap=${sdk.parental.dailyLimitMb}"
        }
        binding.swPause.setOnCheckedChangeListener { _, v -> sdk.parental.internetPause = v }
        binding.swStudy.setOnCheckedChangeListener { _, v -> sdk.parental.studyMode = v }
        binding.swBed.setOnCheckedChangeListener { _, v -> sdk.parental.bedtimeMode = v }
        binding.btnCap.setOnClickListener {
            sdk.parental.dailyLimitMb = binding.capMb.text?.toString()?.toIntOrNull() ?: 0
            refresh()
        }
        binding.btnVpn.setOnClickListener { act.prepareVpn() }
        binding.btnChild.setOnClickListener {
            val name = binding.childName.text?.toString().orEmpty().ifBlank { return@setOnClickListener }
            sdk.parental.upsertProfile(ChildProfile(UUID.randomUUID().toString(), name, "SIM"))
            refresh()
        }
        binding.btnMcc.setOnClickListener {
            val ids = binding.mccmnc.text?.toString().orEmpty()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            sdk.safety.setCompetitorMccMnc(ids)
            refresh()
        }
        refresh()
        return binding.root
    }
}
