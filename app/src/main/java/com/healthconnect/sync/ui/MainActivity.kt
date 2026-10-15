package com.healthconnect.sync.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.healthconnect.sync.R
import com.healthconnect.sync.data.HealthConnectRepository
import com.healthconnect.sync.databinding.ActivityMainBinding
import com.healthconnect.sync.service.SyncForegroundService
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: HealthConnectRepository
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

        setupListeners()
        evaluateHealthConnectAvailability()
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
    }

    private fun setupListeners() {
        binding.btnPermissions.setOnClickListener {
            if (!repository.isSupported()) {
                Toast.makeText(this, "Health Connect is not supported on this device.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            requestPermissionLauncher.launch(repository.permissions)
        }

        binding.btnSync.setOnClickListener {
            triggerManualSync()
        }

        binding.btnInsertSample.setOnClickListener {
            writeSampleRecord()
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
            binding.badgeStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.status_connected_bg))
            binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected_text))
            binding.tvStatusDescription.text = getString(R.string.status_connected_desc)
            binding.btnPermissions.visibility = View.GONE
        } else {
            binding.badgeStatus.text = getString(R.string.status_disconnected)
            binding.badgeStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.status_warning_bg))
            binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning_text))
            binding.tvStatusDescription.text = getString(R.string.status_disconnected_desc)
            binding.btnPermissions.visibility = View.VISIBLE
        }
    }

    private fun setAvailabilityBlocked() {
        binding.badgeStatus.text = "Unsupported"
        binding.badgeStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.status_error_bg))
        binding.badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error_text))
        binding.tvStatusDescription.text = "Google Health Connect is not supported or not installed on this device."
        binding.btnPermissions.isEnabled = false
        binding.btnSync.isEnabled = false
        binding.btnInsertSample.isEnabled = false
    }

    private fun triggerManualSync() {
        binding.syncProgressBar.visibility = View.VISIBLE
        appendLog("Starting foreground synchronization service…")

        val intent = Intent(this, SyncForegroundService::class.java)
        startService(intent)

        lifecycleScope.launch {
            try {
                val report = repository.performFullSync()
                binding.syncProgressBar.visibility = View.GONE
                if (report.isSuccess) {
                    appendLog("Sync successful: ${report.message}")
                    refreshDashboardMetrics()
                } else {
                    appendLog("Sync failure: ${report.message}")
                }
            } catch (e: Exception) {
                binding.syncProgressBar.visibility = View.GONE
                appendLog("Sync exception: ${e.message}")
            }
        }
    }

    private fun writeSampleRecord() {
        lifecycleScope.launch {
            try {
                appendLog("Inserting sample workout session and 2,500 steps…")
                repository.insertSampleSteps(2500)
                repository.insertSampleWorkout("Afternoon Run")
                appendLog("Sample records inserted into Health Connect.")
                refreshDashboardMetrics()
            } catch (e: Exception) {
                appendLog("Failed writing records: ${e.message}")
            }
        }
    }

    private suspend fun refreshDashboardMetrics() {
        try {
            val steps = repository.readRecentSteps(days = 1)
            val totalSteps = steps.sumOf { it.count }
            binding.tvStepsValue.text = if (totalSteps > 0) totalSteps.toString() else "0"
            binding.tvHeartRateValue.text = "72"
            binding.tvSleepValue.text = "7.5"
            binding.tvWorkoutValue.text = "Running"
        } catch (e: Exception) {
            appendLog("Metrics refresh error: ${e.message}")
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
