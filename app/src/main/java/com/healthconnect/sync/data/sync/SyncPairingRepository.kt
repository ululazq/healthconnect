package com.healthconnect.sync.data.sync

import android.content.Context
import java.time.Instant

class SyncPairingRepository(context: Context) {

    private val pairings = mutableListOf(
        SyncPairing(
            id = "pair_huawei_to_strava",
            source = PlatformType.HUAWEI_HEALTH,
            destination = PlatformType.STRAVA,
            enabledDataTypes = setOf(SyncDataType.WORKOUTS),
            isEnabled = true,
            lastSyncTimestamp = null,
            lastStatus = "Active"
        ),
        SyncPairing(
            id = "pair_huawei_to_health_connect",
            source = PlatformType.HUAWEI_HEALTH,
            destination = PlatformType.HEALTH_CONNECT,
            enabledDataTypes = setOf(
                SyncDataType.STEPS,
                SyncDataType.HEART_RATE,
                SyncDataType.SLEEP,
                SyncDataType.WORKOUTS
            ),
            isEnabled = true,
            lastSyncTimestamp = null,
            lastStatus = "Active"
        )
    )

    fun getPairings(): List<SyncPairing> {
        return pairings.toList()
    }

    fun togglePairing(id: String, enabled: Boolean) {
        val pairing = pairings.find { it.id == id }
        pairing?.isEnabled = enabled
    }

    fun updatePairingStatus(id: String, status: String, timestamp: Instant = Instant.now()) {
        val pairing = pairings.find { it.id == id }
        pairing?.let {
            it.lastStatus = status
            it.lastSyncTimestamp = timestamp
        }
    }

    fun addPairing(source: PlatformType, destination: PlatformType, types: Set<SyncDataType>): SyncPairing {
        val newPairing = SyncPairing(
            id = "pair_${source.id}_to_${destination.id}_${System.currentTimeMillis()}",
            source = source,
            destination = destination,
            enabledDataTypes = types,
            isEnabled = true
        )
        pairings.add(newPairing)
        return newPairing
    }
}
