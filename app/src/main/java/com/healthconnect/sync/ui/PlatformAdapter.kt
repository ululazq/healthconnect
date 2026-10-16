package com.healthconnect.sync.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthconnect.sync.R
import com.healthconnect.sync.data.sync.PlatformAccount
import com.healthconnect.sync.data.sync.PlatformType
import com.healthconnect.sync.databinding.ItemPlatformCardBinding

class PlatformAdapter(
    private val onToggleConnect: (account: PlatformAccount) -> Unit
) : ListAdapter<PlatformAccount, PlatformAdapter.PlatformViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlatformViewHolder {
        val binding = ItemPlatformCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlatformViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlatformViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlatformViewHolder(
        private val binding: ItemPlatformCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: PlatformAccount) {
            val context = binding.root.context
            val platform = account.platform

            binding.tvPlatformName.text = platform.displayName
            binding.ivPlatformIcon.setImageResource(getPlatformDrawable(platform))

            // Role / Capabilities
            binding.tvPlatformRole.text = when {
                platform == PlatformType.HUAWEI_HEALTH -> "Master Hub & Primary Source"
                platform.isSourceSupported && platform.isDestinationSupported -> "Source & Destination Supported"
                platform.isDestinationSupported -> "Destination Only"
                else -> "Source Only"
            }

            // Connection state
            if (account.isConnected) {
                binding.tvPlatformStatusBadge.text = "Connected"
                binding.tvPlatformStatusBadge.setBackgroundResource(R.drawable.bg_pill_health_connect)
                binding.tvPlatformStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_connected_text))

                binding.tvAccountIdentifier.text = "Account: ${account.accountIdentifier ?: "Active Session"}"
                binding.tvLastSyncTime.text = "Authorized for synchronization"

                if (platform == PlatformType.HUAWEI_HEALTH) {
                    binding.btnConnectPlatform.text = "Active Hub"
                    binding.btnConnectPlatform.isEnabled = false
                } else {
                    binding.btnConnectPlatform.text = "Disconnect"
                    binding.btnConnectPlatform.isEnabled = true
                }
            } else {
                binding.tvPlatformStatusBadge.text = "Not Linked"
                binding.tvPlatformStatusBadge.setBackgroundResource(R.drawable.bg_pill_neutral)
                binding.tvPlatformStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.on_surface_secondary))

                binding.tvAccountIdentifier.text = "Account: Not configured"
                binding.tvLastSyncTime.text = "Tap Connect to link authorization"

                binding.btnConnectPlatform.text = "Connect"
                binding.btnConnectPlatform.isEnabled = true
            }

            binding.btnConnectPlatform.setOnClickListener {
                onToggleConnect(account)
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

    companion object DiffCallback : DiffUtil.ItemCallback<PlatformAccount>() {
        override fun areItemsTheSame(oldItem: PlatformAccount, newItem: PlatformAccount): Boolean {
            return oldItem.platform == newItem.platform
        }

        override fun areContentsTheSame(oldItem: PlatformAccount, newItem: PlatformAccount): Boolean {
            return oldItem == newItem
        }
    }
}
