package com.solomonprime.tricorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class BioScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val _heartRate = MutableStateFlow(72)
    val heartRate = _heartRate.asStateFlow()

    private val _bloodOxygen = MutableStateFlow(97.5f)
    val bloodOxygen = _bloodOxygen.asStateFlow()

    private val _skinTemp = MutableStateFlow(36.6f)
    val skinTemp = _skinTemp.asStateFlow()

    private val _stressIndex = MutableStateFlow(0.3f)
    val stressIndex = _stressIndex.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            while (true) {
                _heartRate.value = (_heartRate.value + Random.nextInt(-3, 4)).coerceIn(60, 100)
                _bloodOxygen.value = (_bloodOxygen.value + Random.nextFloat() * 0.6f - 0.3f).coerceIn(95.0f, 99.9f)
                _skinTemp.value = (_skinTemp.value + Random.nextFloat() * 0.2f - 0.1f).coerceIn(36.0f, 37.5f)
                _stressIndex.value = (_stressIndex.value + Random.nextFloat() * 0.1f - 0.05f).coerceIn(0.0f, 1.0f)
                delay(2000L)
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
