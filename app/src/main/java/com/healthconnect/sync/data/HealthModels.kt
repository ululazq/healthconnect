package com.healthconnect.sync.data

import java.time.Instant

data class StepTelemetry(
    val id: String = "",
    val count: Long,
    val startTime: Instant,
    val endTime: Instant
)

data class HeartRateTelemetry(
    val id: String = "",
    val bpm: Long,
    val time: Instant
)

data class SleepTelemetry(
    val id: String = "",
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long
)

data class WorkoutTelemetry(
    val id: String = "",
    val title: String,
    val exerciseType: String,
    val startTime: Instant,
    val endTime: Instant,
    val caloriesBurnedKcal: Double? = null,
    val distanceMeters: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val avgPaceOrSpeed: String? = null,
    val source: String = "Huawei Health",
    var isSyncedToHealthConnect: Boolean = false,
    var isSyncedToStrava: Boolean = false
) {
    val durationMinutes: Long
        get() = java.time.Duration.between(startTime, endTime).toMinutes().coerceAtLeast(1)

    val distanceKmString: String
        get() = if (distanceMeters != null && distanceMeters > 0) {
            String.format(java.util.Locale.US, "%.2f km", distanceMeters / 1000.0)
        } else {
            "--"
        }
}


data class SyncReport(
    val timestamp: Instant,
    val stepsSynced: Int,
    val heartRateSamplesSynced: Int,
    val sleepSessionsSynced: Int,
    val workoutsSynced: Int,
    val isSuccess: Boolean,
    val message: String
)
