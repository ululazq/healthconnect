package com.healthconnect.sync.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.healthconnect.sync.data.HealthConnectRepository
import com.healthconnect.sync.data.providers.HuaweiHealthProvider
import com.healthconnect.sync.data.providers.StravaProvider
import com.healthconnect.sync.data.sync.SyncPairingRepository
import com.healthconnect.sync.data.sync.SyncRouterEngine
import com.healthconnect.sync.data.sync.SyncScheduleFrequency
import com.healthconnect.sync.data.sync.SyncSettings
import java.util.concurrent.TimeUnit

class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = HealthConnectRepository(applicationContext)
            val pairingRepository = SyncPairingRepository(applicationContext)
            val huaweiProvider = HuaweiHealthProvider(applicationContext)
            val stravaProvider = StravaProvider(applicationContext)
            val routerEngine = SyncRouterEngine(
                applicationContext,
                pairingRepository,
                repository,
                huaweiProvider,
                stravaProvider
            )

            val results = routerEngine.executeAllActivePairings()
            val anyFailed = results.any { !it.isSuccess }
            if (anyFailed) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_health_sync_work"

        fun schedulePeriodicSync(context: Context, settings: SyncSettings) {
            val workManager = WorkManager.getInstance(context)

            if (!settings.isAutoSyncEnabled || settings.frequency == SyncScheduleFrequency.MANUAL_ONLY) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val intervalMinutes = settings.frequency.intervalMinutes.coerceAtLeast(15)

            val constraintsBuilder = Constraints.Builder()
                .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)

            if (settings.batteryNotLow) {
                constraintsBuilder.setRequiresBatteryNotLow(true)
            }
            if (settings.requiresCharging) {
                constraintsBuilder.setRequiresCharging(true)
            }

            val workRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                intervalMinutes,
                TimeUnit.MINUTES,
                5,
                TimeUnit.MINUTES
            )
                .setConstraints(constraintsBuilder.build())
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
