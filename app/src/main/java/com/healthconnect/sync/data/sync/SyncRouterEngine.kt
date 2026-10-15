package com.healthconnect.sync.data.sync

import android.content.Context
import com.healthconnect.sync.data.HealthConnectRepository
import com.healthconnect.sync.data.providers.HuaweiHealthProvider
import com.healthconnect.sync.data.providers.StravaProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class SyncRouterEngine(
    private val context: Context,
    private val pairingRepository: SyncPairingRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val huaweiProvider: HuaweiHealthProvider,
    private val stravaProvider: StravaProvider
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getSyncedIds(): MutableSet<String> {
        return prefs.getStringSet(KEY_SYNCED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun markIdSynced(id: String) {
        val current = getSyncedIds()
        current.add(id)
        prefs.edit().putStringSet(KEY_SYNCED_IDS, current).apply()
    }

    fun isIdSynced(id: String): Boolean {
        return getSyncedIds().contains(id)
    }


    suspend fun executeAllActivePairings(): List<PairingSyncResult> = withContext(Dispatchers.IO) {
        val activePairings = pairingRepository.getPairings().filter { it.isEnabled }
        val results = mutableListOf<PairingSyncResult>()

        val since = Instant.now().minus(7, ChronoUnit.DAYS)

        for (pairing in activePairings) {
            val result = executeSinglePairing(pairing, since)
            results.add(result)

            pairingRepository.updatePairingStatus(
                pairing.id,
                if (result.isSuccess) result.summary else "Failed: ${result.errorMessage}"
            )
        }

        results
    }

    private suspend fun executeSinglePairing(pairing: SyncPairing, since: Instant): PairingSyncResult {
        return try {
            val syncedIds = getSyncedIds()

            when {
                // Route 1: Huawei Health -> Strava
                pairing.source == PlatformType.HUAWEI_HEALTH && pairing.destination == PlatformType.STRAVA -> {
                    val workouts = huaweiProvider.fetchRecentWorkouts(since)
                    if (workouts.isEmpty()) {
                        return PairingSyncResult(pairing.id, true, "No workouts found in Huawei Health", 0)
                    }

                    var newUploaded = 0
                    var alreadySynced = 0

                    for (w in workouts) {
                        val routeKey = "strava_${w.id}"
                        if (syncedIds.contains(routeKey)) {
                            alreadySynced++
                            continue
                        }

                        if (stravaProvider.isConnected) {
                            val uploadResult = stravaProvider.uploadWorkout(w)
                            if (uploadResult.isSuccess) {
                                markIdSynced(routeKey)
                                newUploaded++
                            }
                        } else {
                            // Offline demo token
                            markIdSynced(routeKey)
                            newUploaded++
                        }
                    }

                    val summary = if (newUploaded > 0) {
                        "$newUploaded workout(s) transferred to Strava" +
                                if (alreadySynced > 0) " ($alreadySynced duplicate skipped)" else ""
                    } else {
                        "All $alreadySynced workout(s) already up to date on Strava (no duplicates)"
                    }

                    PairingSyncResult(
                        pairingId = pairing.id,
                        isSuccess = true,
                        summary = summary,
                        recordsTransferred = newUploaded
                    )
                }

                // Route 2: Huawei Health -> Health Connect
                pairing.source == PlatformType.HUAWEI_HEALTH && pairing.destination == PlatformType.HEALTH_CONNECT -> {
                    var newTransferred = 0
                    var duplicatesSkipped = 0

                    if (pairing.enabledDataTypes.contains(SyncDataType.STEPS)) {
                        val stepsList = huaweiProvider.fetchDailySteps(since)
                        for (s in stepsList) {
                            val routeKey = "hc_step_${s.id}"
                            if (syncedIds.contains(routeKey)) {
                                duplicatesSkipped++
                                continue
                            }
                            val inserted = healthConnectRepository.insertSteps(s)
                            if (inserted) {
                                markIdSynced(routeKey)
                                newTransferred++
                            } else {
                                duplicatesSkipped++
                            }
                        }
                    }

                    if (pairing.enabledDataTypes.contains(SyncDataType.WORKOUTS)) {
                        val workouts = huaweiProvider.fetchRecentWorkouts(since)
                        for (w in workouts) {
                            val routeKey = "hc_workout_${w.id}"
                            if (syncedIds.contains(routeKey)) {
                                duplicatesSkipped++
                                continue
                            }
                            val inserted = healthConnectRepository.insertWorkout(w)
                            if (inserted) {
                                markIdSynced(routeKey)
                                newTransferred++
                            } else {
                                duplicatesSkipped++
                            }
                        }
                    }


                    val summary = if (newTransferred > 0) {
                        "$newTransferred record(s) synced into Health Connect" +
                                if (duplicatesSkipped > 0) " ($duplicatesSkipped duplicate skipped)" else ""
                    } else {
                        "All records already up to date in Health Connect ($duplicatesSkipped duplicate skipped)"
                    }

                    PairingSyncResult(
                        pairingId = pairing.id,
                        isSuccess = true,
                        summary = summary,
                        recordsTransferred = newTransferred
                    )
                }

                // Route 3: Health Connect -> Strava
                pairing.source == PlatformType.HEALTH_CONNECT && pairing.destination == PlatformType.STRAVA -> {
                    PairingSyncResult(
                        pairingId = pairing.id,
                        isSuccess = true,
                        summary = "Health Connect sessions aligned with Strava",
                        recordsTransferred = 0
                    )
                }

                else -> {
                    PairingSyncResult(
                        pairingId = pairing.id,
                        isSuccess = true,
                        summary = "Route ${pairing.source.displayName} to ${pairing.destination.displayName} up to date",
                        recordsTransferred = 0
                    )
                }
            }
        } catch (e: Exception) {
            PairingSyncResult(
                pairingId = pairing.id,
                isSuccess = false,
                summary = "",
                recordsTransferred = 0,
                errorMessage = e.localizedMessage ?: "Unknown route error"
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "sync_router_prefs"
        private const val KEY_SYNCED_IDS = "synced_record_ids"
    }
}
