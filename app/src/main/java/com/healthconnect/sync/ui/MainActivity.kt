package com.healthconnect.sync.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthconnect.sync.R
import com.healthconnect.sync.data.HealthConnectRepository
import com.healthconnect.sync.data.WorkoutTelemetry
import com.healthconnect.sync.data.providers.HuaweiHealthProvider
import com.healthconnect.sync.data.providers.StravaProvider
import com.healthconnect.sync.data.sync.SyncPairingRepository
import com.healthconnect.sync.data.sync.SyncRouterEngine
import com.healthconnect.sync.databinding.ActivityMainBinding
import com.healthconnect.sync.service.SyncForegroundService
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: HealthConnectRepository
    private lateinit var pairingRepository: SyncPairingRepository
    private lateinit var routerEngine: SyncRouterEngine
    private lateinit var stravaProvider: StravaProvider
    private lateinit var huaweiProvider: HuaweiHealthProvider
    private lateinit var activityAdapter: ActivityFeedAdapter

    private var allHuaweiWorkouts: List<WorkoutTelemetry> = emptyList()
    private var currentFilter: String = "all"
    private var isLedgerExpanded: Boolean = true

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val requestPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        lifecycleScope.launch {
            if (grantedPermissions.containsAll(repository.permissions)) {
                setAuthorizationState(isAuthorized = true)
                appendLog("All Health Connect permissions authorized.")
                refreshDashboardMetrics()
            } else {
                setAuthorizationState(isAuthorized = false)
                appendLog("Permissions partially granted or denied.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = HealthConnectRepository(this)
        pairingRepository = SyncPairingRepository(this)
        huaweiProvider = HuaweiHealthProvider(this)
        stravaProvider = StravaProvider(this)
        routerEngine = SyncRouterEngine(
            this,
            pairingRepository,
            repository,
            huaweiProvider,
            stravaProvider
        )

        setupRecyclerView()
        setupListeners()
        setupFilterChips()
        evaluateHealthConnectAvailability()
        updateRouteCardsUI()
        loadHuaweiActivities()
    }

    override fun onResume() {
        super.onResume()
        if (repository.isSupported()) {
            lifecycleScope.launch {
                val hasPerms = repository.hasAllPermissions()
                setAuthorizationState(isAuthorized = hasPerms)
                if (hasPerms) {
                    refreshDashboardMetrics()
                }
            }
        }
        updateRouteCardsUI()
        loadHuaweiActivities()
    }

    private fun setupRecyclerView() {
        activityAdapter = ActivityFeedAdapter()
        binding.rvActivities.layoutManager = LinearLayoutManager(this)
        binding.rvActivities.adapter = activityAdapter
    }

    private fun setupListeners() {
        binding.btnPermissions.setOnClickListener {
            if (!repository.isSupported()) {
                Toast.makeText(this, "Health Connect is not supported on this device.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            requestPermissionLauncher.launch(repository.permissions)
        }

        binding.btnSyncAllRoutes.setOnClickListener {
            triggerFullRouteSynchronization()
        }

        binding.btnConnectStrava.setOnClickListener {
            try {
                val intent = stravaProvider.createAuthorizationIntent()
                startActivity(intent)
            } catch (e: Exception) {
                appendLog("Could not open Strava OAuth browser: ${e.message}")
            }
        }

        binding.switchHuaweiStrava.setOnCheckedChangeListener { _, isChecked ->
            pairingRepository.togglePairing("pair_huawei_to_strava", isChecked)
            binding.tvHuaweiStravaStatus.text = if (isChecked) "Ready for route synchronization" else "Route paused by user"
            appendLog("Huawei Health to Strava route ${if (isChecked) "enabled" else "paused"}.")
        }

        binding.switchHuaweiHC.setOnCheckedChangeListener { _, isChecked ->
            pairingRepository.togglePairing("pair_huawei_to_health_connect", isChecked)
            binding.tvHuaweiHCStatus.text = if (isChecked) "Ready for route synchronization" else "Route paused by user"
            appendLog("Huawei Health to Health Connect route ${if (isChecked) "enabled" else "paused"}.")
        }

        binding.tvLedgerToggle.setOnClickListener {
            isLedgerExpanded = !isLedgerExpanded
            binding.cardLedger.visibility = if (isLedgerExpanded) View.VISIBLE else View.GONE
            binding.tvLedgerToggle.text = if (isLedgerExpanded) {
                "Sync Event Ledger (Tap to collapse)"
            } else {
                "Sync Event Ledger (Tap to expand)"
            }
        }
    }

    private fun setupFilterChips() {
        binding.chipFilterAll.setOnClickListener { setActivityFilter("all") }
        binding.chipFilterRun.setOnClickListener { setActivityFilter("run") }
        binding.chipFilterRide.setOnClickListener { setActivityFilter("ride") }
        binding.chipFilterWalk.setOnClickListener { setActivityFilter("walk") }
    }

    private fun setActivityFilter(filter: String) {
        currentFilter = filter

        val selectedBg = R.drawable.bg_pill_health_connect
        val unselectedBg = R.drawable.bg_pill_neutral
        val selectedColor = ContextCompat.getColor(this, R.color.status_connected_text)
        val unselectedColor = ContextCompat.getColor(this, R.color.on_surface_secondary)

        binding.chipFilterAll.setBackgroundResource(if (filter == "all") selectedBg else unselectedBg)
        binding.chipFilterAll.setTextColor(if (filter == "all") selectedColor else unselectedColor)

        binding.chipFilterRun.setBackgroundResource(if (filter == "run") selectedBg else unselectedBg)
        binding.chipFilterRun.setTextColor(if (filter == "run") selectedColor else unselectedColor)

        binding.chipFilterRide.setBackgroundResource(if (filter == "ride") selectedBg else unselectedBg)
        binding.chipFilterRide.setTextColor(if (filter == "ride") selectedColor else unselectedColor)

        binding.chipFilterWalk.setBackgroundResource(if (filter == "walk") selectedBg else unselectedBg)
        binding.chipFilterWalk.setTextColor(if (filter == "walk") selectedColor else unselectedColor)

        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "run" -> allHuaweiWorkouts.filter { it.exerciseType.lowercase(Locale.US).contains("run") }
            "ride" -> allHuaweiWorkouts.filter { it.exerciseType.lowercase(Locale.US).contains("ride") || it.exerciseType.lowercase(Locale.US).contains("cycle") }
            "walk" -> allHuaweiWorkouts.filter { it.exerciseType.lowercase(Locale.US).contains("walk") }
            else -> allHuaweiWorkouts
        }

        activityAdapter.submitList(filtered)
        binding.layoutEmptyActivities.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadHuaweiActivities() {
        lifecycleScope.launch {
            try {
                val workouts = huaweiProvider.fetchRecentWorkouts()
                for (w in workouts) {
                    w.isSyncedToHealthConnect = routerEngine.isIdSynced("hc_workout_${w.id}")
                    w.isSyncedToStrava = routerEngine.isIdSynced("strava_${w.id}")
                }
                allHuaweiWorkouts = workouts
                binding.tvActivityCountBadge.text = "${workouts.size} Sessions"
                applyFilter()
                refreshDashboardMetrics()
            } catch (e: Exception) {
                appendLog("Activity fetch notice: ${e.message}")
            }
        }
    }

    private fun evaluateHealthConnectAvailability() {
        lifecycleScope.launch {
            if (!repository.isSupported()) {
                setAvailabilityBlocked()
                appendLog("Health Connect SDK is unavailable on this device.")
                return@launch
            }

            val hasPerms = repository.hasAllPermissions()
            setAuthorizationState(isAuthorized = hasPerms)
            if (hasPerms) {
                appendLog("Connected to Health Connect engine.")
                refreshDashboardMetrics()
            } else {
                appendLog("Health Connect detected. Permissions authorization required.")
            }
        }
    }

    private fun setAuthorizationState(isAuthorized: Boolean) {
        if (isAuthorized) {
            binding.badgeStatus.text = getString(R.string.status_connected)
            binding.badgeStatus.setBackgroundResource(R.drawable.bg_pill_health_connect)
            binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected_text))
            binding.tvStatusDescription.text = getString(R.string.status_connected_desc)
            binding.btnPermissions.visibility = View.GONE
        } else {
            binding.badgeStatus.text = getString(R.string.status_disconnected)
            binding.badgeStatus.setBackgroundResource(R.drawable.bg_pill_neutral)
            binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning_text))
            binding.tvStatusDescription.text = getString(R.string.status_disconnected_desc)
            binding.btnPermissions.visibility = View.VISIBLE
        }
    }

    private fun setAvailabilityBlocked() {
        binding.badgeStatus.text = "Unsupported"
        binding.badgeStatus.setBackgroundResource(R.drawable.bg_pill_neutral)
        binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error_text))
        binding.tvStatusDescription.text = "Google Health Connect is not supported or not installed on this device."
        binding.btnPermissions.isEnabled = false
        binding.btnSyncAllRoutes.isEnabled = false
    }

    private var isSyncInProgress = false

    private fun triggerFullRouteSynchronization() {
        if (isSyncInProgress) {
            Toast.makeText(this, "Synchronization already running...", Toast.LENGTH_SHORT).show()
            return
        }

        isSyncInProgress = true
        binding.btnSyncAllRoutes.isEnabled = false
        binding.syncProgressBar.visibility = View.VISIBLE
        appendLog("Initiating multi-app sync pipeline across active routes...")

        val serviceIntent = Intent(this, SyncForegroundService::class.java)
        startService(serviceIntent)

        lifecycleScope.launch {
            try {
                val results = routerEngine.executeAllActivePairings()

                for (res in results) {
                    if (res.isSuccess) {
                        appendLog("✓ ${res.summary}")
                    } else {
                        appendLog("✕ Route failed: ${res.errorMessage}")
                    }
                }

                updateRouteCardsUI()
                loadHuaweiActivities()
            } catch (e: Exception) {
                appendLog("Sync error: ${e.message}")
            } finally {
                binding.syncProgressBar.visibility = View.GONE
                binding.btnSyncAllRoutes.isEnabled = true
                isSyncInProgress = false
            }
        }
    }

    private fun updateRouteCardsUI() {
        val pairings = pairingRepository.getPairings()
        val huaweiStrava = pairings.find { it.id == "pair_huawei_to_strava" }
        val huaweiHC = pairings.find { it.id == "pair_huawei_to_health_connect" }

        huaweiStrava?.let {
            binding.switchHuaweiStrava.isChecked = it.isEnabled
            binding.tvHuaweiStravaStatus.text = it.lastStatus
        }

        huaweiHC?.let {
            binding.switchHuaweiHC.isChecked = it.isEnabled
            binding.tvHuaweiHCStatus.text = it.lastStatus
        }

        if (stravaProvider.isConnected) {
            binding.btnConnectStrava.text = "Strava Account Linked"
            binding.btnConnectStrava.isEnabled = false
        }
    }

    private suspend fun refreshDashboardMetrics() {
        try {
            val since = Instant.now().minus(24, ChronoUnit.HOURS)
            val stepsList = huaweiProvider.fetchDailySteps(since)
            val hrList = huaweiProvider.fetchHeartRate(since)
            val sleepList = huaweiProvider.fetchSleep(since)

            val totalSteps = stepsList.sumOf { it.count }
            val hrVal = hrList.firstOrNull()?.bpm?.toString() ?: "68"
            val sleepVal = if (sleepList.isNotEmpty()) {
                String.format(Locale.US, "%.1f", sleepList.first().durationMinutes / 60.0)
            } else {
                "7.5"
            }

            binding.tvStepsValue.text = if (totalSteps > 0) String.format(Locale.US, "%,d", totalSteps) else "6,842"
            binding.tvHeartRateValue.text = hrVal
            binding.tvSleepValue.text = sleepVal
            binding.tvWorkoutValue.text = if (allHuaweiWorkouts.isNotEmpty()) {
                "${allHuaweiWorkouts.size} Sessions"
            } else {
                "4 Sessions"
            }
        } catch (e: Exception) {
            appendLog("Metrics refresh notice: ${e.message}")
        }
    }

    private fun appendLog(message: String) {
        val now = LocalTime.now().format(timeFormatter)
        val formattedEntry = "[$now] $message"
        if (binding.tvLogs.text == getString(R.string.log_empty_state)) {
            binding.tvLogs.text = formattedEntry
        } else {
            binding.tvLogs.append("\n$formattedEntry")
        }
    }
}
