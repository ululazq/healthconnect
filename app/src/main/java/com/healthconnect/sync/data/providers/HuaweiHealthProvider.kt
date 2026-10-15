package com.healthconnect.sync.data.providers

import android.content.Context
import com.healthconnect.sync.data.HeartRateTelemetry
import com.healthconnect.sync.data.SleepTelemetry
import com.healthconnect.sync.data.StepTelemetry
import com.healthconnect.sync.data.WorkoutTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
        val today = LocalDate.now()
        val workoutStart = today.atTime(7, 0).atZone(ZoneId.systemDefault()).toInstant()
        val workoutEnd = workoutStart.plus(45, ChronoUnit.MINUTES)

        val stableId = "huawei_workout_${today}_0700"

        listOf(
            WorkoutTelemetry(
                id = stableId,
                title = "Huawei Health Outdoor Run",
                exerciseType = "Run",
                startTime = workoutStart,
                endTime = workoutEnd,
                caloriesBurnedKcal = 385.0,
                distanceMeters = 5240.0
            )
        )
    }

    suspend fun fetchDailySteps(since: Instant): List<StepTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val dayEnd = dayStart.plus(18, ChronoUnit.HOURS)

        val stableId = "huawei_steps_${today}"

        listOf(
            StepTelemetry(
                id = stableId,
                count = 6842,
                startTime = dayStart,
                endTime = dayEnd
            )
        )
    }

    suspend fun fetchHeartRate(since: Instant): List<HeartRateTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val sampleTime1 = today.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant()
        val sampleTime2 = today.atTime(12, 30).atZone(ZoneId.systemDefault()).toInstant()

        listOf(
            HeartRateTelemetry(id = "huawei_hr_${today}_0800", bpm = 68, time = sampleTime1),
            HeartRateTelemetry(id = "huawei_hr_${today}_1230", bpm = 74, time = sampleTime2)
        )
    }

    suspend fun fetchSleep(since: Instant): List<SleepTelemetry> = withContext(Dispatchers.IO) {
        val yesterday = LocalDate.now().minusDays(1)
        val sleepStart = yesterday.atTime(23, 0).atZone(ZoneId.systemDefault()).toInstant()
        val sleepEnd = LocalDate.now().atTime(6, 30).atZone(ZoneId.systemDefault()).toInstant()

        val stableId = "huawei_sleep_${yesterday}"

        listOf(
            SleepTelemetry(
                id = stableId,
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
