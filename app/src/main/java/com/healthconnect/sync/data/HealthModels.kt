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
    val distanceMeters: Double? = null
)

data class SyncReport(
    val timestamp: Instant,
    val stepsSynced: Int,
    val heartRateSamplesSynced: Int,
    val sleepSessionsSynced: Int,
    val workoutsSynced: Int,
    val isSuccess: Boolean,
    val message: String
)
