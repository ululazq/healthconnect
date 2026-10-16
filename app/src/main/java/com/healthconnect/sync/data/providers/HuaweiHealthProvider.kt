package com.healthconnect.sync.data.providers

import android.content.Context
import com.healthconnect.sync.data.BloodPressureTelemetry
import com.healthconnect.sync.data.HeartRateTelemetry
import com.healthconnect.sync.data.HydrationTelemetry
import com.healthconnect.sync.data.OxygenSaturationTelemetry
import com.healthconnect.sync.data.SleepTelemetry
import com.healthconnect.sync.data.StepTelemetry
import com.healthconnect.sync.data.WeightTelemetry
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
        get() = prefs.getString(KEY_ACCOUNT_ID, "Huawei_Cloud_Sync")
        set(value) = prefs.edit().putString(KEY_ACCOUNT_ID, value).apply()

    suspend fun fetchRecentWorkouts(since: Instant = Instant.EPOCH): List<WorkoutTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val threeDaysAgo = today.minusDays(3)
        val fourDaysAgo = today.minusDays(4)
        val fiveDaysAgo = today.minusDays(5)

        val run1Start = today.atTime(6, 45).atZone(ZoneId.systemDefault()).toInstant()
        val run1End = run1Start.plus(42, ChronoUnit.MINUTES)

        val ride1Start = yesterday.atTime(17, 15).atZone(ZoneId.systemDefault()).toInstant()
        val ride1End = ride1Start.plus(55, ChronoUnit.MINUTES)

        val walk1Start = twoDaysAgo.atTime(7, 30).atZone(ZoneId.systemDefault()).toInstant()
        val walk1End = walk1Start.plus(38, ChronoUnit.MINUTES)

        val run2Start = threeDaysAgo.atTime(6, 30).atZone(ZoneId.systemDefault()).toInstant()
        val run2End = run2Start.plus(50, ChronoUnit.MINUTES)

        val swimStart = fourDaysAgo.atTime(16, 0).atZone(ZoneId.systemDefault()).toInstant()
        val swimEnd = swimStart.plus(45, ChronoUnit.MINUTES)

        val hikeStart = fiveDaysAgo.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant()
        val hikeEnd = hikeStart.plus(120, ChronoUnit.MINUTES)

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
                maxHeartRateBpm = 168,
                avgPaceOrSpeed = "5:18 /km",
                elevationGainMeters = 35.0,
                avgCadence = 164,
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
                maxHeartRateBpm = 155,
                avgPaceOrSpeed = "20.3 km/h",
                elevationGainMeters = 120.0,
                avgCadence = 82,
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
                maxHeartRateBpm = 122,
                avgPaceOrSpeed = "11:00 /km",
                elevationGainMeters = 15.0,
                avgCadence = 112,
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
                maxHeartRateBpm = 175,
                avgPaceOrSpeed = "4:56 /km",
                elevationGainMeters = 48.0,
                avgCadence = 172,
                source = "Huawei Health"
            ),
            WorkoutTelemetry(
                id = "huawei_workout_${fourDaysAgo}_1600",
                title = "Indoor Pool Swimming",
                exerciseType = "Swim",
                startTime = swimStart,
                endTime = swimEnd,
                caloriesBurnedKcal = 340.0,
                distanceMeters = 1500.0,
                avgHeartRateBpm = 128,
                maxHeartRateBpm = 145,
                avgPaceOrSpeed = "2:15 /100m",
                elevationGainMeters = 0.0,
                avgCadence = 28,
                source = "Huawei Health"
            ),
            WorkoutTelemetry(
                id = "huawei_workout_${fiveDaysAgo}_0800",
                title = "Mountain Trail Hike",
                exerciseType = "Hike",
                startTime = hikeStart,
                endTime = hikeEnd,
                caloriesBurnedKcal = 620.0,
                distanceMeters = 7800.0,
                avgHeartRateBpm = 126,
                maxHeartRateBpm = 152,
                avgPaceOrSpeed = "15:23 /km",
                elevationGainMeters = 310.0,
                avgCadence = 98,
                source = "Huawei Health"
            )
        )

        allWorkouts.filter { it.startTime.isAfter(since) }
    }

    suspend fun fetchDailySteps(since: Instant): List<StepTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val dayEnd = dayStart.plus(18, ChronoUnit.HOURS)

        val yStart = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val yEnd = yStart.plus(22, ChronoUnit.HOURS)

        val list = listOf(
            StepTelemetry(
                id = "huawei_steps_${today}",
                count = 6842,
                distanceMeters = 5120.0,
                caloriesKcal = 285.0,
                startTime = dayStart,
                endTime = dayEnd
            ),
            StepTelemetry(
                id = "huawei_steps_${yesterday}",
                count = 10450,
                distanceMeters = 8150.0,
                caloriesKcal = 420.0,
                startTime = yStart,
                endTime = yEnd
            )
        )
        list.filter { it.startTime.isAfter(since) }
    }

    suspend fun fetchHeartRate(since: Instant): List<HeartRateTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val sampleTime1 = today.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant()
        val sampleTime2 = today.atTime(12, 30).atZone(ZoneId.systemDefault()).toInstant()

        listOf(
            HeartRateTelemetry(id = "huawei_hr_${today}_0800", bpm = 68, restingBpm = 64, time = sampleTime1),
            HeartRateTelemetry(id = "huawei_hr_${today}_1230", bpm = 74, restingBpm = 64, time = sampleTime2)
        ).filter { it.time.isAfter(since) }
    }

    suspend fun fetchSleep(since: Instant): List<SleepTelemetry> = withContext(Dispatchers.IO) {
        val yesterday = LocalDate.now().minusDays(1)
        val sleepStart = yesterday.atTime(23, 0).atZone(ZoneId.systemDefault()).toInstant()
        val sleepEnd = LocalDate.now().atTime(6, 30).atZone(ZoneId.systemDefault()).toInstant()

        listOf(
            SleepTelemetry(
                id = "huawei_sleep_${yesterday}",
                title = "Huawei TruSleep",
                startTime = sleepStart,
                endTime = sleepEnd,
                durationMinutes = 450,
                deepMinutes = 110,
                lightMinutes = 230,
                remMinutes = 90,
                awakeMinutes = 20,
                sleepScore = 88
            )
        ).filter { it.startTime.isAfter(since) }
    }

    suspend fun fetchWeight(since: Instant): List<WeightTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val weighTime = today.atTime(7, 0).atZone(ZoneId.systemDefault()).toInstant()

        listOf(
            WeightTelemetry(
                id = "huawei_weight_${today}",
                weightKg = 68.4,
                bodyFatPercentage = 16.2,
                bmi = 22.1,
                muscleMassKg = 54.2,
                time = weighTime
            )
        ).filter { it.time.isAfter(since) }
    }

    suspend fun fetchBloodPressure(since: Instant): List<BloodPressureTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val bpTime = today.atTime(8, 15).atZone(ZoneId.systemDefault()).toInstant()

        listOf(
            BloodPressureTelemetry(
                id = "huawei_bp_${today}",
                systolic = 118.0,
                diastolic = 76.0,
                pulse = 68,
                time = bpTime
            )
        ).filter { it.time.isAfter(since) }
    }

    suspend fun fetchSpO2(since: Instant): List<OxygenSaturationTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val spo2Time = today.atTime(8, 30).atZone(ZoneId.systemDefault()).toInstant()

        listOf(
            OxygenSaturationTelemetry(
                id = "huawei_spo2_${today}",
                percentage = 98.5,
                time = spo2Time
            )
        ).filter { it.time.isAfter(since) }
    }

    suspend fun fetchHydration(since: Instant): List<HydrationTelemetry> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val hydroTime = today.atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant()

        listOf(
            HydrationTelemetry(
                id = "huawei_hydro_${today}",
                volumeMilliliters = 1500.0,
                time = hydroTime
            )
        ).filter { it.time.isAfter(since) }
    }

    companion object {
        private const val PREFS_NAME = "huawei_health_prefs"
        private const val KEY_IS_CONNECTED = "huawei_is_connected"
        private const val KEY_ACCOUNT_ID = "huawei_account_id"
    }
}
