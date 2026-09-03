package com.telco.safetysdk.sample

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telco.safetysdk.Capability
import com.telco.safetysdk.Feasibility
import com.telco.safetysdk.SafetySdk
import com.telco.safetysdk.sample.databinding.FragmentListBinding

class FeasibilityFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentListBinding.inflate(inflater, container, false)
        val caps = SafetySdk.get(requireContext()).capabilities()
        val impl = caps.count { it.implemented }
        binding.summary.text =
            "$impl/${caps.size} implemented in this third-party SDK. " +
                "Carrier-only and Play-restricted items stay listed so you do not ship fake controls."
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = CapAdapter(caps)
        return binding.root
    }
}

class CapAdapter(private val items: List<Capability>) : RecyclerView.Adapter<CapAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val status: TextView = v.findViewById(R.id.status)
        val why: TextView = v.findViewById(R.id.why)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_capability, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.title.text = "${c.category}: ${c.title}"
        val label = when {
            c.implemented -> "IMPLEMENTED · ${c.feasibility}"
            c.feasibility == Feasibility.CARRIER_ONLY -> "NOT IN SDK · CARRIER API"
            c.feasibility == Feasibility.HOST_ROLE -> "NOT ENFORCEABLE HERE · HOST ROLE"
            else -> "NOT IN THIRD-PARTY SDK"
        }
        holder.status.text = label
        holder.status.setTextColor(
            when {
                c.implemented -> Color.parseColor("#1B7F3A")
                c.feasibility == Feasibility.CARRIER_ONLY -> Color.parseColor("#B00020")
                else -> Color.parseColor("#C43E00")
            }
        )
        holder.why.text = c.why
    }
}
