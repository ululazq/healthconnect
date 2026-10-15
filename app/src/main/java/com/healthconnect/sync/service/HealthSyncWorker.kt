package com.healthconnect.sync.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthconnect.sync.data.HealthConnectRepository

class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = HealthConnectRepository(appContext)

    override suspend fun doWork(): Result {
        return try {
            if (!repository.isSupported() || !repository.hasAllPermissions()) {
                return Result.retry()
            }
            val report = repository.performFullSync()
            if (report.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
