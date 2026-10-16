package com.healthconnect.sync.data

import android.content.Context
import com.healthconnect.sync.data.sync.PlatformAccount
import com.healthconnect.sync.data.sync.PlatformType
import java.time.Instant

class PlatformManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllAccounts(): List<PlatformAccount> {
        return PlatformType.entries.map { platform ->
            val isConnected = prefs.getBoolean("connected_${platform.id}", defaultConnected(platform))
            val accountId = prefs.getString("account_${platform.id}", defaultIdentifier(platform))
            val token = prefs.getString("token_${platform.id}", if (isConnected) "sample_token_${platform.id}" else null)

            PlatformAccount(
                platform = platform,
                isConnected = isConnected,
                accountIdentifier = accountId,
                authToken = token
            )
        }
    }

    fun isPlatformConnected(platform: PlatformType): Boolean {
        return prefs.getBoolean("connected_${platform.id}", defaultConnected(platform))
    }

    fun setPlatformConnected(platform: PlatformType, connected: Boolean, accountId: String? = null) {
        prefs.edit().apply {
            putBoolean("connected_${platform.id}", connected)
            if (connected) {
                putString("account_${platform.id}", accountId ?: defaultIdentifier(platform))
                putString("token_${platform.id}", "token_${platform.id}_${System.currentTimeMillis()}")
            } else {
                remove("account_${platform.id}")
                remove("token_${platform.id}")
            }
            apply()
        }
    }

    fun recordSyncTime(platform: PlatformType) {
        prefs.edit().putLong("last_sync_${platform.id}", Instant.now().epochSecond).apply()
    }

    fun getLastSyncTime(platform: PlatformType): Instant? {
        val seconds = prefs.getLong("last_sync_${platform.id}", 0)
        return if (seconds > 0) Instant.ofEpochSecond(seconds) else null
    }

    private fun defaultConnected(platform: PlatformType): Boolean {
        // Huawei Health is master, Health Connect and Strava pre-configured
        return when (platform) {
            PlatformType.HUAWEI_HEALTH -> true
            PlatformType.HEALTH_CONNECT -> true
            PlatformType.STRAVA -> false
            PlatformType.GARMIN -> false
            PlatformType.SAMSUNG_HEALTH -> false
            PlatformType.FITBIT -> false
            else -> false
        }
    }

    private fun defaultIdentifier(platform: PlatformType): String {
        return when (platform) {
            PlatformType.HUAWEI_HEALTH -> "Huawei Cloud Account"
            PlatformType.HEALTH_CONNECT -> "Android System Client"
            PlatformType.STRAVA -> "Strava Athlete #83921"
            PlatformType.GARMIN -> "Garmin Connect User"
            PlatformType.SAMSUNG_HEALTH -> "Samsung Health Device"
            PlatformType.FITBIT -> "Fitbit Web API Profile"
            PlatformType.GOOGLE_FIT -> "Google Account Fitness"
            PlatformType.POLAR -> "Polar Flow User"
            PlatformType.SUUNTO -> "Suunto Movescount"
            PlatformType.OURA -> "Oura Cloud Ring"
            PlatformType.COROS -> "Coros Training Hub"
            PlatformType.WITHINGS -> "Withings Health Mate"
        }
    }

    companion object {
        private const val PREFS_NAME = "platform_manager_prefs"
    }
}
