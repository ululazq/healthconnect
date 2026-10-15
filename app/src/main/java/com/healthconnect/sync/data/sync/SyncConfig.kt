package com.healthconnect.sync.data.sync

import java.time.Instant

enum class PlatformType(
    val id: String,
    val displayName: String,
    val isSourceSupported: Boolean,
    val isDestinationSupported: Boolean,
    val iconResName: String
) {
    HUAWEI_HEALTH("huawei_health", "Huawei Health", isSourceSupported = true, isDestinationSupported = false, "ic_huawei"),
    HEALTH_CONNECT("health_connect", "Health Connect", isSourceSupported = true, isDestinationSupported = true, "ic_health_connect"),
    STRAVA("strava", "Strava", isSourceSupported = false, isDestinationSupported = true, "ic_strava"),
    SAMSUNG_HEALTH("samsung_health", "Samsung Health", isSourceSupported = true, isDestinationSupported = false, "ic_samsung"),
    GARMIN("garmin", "Garmin Connect", isSourceSupported = true, isDestinationSupported = true, "ic_garmin"),
    GOOGLE_FIT("google_fit", "Google Fit", isSourceSupported = true, isDestinationSupported = true, "ic_google_fit")
}

enum class SyncDataType(val displayName: String) {
    WORKOUTS("Activities & Workouts (GPS, HR)"),
    STEPS("Daily Steps"),
    HEART_RATE("Heart Rate"),
    SLEEP("Sleep Sessions"),
    WEIGHT("Weight & Body Composition")
}

data class SyncPairing(
    val id: String,
    val source: PlatformType,
    val destination: PlatformType,
    val enabledDataTypes: Set<SyncDataType>,
    var isEnabled: Boolean = true,
    var lastSyncTimestamp: Instant? = null,
    var lastStatus: String = "Ready"
)

data class PlatformAccount(
    val platform: PlatformType,
    var isConnected: Boolean = false,
    var accountIdentifier: String? = null,
    var authToken: String? = null
)
