package com.healthconnect.sync.data.sync

import com.healthconnect.sync.data.HistoricalSyncRange
import java.time.Instant

enum class PlatformType(
    val id: String,
    val displayName: String,
    val isSourceSupported: Boolean,
    val isDestinationSupported: Boolean,
    val iconResName: String
) {
    HUAWEI_HEALTH("huawei_health", "Huawei Health", isSourceSupported = true, isDestinationSupported = true, "ic_huawei"),
    HEALTH_CONNECT("health_connect", "Google Health Connect", isSourceSupported = true, isDestinationSupported = true, "ic_health_connect"),
    STRAVA("strava", "Strava", isSourceSupported = false, isDestinationSupported = true, "ic_strava"),
    GARMIN("garmin", "Garmin Connect", isSourceSupported = true, isDestinationSupported = true, "ic_garmin"),
    SAMSUNG_HEALTH("samsung_health", "Samsung Health", isSourceSupported = true, isDestinationSupported = true, "ic_samsung"),
    FITBIT("fitbit", "Fitbit", isSourceSupported = true, isDestinationSupported = true, "ic_fitbit"),
    GOOGLE_FIT("google_fit", "Google Fit", isSourceSupported = true, isDestinationSupported = true, "ic_google_fit"),
    POLAR("polar", "Polar Flow", isSourceSupported = true, isDestinationSupported = true, "ic_polar"),
    SUUNTO("suunto", "Suunto", isSourceSupported = true, isDestinationSupported = true, "ic_run"),
    OURA("oura", "Oura Ring", isSourceSupported = true, isDestinationSupported = false, "ic_oura"),
    COROS("coros", "Coros", isSourceSupported = true, isDestinationSupported = true, "ic_coros"),
    WITHINGS("withings", "Withings Health", isSourceSupported = true, isDestinationSupported = true, "ic_withings")
}

enum class SyncDataType(val displayName: String) {
    WORKOUTS("Workouts & GPS Tracks"),
    STEPS("Daily Steps"),
    HEART_RATE("Heart Rate & Resting HR"),
    SLEEP("Sleep & Sleep Stages"),
    WEIGHT("Weight & Body Composition"),
    BLOOD_PRESSURE("Blood Pressure"),
    OXYGEN_SATURATION("Blood Oxygen (SpO2)"),
    HYDRATION("Water Intake")
}

enum class SyncScheduleFrequency(val intervalMinutes: Long, val displayName: String) {
    MINUTES_15(15, "Every 15 Minutes"),
    MINUTES_30(30, "Every 30 Minutes"),
    HOURS_1(60, "Every 1 Hour (Recommended)"),
    HOURS_3(180, "Every 3 Hours"),
    HOURS_6(360, "Every 6 Hours"),
    MANUAL_ONLY(0, "Manual Sync Only")
}

data class SyncSettings(
    var isAutoSyncEnabled: Boolean = true,
    var frequency: SyncScheduleFrequency = SyncScheduleFrequency.HOURS_1,
    var wifiOnly: Boolean = false,
    var requiresCharging: Boolean = false,
    var batteryNotLow: Boolean = true,
    var historicalLookback: HistoricalSyncRange = HistoricalSyncRange.PAST_7_DAYS
)

data class SyncPairing(
    val id: String,
    val source: PlatformType,
    val destination: PlatformType,
    var enabledDataTypes: Set<SyncDataType>,
    var isEnabled: Boolean = true,
    var lastSyncTimestamp: Instant? = null,
    var lastStatus: String = "Ready",
    var totalRecordsSynced: Int = 0
)

data class PlatformAccount(
    val platform: PlatformType,
    var isConnected: Boolean = false,
    var accountIdentifier: String? = null,
    var authToken: String? = null,
    var lastSyncTimestamp: Instant? = null
)

data class PairingSyncResult(
    val pairingId: String,
    val isSuccess: Boolean,
    val summary: String,
    val recordsTransferred: Int,
    val errorMessage: String? = null
)
