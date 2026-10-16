package com.healthconnect.sync.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthconnect.sync.R
import com.healthconnect.sync.data.sync.PlatformType
import com.healthconnect.sync.data.sync.SyncPairing
import com.healthconnect.sync.databinding.ItemSyncRouteCardBinding

class SyncRouteAdapter(
    private val onToggle: (pairing: SyncPairing, isEnabled: Boolean) -> Unit,
    private val onDelete: (pairing: SyncPairing) -> Unit
) : ListAdapter<SyncPairing, SyncRouteAdapter.RouteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemSyncRouteCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RouteViewHolder(
        private val binding: ItemSyncRouteCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pairing: SyncPairing) {
            val context = binding.root.context

            // Title
            binding.tvRouteTitle.text = "${pairing.source.displayName} → ${pairing.destination.displayName}"

            // Icons
            binding.ivRouteSourceIcon.setImageResource(getPlatformDrawable(pairing.source))
            binding.ivRouteDestIcon.setImageResource(getPlatformDrawable(pairing.destination))

            // Switch
            binding.switchRouteEnable.setOnCheckedChangeListener(null)
            binding.switchRouteEnable.isChecked = pairing.isEnabled
            binding.switchRouteEnable.setOnCheckedChangeListener { _, isChecked ->
                onToggle(pairing, isChecked)
            }

            // Status
            if (pairing.isEnabled) {
                binding.tvRouteStatus.text = pairing.lastStatus
                binding.tvRouteStatus.setTextColor(ContextCompat.getColor(context, R.color.status_connected_text))
            } else {
                binding.tvRouteStatus.text = "Paused by user"
                binding.tvRouteStatus.setTextColor(ContextCompat.getColor(context, R.color.on_surface_secondary))
            }

            // Data types description
            val typesList = pairing.enabledDataTypes.map { it.displayName }
            binding.tvRouteDataTypes.text = if (typesList.isNotEmpty()) {
                "Syncs: " + typesList.joinToString(", ")
            } else {
                "No data streams selected"
            }

            // Count
            binding.tvRouteCount.text = "Transferred: ${pairing.totalRecordsSynced} records"

            // Allow deleting custom routes or any route
            val isCustomRoute = pairing.id.startsWith("pair_") &&
                    pairing.id != "pair_huawei_to_strava" &&
                    pairing.id != "pair_huawei_to_health_connect"

            if (isCustomRoute) {
                binding.btnDeleteRoute.visibility = View.VISIBLE
                binding.btnDeleteRoute.setOnClickListener {
                    onDelete(pairing)
                }
            } else {
                binding.btnDeleteRoute.visibility = View.GONE
            }
        }

        private fun getPlatformDrawable(platform: PlatformType): Int {
            return when (platform) {
                PlatformType.HUAWEI_HEALTH -> R.drawable.ic_huawei
                PlatformType.HEALTH_CONNECT -> R.drawable.ic_health_connect
                PlatformType.STRAVA -> R.drawable.ic_strava
                PlatformType.GARMIN -> R.drawable.ic_garmin
                PlatformType.SAMSUNG_HEALTH -> R.drawable.ic_samsung
                PlatformType.FITBIT -> R.drawable.ic_fitbit
                PlatformType.GOOGLE_FIT -> R.drawable.ic_google_fit
                PlatformType.POLAR -> R.drawable.ic_polar
                PlatformType.SUUNTO -> R.drawable.ic_run
                PlatformType.OURA -> R.drawable.ic_oura
                PlatformType.COROS -> R.drawable.ic_coros
                PlatformType.WITHINGS -> R.drawable.ic_withings
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SyncPairing>() {
        override fun areItemsTheSame(oldItem: SyncPairing, newItem: SyncPairing): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SyncPairing, newItem: SyncPairing): Boolean {
            return oldItem == newItem
        }
    }
}
