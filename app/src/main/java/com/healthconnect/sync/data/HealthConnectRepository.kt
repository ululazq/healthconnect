package com.healthconnect.sync.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
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
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class)
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

        val exerciseTypeConstant = when {
            workout.exerciseType.contains("run", ignoreCase = true) -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            workout.exerciseType.contains("bike", ignoreCase = true) || workout.exerciseType.contains("cycle", ignoreCase = true) -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            workout.exerciseType.contains("walk", ignoreCase = true) -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            workout.exerciseType.contains("swim", ignoreCase = true) -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            workout.exerciseType.contains("hike", ignoreCase = true) -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }

        val record = ExerciseSessionRecord(
            startTime = workout.startTime,
            startZoneOffset = null,
            endTime = workout.endTime,
            endZoneOffset = null,
            exerciseType = exerciseTypeConstant,
            title = workout.title
        )
        client.insertRecords(listOf(record))
        return true
    }

    suspend fun insertHeartRate(hr: HeartRateTelemetry): Boolean {
        val client = healthConnectClient ?: return false

        val existing = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    hr.time.minusSeconds(5),
                    hr.time.plusSeconds(5)
                )
            )
        )

        if (existing.records.isNotEmpty()) {
            return false
        }

        val sample = HeartRateRecord.Sample(
            time = hr.time,
            beatsPerMinute = hr.bpm
        )

        val record = HeartRateRecord(
            startTime = hr.time.minusSeconds(1),
            startZoneOffset = null,
            endTime = hr.time.plusSeconds(1),
            endZoneOffset = null,
            samples = listOf(sample)
        )
        client.insertRecords(listOf(record))
        return true
    }

    suspend fun insertSleep(sleep: SleepTelemetry): Boolean {
        val client = healthConnectClient ?: return false

        val existing = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    sleep.startTime.minusSeconds(1),
                    sleep.endTime.plusSeconds(1)
                )
            )
        )

        if (existing.records.isNotEmpty()) {
            return false
        }

        val record = SleepSessionRecord(
            startTime = sleep.startTime,
            startZoneOffset = null,
            endTime = sleep.endTime,
            endZoneOffset = null,
            title = sleep.title
        )
        client.insertRecords(listOf(record))
        return true
    }

    suspend fun insertWeight(weight: WeightTelemetry): Boolean {
        val client = healthConnectClient ?: return false

        val existing = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    weight.time.minusSeconds(60),
                    weight.time.plusSeconds(60)
                )
            )
        )

        if (existing.records.isNotEmpty()) {
            return false
        }

        val record = WeightRecord(
            weight = Mass.kilograms(weight.weightKg),
            time = weight.time,
            zoneOffset = null
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
