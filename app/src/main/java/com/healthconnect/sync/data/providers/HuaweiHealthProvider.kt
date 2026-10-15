package com.healthconnect.sync.data.providers

import android.content.Context
import com.healthconnect.sync.data.HeartRateTelemetry
import com.healthconnect.sync.data.SleepTelemetry
import com.healthconnect.sync.data.StepTelemetry
import com.healthconnect.sync.data.WorkoutTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class HuaweiHealthProvider(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isConnected: Boolean
        get() = prefs.getBoolean(KEY_IS_CONNECTED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_CONNECTED, value).apply()

    var huaweiAccountId: String?
        get() = prefs.getString(KEY_ACCOUNT_ID, "Huawei_User_Sync")
        set(value) = prefs.edit().putString(KEY_ACCOUNT_ID, value).apply()

    suspend fun fetchRecentWorkouts(since: Instant): List<WorkoutTelemetry> = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val workoutStart = now.minus(50, ChronoUnit.MINUTES)

        listOf(
            WorkoutTelemetry(
                title = "Huawei Health Outdoor Run",
                exerciseType = "Run",
                startTime = workoutStart,
                endTime = now.minus(5, ChronoUnit.MINUTES),
                caloriesBurnedKcal = 385.0,
                distanceMeters = 5240.0
            )
        )
    }

    suspend fun fetchDailySteps(since: Instant): List<StepTelemetry> = withContext(Dispatchers.IO) {
        val now = Instant.now()
        listOf(
            StepTelemetry(
                count = 6842,
                startTime = now.minus(12, ChronoUnit.HOURS),
                endTime = now
            )
        )
    }

    suspend fun fetchHeartRate(since: Instant): List<HeartRateTelemetry> = withContext(Dispatchers.IO) {
        val now = Instant.now()
        listOf(
            HeartRateTelemetry(bpm = 68, time = now.minus(15, ChronoUnit.MINUTES)),
            HeartRateTelemetry(bpm = 74, time = now.minus(5, ChronoUnit.MINUTES))
        )
    }

    suspend fun fetchSleep(since: Instant): List<SleepTelemetry> = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val sleepStart = now.minus(24, ChronoUnit.HOURS).plus(22, ChronoUnit.HOURS)
        val sleepEnd = now.minus(24, ChronoUnit.HOURS).plus(29, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES)

        listOf(
            SleepTelemetry(
                title = "Huawei TruSleep",
                startTime = sleepStart,
                endTime = sleepEnd,
                durationMinutes = 450
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "huawei_health_prefs"
        private const val KEY_IS_CONNECTED = "huawei_is_connected"
        private const val KEY_ACCOUNT_ID = "huawei_account_id"
    }
}
