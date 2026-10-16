package com.healthconnect.sync.data.sync

import android.content.Context

class SyncSettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): SyncSettings {
        val autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true)
        val freqName = prefs.getString(KEY_FREQUENCY, SyncScheduleFrequency.HOURS_1.name)
        val frequency = try {
            SyncScheduleFrequency.valueOf(freqName ?: SyncScheduleFrequency.HOURS_1.name)
        } catch (_: Exception) {
            SyncScheduleFrequency.HOURS_1
        }
        val wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false)
        val chargingOnly = prefs.getBoolean(KEY_CHARGING_ONLY, false)
        val batteryNotLow = prefs.getBoolean(KEY_BATTERY_NOT_LOW, true)

        return SyncSettings(
            isAutoSyncEnabled = autoSync,
            frequency = frequency,
            wifiOnly = wifiOnly,
            requiresCharging = chargingOnly,
            batteryNotLow = batteryNotLow
        )
    }

    fun saveSettings(settings: SyncSettings) {
        prefs.edit().apply {
            putBoolean(KEY_AUTO_SYNC, settings.isAutoSyncEnabled)
            putString(KEY_FREQUENCY, settings.frequency.name)
            putBoolean(KEY_WIFI_ONLY, settings.wifiOnly)
            putBoolean(KEY_CHARGING_ONLY, settings.requiresCharging)
            putBoolean(KEY_BATTERY_NOT_LOW, settings.batteryNotLow)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "sync_settings_prefs"
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"
        private const val KEY_FREQUENCY = "sync_frequency"
        private const val KEY_WIFI_ONLY = "sync_wifi_only"
        private const val KEY_CHARGING_ONLY = "sync_charging_only"
        private const val KEY_BATTERY_NOT_LOW = "sync_battery_not_low"
    }
}
