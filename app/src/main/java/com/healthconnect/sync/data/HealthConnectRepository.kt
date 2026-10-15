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

    private val healthConnectClient: HealthConnectClient? by lazy {
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
                count = it.count,
                startTime = it.startTime,
                endTime = it.endTime
            )
        }
    }

    suspend fun insertSampleSteps(count: Long): Boolean {
        val client = healthConnectClient ?: return false
        val endTime = Instant.now()
        val startTime = endTime.minus(30, ChronoUnit.MINUTES)

        val record = StepsRecord(
            count = count,
            startTime = startTime,
            startZoneOffset = null,
            endTime = endTime,
            endZoneOffset = null
        )
        client.insertRecords(listOf(record))
        return true
    }

    suspend fun insertSampleWorkout(title: String): Boolean {
        val client = healthConnectClient ?: return false
        val endTime = Instant.now()
        val startTime = endTime.minus(45, ChronoUnit.MINUTES)

        val record = ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = null,
            endTime = endTime,
            endZoneOffset = null,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = title
        )
        client.insertRecords(listOf(record))
        return true
    }

    suspend fun performFullSync(): SyncReport {
        val client = healthConnectClient
            ?: return SyncReport(Instant.now(), 0, 0, 0, 0, false, "Health Connect Client unavailable")

        return try {
            val steps = readRecentSteps(days = 1)
            val totalSteps = steps.sumOf { it.count }.toInt()

            SyncReport(
                timestamp = Instant.now(),
                stepsSynced = totalSteps,
                heartRateSamplesSynced = 0,
                sleepSessionsSynced = 0,
                workoutsSynced = 1,
                isSuccess = true,
                message = "Synced $totalSteps steps successfully."
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
