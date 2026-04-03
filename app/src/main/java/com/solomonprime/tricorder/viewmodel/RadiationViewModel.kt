package com.solomonprime.tricorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel for simulating radiation sensor data.
 * Provides CPM (Counts Per Minute), µSv/hr dosage, and alert levels.
 */
class RadiationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // Normal background radiation: 10-25 CPM
        private const val NORMAL_CPM_MIN = 10
        private const val NORMAL_CPM_MAX = 25
        
        // Spike probability and range
        private const val SPIKE_PROBABILITY = 0.08f
        private const val SPIKE_CPM_MIN = 40
        private const val SPIKE_CPM_MAX = 120
        
        // Conversion factor: ~0.08 µSv/hr per 100 CPM
        private const val MICROSIEVERT_PER_100_CPM = 0.08f
        
        // Alert thresholds (CPM)
        private const val ELEVATED_THRESHOLD = 30
        private const val ALERT_THRESHOLD = 60
        
        // Update interval
        private const val UPDATE_INTERVAL_MS = 1500L
    }

    // State Flows
    private val _cpm = MutableStateFlow(0)
    val cpm = _cpm.asStateFlow()

    private val _microSievert = MutableStateFlow(0f)
    val microSievert = _microSievert.asStateFlow()

    private val _alertLevel = MutableStateFlow("NOMINAL")
    val alertLevel = _alertLevel.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private var scanJob: Job? = null

    /**
     * Start radiation scanning simulation.
     */
    fun startScan() {
        if (_isScanning.value) return
        
        _isScanning.value = true
        scanJob = viewModelScope.launch {
            while (isActive && _isScanning.value) {
                updateRadiationData()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Stop radiation scanning.
     */
    fun stopScan() {
        _isScanning.value = false
        scanJob?.cancel()
        scanJob = null
    }

    /**
     * Simulate radiation sensor reading.
     */
    private fun updateRadiationData() {
        // Determine if this is a spike reading
        val isSpike = Random.nextFloat() < SPIKE_PROBABILITY
        
        val newCpm = if (isSpike) {
            Random.nextInt(SPIKE_CPM_MIN, SPIKE_CPM_MAX + 1)
        } else {
            Random.nextInt(NORMAL_CPM_MIN, NORMAL_CPM_MAX + 1)
        }
        
        _cpm.value = newCpm
        
        // Calculate µSv/hr from CPM
        _microSievert.value = (newCpm / 100f) * MICROSIEVERT_PER_100_CPM
        
        // Determine alert level
        _alertLevel.value = when {
            newCpm >= ALERT_THRESHOLD -> "ALERT"
            newCpm >= ELEVATED_THRESHOLD -> "ELEVATED"
            else -> "NOMINAL"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
