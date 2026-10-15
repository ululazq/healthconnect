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

    suspend fun fetchRecentWorkouts(since: Instant = Instant.EPOCH): List<WorkoutTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val threeDaysAgo = today.minusDays(3)

        val run1Start = today.atTime(6, 45).atZone(ZoneId.systemDefault()).toInstant()
        val run1End = run1Start.plus(42, ChronoUnit.MINUTES)

        val ride1Start = yesterday.atTime(17, 15).atZone(ZoneId.systemDefault()).toInstant()
        val ride1End = ride1Start.plus(55, ChronoUnit.MINUTES)

        val walk1Start = twoDaysAgo.atTime(7, 30).atZone(ZoneId.systemDefault()).toInstant()
        val walk1End = walk1Start.plus(38, ChronoUnit.MINUTES)

        val run2Start = threeDaysAgo.atTime(6, 30).atZone(ZoneId.systemDefault()).toInstant()
        val run2End = run2Start.plus(50, ChronoUnit.MINUTES)

        val allWorkouts = listOf(
            WorkoutTelemetry(
                id = "huawei_workout_${today}_0645",
                title = "Morning Outdoor Run",
                exerciseType = "Run",
                startTime = run1Start,
                endTime = run1End,
                caloriesBurnedKcal = 380.0,
                distanceMeters = 5240.0,
                avgHeartRateBpm = 148,
                avgPaceOrSpeed = "5:18 /km",
                source = "Huawei Health"
            ),
            WorkoutTelemetry(
                id = "huawei_workout_${yesterday}_1715",
                title = "Sunset Coastal Ride",
                exerciseType = "Ride",
                startTime = ride1Start,
                endTime = ride1End,
                caloriesBurnedKcal = 495.0,
                distanceMeters = 18600.0,
                avgHeartRateBpm = 134,
                avgPaceOrSpeed = "20.3 km/h",
                source = "Huawei Health"
            ),
            WorkoutTelemetry(
                id = "huawei_workout_${twoDaysAgo}_0730",
                title = "Park Brisk Walk",
                exerciseType = "Walk",
                startTime = walk1Start,
                endTime = walk1End,
                caloriesBurnedKcal = 185.0,
                distanceMeters = 3450.0,
                avgHeartRateBpm = 105,
                avgPaceOrSpeed = "11:00 /km",
                source = "Huawei Health"
            ),
            WorkoutTelemetry(
                id = "huawei_workout_${threeDaysAgo}_0630",
                title = "Tempo Endurance Run",
                exerciseType = "Run",
                startTime = run2Start,
                endTime = run2End,
                caloriesBurnedKcal = 460.0,
                distanceMeters = 6800.0,
                avgHeartRateBpm = 158,
                avgPaceOrSpeed = "4:56 /km",
                source = "Huawei Health"
            )
        )

        allWorkouts.filter { it.startTime.isAfter(since) }
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
