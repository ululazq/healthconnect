package com.healthconnect.sync.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthconnect.sync.R
import com.healthconnect.sync.data.WorkoutTelemetry
import com.healthconnect.sync.databinding.ItemWorkoutActivityBinding
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ActivityFeedAdapter : ListAdapter<WorkoutTelemetry, ActivityFeedAdapter.ActivityViewHolder>(DiffCallback) {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemWorkoutActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(
        private val binding: ItemWorkoutActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: WorkoutTelemetry) {
            binding.tvWorkoutTitle.text = workout.title
            binding.tvSourceBadge.text = workout.source

            // Format date & time
            val localDateTime = workout.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val activityDate = localDateTime.toLocalDate()
            val today = LocalDate.now()
            val timePart = localDateTime.format(timeFormatter)

            val datePrefix = when {
                activityDate.isEqual(today) -> "Today"
                activityDate.isEqual(today.minusDays(1)) -> "Yesterday"
                else -> activityDate.format(dateFormatter)
            }
            binding.tvWorkoutTime.text = "$datePrefix, $timePart • GPS Tracked"

            // Icon according to exercise type
            val typeLower = workout.exerciseType.lowercase(Locale.US)
            val iconRes = when {
                typeLower.contains("ride") || typeLower.contains("cycle") -> R.drawable.ic_bike
                typeLower.contains("walk") -> R.drawable.ic_walk
                else -> R.drawable.ic_run
            }
            binding.ivWorkoutIcon.setImageResource(iconRes)

            // Metrics
            binding.tvMetricDistance.text = workout.distanceKmString
            binding.tvMetricDuration.text = "${workout.durationMinutes} min"
            binding.tvMetricCalories.text = if (workout.caloriesBurnedKcal != null) {
                "${workout.caloriesBurnedKcal.toInt()} kcal"
            } else {
                "--"
            }
            binding.tvMetricHeartRate.text = if (workout.avgHeartRateBpm != null) {
                "${workout.avgHeartRateBpm} bpm"
            } else {
                "--"
            }

            if (typeLower.contains("ride") || typeLower.contains("cycle")) {
                binding.tvMetricPaceLabel.text = "Avg Speed"
                binding.tvMetricPace.text = workout.avgPaceOrSpeed ?: "21.0 km/h"
            } else {
                binding.tvMetricPaceLabel.text = "Avg Pace"
                binding.tvMetricPace.text = workout.avgPaceOrSpeed ?: "5:20 /km"
            }

            // Sync status pills
            val context = binding.root.context
            if (workout.isSyncedToHealthConnect) {
                binding.badgeSyncHealthConnect.setBackgroundResource(R.drawable.bg_pill_health_connect)
                binding.badgeSyncHealthConnect.text = "Health Connect ✓"
                binding.badgeSyncHealthConnect.setTextColor(ContextCompat.getColor(context, R.color.status_connected_text))
            } else {
                binding.badgeSyncHealthConnect.setBackgroundResource(R.drawable.bg_pill_neutral)
                binding.badgeSyncHealthConnect.text = "Health Connect"
                binding.badgeSyncHealthConnect.setTextColor(ContextCompat.getColor(context, R.color.on_surface_secondary))
            }

            if (workout.isSyncedToStrava) {
                binding.badgeSyncStrava.setBackgroundResource(R.drawable.bg_pill_strava)
                binding.badgeSyncStrava.text = "Strava ✓"
                binding.badgeSyncStrava.setTextColor(ContextCompat.getColor(context, R.color.accent_workout))
            } else {
                binding.badgeSyncStrava.setBackgroundResource(R.drawable.bg_pill_neutral)
                binding.badgeSyncStrava.text = "Strava"
                binding.badgeSyncStrava.setTextColor(ContextCompat.getColor(context, R.color.on_surface_secondary))
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<WorkoutTelemetry>() {
        override fun areItemsTheSame(oldItem: WorkoutTelemetry, newItem: WorkoutTelemetry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutTelemetry, newItem: WorkoutTelemetry): Boolean {
            return oldItem == newItem
        }
    }
}
