package com.healthconnect.sync.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectRepository(private val context: Context) {

    val healthConnectClient: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    fun isSupported(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun readRecentSteps(days: Long = 1): List<StepTelemetry> {
        val client = healthConnectClient ?: return emptyList()
        val endTime = Instant.now()
        val startTime = endTime.minus(days, ChronoUnit.DAYS)

        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        val response = client.readRecords(request)
        return response.records.map {
            StepTelemetry(
                id = it.metadata.id,
                count = it.count,
                startTime = it.startTime,
                endTime = it.endTime
            )
        }
    }

    suspend fun readRecentWorkouts(days: Long = 1): List<WorkoutTelemetry> {
        val client = healthConnectClient ?: return emptyList()
        val endTime = Instant.now()
        val startTime = endTime.minus(days, ChronoUnit.DAYS)

        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        val response = client.readRecords(request)
        return response.records.map {
            WorkoutTelemetry(
                id = it.metadata.id,
                title = it.title ?: "Exercise Session",
                exerciseType = it.exerciseType.toString(),
                startTime = it.startTime,
                endTime = it.endTime
            )
        }
    }

    suspend fun insertSteps(steps: StepTelemetry): Boolean {
        val client = healthConnectClient ?: return false

        // Deduplication check: check if record with same time range already exists in Health Connect
        val existing = client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    steps.startTime.minusSeconds(1),
                    steps.endTime.plusSeconds(1)
                )
            )
        )

        val isDuplicate = existing.records.any {
            it.startTime == steps.startTime && it.endTime == steps.endTime
        }

        if (isDuplicate) {
            return false
        }

        val record = StepsRecord(
            count = steps.count,
            startTime = steps.startTime,
            startZoneOffset = null,
            endTime = steps.endTime,
            endZoneOffset = null
        )
        client.insertRecords(listOf(record))
        return true
    }

    suspend fun insertWorkout(workout: WorkoutTelemetry): Boolean {
        val client = healthConnectClient ?: return false

        // Deduplication check: verify if workout with identical time already exists
        val existing = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    workout.startTime.minusSeconds(1),
                    workout.endTime.plusSeconds(1)
                )
            )
        )

        val isDuplicate = existing.records.any {
            it.startTime == workout.startTime && it.endTime == workout.endTime
        }

        if (isDuplicate) {
            return false
        }

        val record = ExerciseSessionRecord(
            startTime = workout.startTime,
            startZoneOffset = null,
            endTime = workout.endTime,
            endZoneOffset = null,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = workout.title
        )
        client.insertRecords(listOf(record))
        return true
    }


    suspend fun performFullSync(): SyncReport {
        val client = healthConnectClient
            ?: return SyncReport(Instant.now(), 0, 0, 0, 0, false, "Health Connect Client unavailable")

        return try {
            val steps = readRecentSteps(days = 1)
            val workouts = readRecentWorkouts(days = 1)
            val totalSteps = steps.sumOf { it.count }.toInt()

            SyncReport(
                timestamp = Instant.now(),
                stepsSynced = totalSteps,
                heartRateSamplesSynced = 0,
                sleepSessionsSynced = 0,
                workoutsSynced = workouts.size,
                isSuccess = true,
                message = "Synced $totalSteps steps and ${workouts.size} workout(s) successfully."
            )
        } catch (e: Exception) {
            SyncReport(
                timestamp = Instant.now(),
                stepsSynced = 0,
                heartRateSamplesSynced = 0,
                sleepSessionsSynced = 0,
                workoutsSynced = 0,
                isSuccess = false,
                message = e.localizedMessage ?: "Unknown sync error"
            )
        }
    }
}
