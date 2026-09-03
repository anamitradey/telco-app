package com.telco.safetysdk.sample

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.telco.safetysdk.SafetySdk
import com.telco.safetysdk.SdkListener
import com.telco.safetysdk.sample.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val sdk by lazy { SafetySdk.get(this) }
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        Toast.makeText(this, "permissions: $granted", Toast.LENGTH_SHORT).show()
    }

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startService(Intent(this, PolicyVpnService::class.java))
        }
    }

    private val listener = SdkListener { event ->
        runOnUiThread {
            val line = "${fmt.format(Date(event.atMillis))} [${event.capabilityId}] ${event.type}: ${event.message}\n"
            binding.eventLog.append(line)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.eventLog.movementMethod = ScrollingMovementMethod()
        sdk.addListener(listener)
        val tabs = listOf("Feasibility", "Signals", "Safety", "Parental")
        tabs.forEach { binding.tabs.addTab(binding.tabs.newTab().setText(it)) }
        show(FeasibilityFragment())
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                show(
                    when (tab.position) {
                        1 -> SignalsFragment()
                        2 -> SafetyFragment()
                        3 -> ParentalFragment()
                        else -> FeasibilityFragment()
                    }
                )
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onDestroy() {
        sdk.removeListener(listener)
        super.onDestroy()
    }

    fun requestSdkPermissions() {
        val perms = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) {
            perms += Manifest.permission.NEARBY_WIFI_DEVICES
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        permLauncher.launch(perms.toTypedArray())
    }

    fun prepareVpn() {
        val i = VpnService.prepare(this)
        if (i != null) vpnLauncher.launch(i)
        else startService(Intent(this, PolicyVpnService::class.java))
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
