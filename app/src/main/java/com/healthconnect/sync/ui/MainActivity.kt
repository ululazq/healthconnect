package com.healthconnect.sync.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.healthconnect.sync.data.HealthConnectRepository
import com.healthconnect.sync.databinding.ActivityMainBinding
import com.healthconnect.sync.service.SyncForegroundService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: HealthConnectRepository

    private val requestPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        lifecycleScope.launch {
            if (grantedPermissions.containsAll(repository.permissions)) {
                updateStatus("All Health Connect permissions granted!")
                appendLog("Permissions granted successfully.")
            } else {
                updateStatus("Some permissions were denied.")
                appendLog("Permissions partially granted or denied.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = HealthConnectRepository(this)

        initViews()
        checkHealthConnectStatus()
    }

    private fun initViews() {
        binding.btnPermissions.setOnClickListener {
            if (!repository.isSupported()) {
                Toast.makeText(this, "Health Connect is not supported or not installed.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            requestPermissionLauncher.launch(repository.permissions)
        }

        binding.btnSync.setOnClickListener {
            startSyncService()
        }

        binding.btnInsertSample.setOnClickListener {
            insertSampleData()
        }
    }

    private fun checkHealthConnectStatus() {
        lifecycleScope.launch {
            if (!repository.isSupported()) {
                updateStatus("Health Connect is NOT available on this device.")
                appendLog("Health Connect SDK unavailable. Please install Health Connect from Google Play.")
                binding.btnPermissions.isEnabled = false
                binding.btnSync.isEnabled = false
                binding.btnInsertSample.isEnabled = false
                return@launch
            }

            val hasPerms = repository.hasAllPermissions()
            if (hasPerms) {
                updateStatus("Health Connect ready & permissions granted.")
                appendLog("Connected to Health Connect engine.")
            } else {
                updateStatus("Permissions needed.")
                appendLog("Ready. Please grant required permissions.")
            }
        }
    }

    private fun startSyncService() {
        appendLog("Triggering Foreground Sync Service…")
        val intent = Intent(this, SyncForegroundService::class.java)
        startService(intent)
        appendLog("Foreground sync dispatched.")
    }

    private fun insertSampleData() {
        lifecycleScope.launch {
            try {
                appendLog("Inserting sample records…")
                repository.insertSampleSteps(3500)
                repository.insertSampleWorkout("Morning Run")
                appendLog("Successfully inserted sample steps & workout.")
                val steps = repository.readRecentSteps()
                appendLog("Verification: Found ${steps.size} step entries in Health Connect.")
            } catch (e: Exception) {
                appendLog("Insert error: ${e.message}")
            }
        }
    }

    private fun updateStatus(text: String) {
        binding.tvStatus.text = text
    }

    private fun appendLog(msg: String) {
        binding.tvLogs.append("\n• $msg")
    }
}
