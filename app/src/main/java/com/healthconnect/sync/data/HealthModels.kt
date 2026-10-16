package com.healthconnect.sync.data

import java.time.Duration
import java.time.Instant
import java.util.Locale

data class StepTelemetry(
    val id: String = "",
    val count: Long,
    val distanceMeters: Double? = null,
    val caloriesKcal: Double? = null,
    val startTime: Instant,
    val endTime: Instant
)

data class HeartRateTelemetry(
    val id: String = "",
    val bpm: Long,
    val restingBpm: Long? = null,
    val time: Instant
)

data class SleepTelemetry(
    val id: String = "",
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long,
    val deepMinutes: Long? = null,
    val lightMinutes: Long? = null,
    val remMinutes: Long? = null,
    val awakeMinutes: Long? = null,
    val sleepScore: Int? = null
)

data class WeightTelemetry(
    val id: String = "",
    val weightKg: Double,
    val bodyFatPercentage: Double? = null,
    val bmi: Double? = null,
    val muscleMassKg: Double? = null,
    val time: Instant
)

data class BloodPressureTelemetry(
    val id: String = "",
    val systolic: Double,
    val diastolic: Double,
    val pulse: Int? = null,
    val time: Instant
)

data class OxygenSaturationTelemetry(
    val id: String = "",
    val percentage: Double,
    val time: Instant
)

data class HydrationTelemetry(
    val id: String = "",
    val volumeMilliliters: Double,
    val time: Instant
)

data class GPSPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double? = null,
    val heartRateBpm: Int? = null,
    val timestamp: Instant
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
    val maxHeartRateBpm: Int? = null,
    val avgPaceOrSpeed: String? = null,
    val elevationGainMeters: Double? = null,
    val avgCadence: Int? = null,
    val source: String = "Huawei Health",
    var isSyncedToHealthConnect: Boolean = false,
    var isSyncedToStrava: Boolean = false,
    var isSyncedToGarmin: Boolean = false,
    val gpsPoints: List<GPSPoint> = emptyList(),
    val gpxContent: String? = null
) {
    val durationMinutes: Long
        get() = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(1)

    val distanceKmString: String
        get() = if (distanceMeters != null && distanceMeters > 0) {
            String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
        } else {
            "--"
        }

    val elevationGainString: String
        get() = if (elevationGainMeters != null && elevationGainMeters > 0) {
            String.format(Locale.US, "+%.0f m", elevationGainMeters)
        } else {
            "--"
        }
}

enum class HistoricalSyncRange(val days: Long, val displayName: String) {
    PAST_24_HOURS(1, "Past 24 Hours"),
    PAST_7_DAYS(7, "Past 7 Days (Recommended)"),
    PAST_14_DAYS(14, "Past 14 Days"),
    PAST_30_DAYS(30, "Past 30 Days (1 Month)"),
    PAST_90_DAYS(90, "Past 90 Days (Quarter)"),
    ALL_TIME(365, "All Available Records")
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
