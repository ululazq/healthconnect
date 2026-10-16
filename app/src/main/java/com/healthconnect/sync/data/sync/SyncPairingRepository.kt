package com.healthconnect.sync.data.sync

import android.content.Context
import java.time.Instant

class SyncPairingRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val pairings = mutableListOf<SyncPairing>()

    init {
        loadPairings()
    }

    fun getPairings(): List<SyncPairing> {
        return pairings.toList()
    }

    fun togglePairing(id: String, enabled: Boolean) {
        val pairing = pairings.find { it.id == id }
        pairing?.let {
            it.isEnabled = enabled
            savePairings()
        }
    }

    fun updatePairingStatus(id: String, status: String, timestamp: Instant = Instant.now()) {
        val pairing = pairings.find { it.id == id }
        pairing?.let {
            it.lastStatus = status
            it.lastSyncTimestamp = timestamp
            savePairings()
        }
    }

    fun incrementRecordCount(id: String, count: Int) {
        val pairing = pairings.find { it.id == id }
        pairing?.let {
            it.totalRecordsSynced += count
            savePairings()
        }
    }

    fun updatePairingDataTypes(id: String, types: Set<SyncDataType>) {
        val pairing = pairings.find { it.id == id }
        pairing?.let {
            it.enabledDataTypes = types
            savePairings()
        }
    }

    fun addPairing(source: PlatformType, destination: PlatformType, types: Set<SyncDataType>): SyncPairing {
        val newPairing = SyncPairing(
            id = "pair_${source.id}_to_${destination.id}_${System.currentTimeMillis()}",
            source = source,
            destination = destination,
            enabledDataTypes = types,
            isEnabled = true,
            lastStatus = "Active"
        )
        pairings.add(newPairing)
        savePairings()
        return newPairing
    }

    fun deletePairing(id: String): Boolean {
        val removed = pairings.removeIf { it.id == id }
        if (removed) {
            savePairings()
        }
        return removed
    }

    private fun loadPairings() {
        pairings.clear()
        val serialized = prefs.getString(KEY_PAIRINGS, null)
        if (serialized.isNullOrBlank()) {
            // Default initial routes:
            // 1. Huawei Health -> Strava
            // 2. Huawei Health -> Health Connect
            // 3. Huawei Health -> Garmin Connect
            pairings.add(
                SyncPairing(
                    id = "pair_huawei_to_strava",
                    source = PlatformType.HUAWEI_HEALTH,
                    destination = PlatformType.STRAVA,
                    enabledDataTypes = setOf(SyncDataType.WORKOUTS),
                    isEnabled = true,
                    lastStatus = "Active"
                )
            )
            pairings.add(
                SyncPairing(
                    id = "pair_huawei_to_health_connect",
                    source = PlatformType.HUAWEI_HEALTH,
                    destination = PlatformType.HEALTH_CONNECT,
                    enabledDataTypes = setOf(
                        SyncDataType.WORKOUTS,
                        SyncDataType.STEPS,
                        SyncDataType.HEART_RATE,
                        SyncDataType.SLEEP,
                        SyncDataType.WEIGHT
                    ),
                    isEnabled = true,
                    lastStatus = "Active"
                )
            )
            pairings.add(
                SyncPairing(
                    id = "pair_huawei_to_garmin",
                    source = PlatformType.HUAWEI_HEALTH,
                    destination = PlatformType.GARMIN,
                    enabledDataTypes = setOf(SyncDataType.WORKOUTS, SyncDataType.STEPS),
                    isEnabled = false,
                    lastStatus = "Paused"
                )
            )
            savePairings()
        } else {
            // Parse saved pipe-separated format: id|source|dest|types|enabled|count|status
            val lines = serialized.split("\n")
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split(";;")
                if (parts.size >= 5) {
                    val id = parts[0]
                    val source = PlatformType.entries.find { it.id == parts[1] } ?: PlatformType.HUAWEI_HEALTH
                    val dest = PlatformType.entries.find { it.id == parts[2] } ?: PlatformType.HEALTH_CONNECT
                    val typeNames = parts[3].split(",").filter { it.isNotBlank() }
                    val types = typeNames.mapNotNull { name ->
                        try { SyncDataType.valueOf(name) } catch (_: Exception) { null }
                    }.toSet()
                    val enabled = parts[4].toBoolean()
                    val count = if (parts.size > 5) parts[5].toIntOrNull() ?: 0 else 0
                    val status = if (parts.size > 6) parts[6] else "Active"

                    pairings.add(
                        SyncPairing(
                            id = id,
                            source = source,
                            destination = dest,
                            enabledDataTypes = types,
                            isEnabled = enabled,
                            totalRecordsSynced = count,
                            lastStatus = status
                        )
                    )
                }
            }
        }
    }

    private fun savePairings() {
        val sb = StringBuilder()
        for (p in pairings) {
            val typesStr = p.enabledDataTypes.joinToString(",") { it.name }
            sb.append("${p.id};;${p.source.id};;${p.destination.id};;$typesStr;;${p.isEnabled};;${p.totalRecordsSynced};;${p.lastStatus}\n")
        }
        prefs.edit().putString(KEY_PAIRINGS, sb.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "sync_pairings_prefs"
        private const val KEY_PAIRINGS = "saved_pairings_v2"
    }
}
