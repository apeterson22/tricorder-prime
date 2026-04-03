package com.solomonprime.tricorder.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sqrt
import kotlin.random.Random

class BioScannerViewModel(application: Application) : AndroidViewModel(application) {

    // --- Root detection ---
    val isRooted: Boolean

    // --- Sensor flows ---
    private val _heartRate = MutableStateFlow(0)
    val heartRate = _heartRate.asStateFlow()

    private val _bloodOxygen = MutableStateFlow(97.5f)
    val bloodOxygen = _bloodOxygen.asStateFlow()

    private val _skinTemp = MutableStateFlow(0f)
    val skinTemp = _skinTemp.asStateFlow()

    private val _stressIndex = MutableStateFlow(0.3f)
    val stressIndex = _stressIndex.asStateFlow()

    private val _movementLevel = MutableStateFlow(0f)
    val movementLevel = _movementLevel.asStateFlow()

    // --- Data source ---
    private val _dataSource = MutableStateFlow("SIMULATED")
    val dataSource = _dataSource.asStateFlow()

    // --- Bluetooth ---
    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName = _connectedDeviceName.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<String>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    // --- Internal state ---
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var hasHardwareHeartRate = false
    private var hasBtDevice = false
    private var scanJob: Job? = null
    private var simulationJob: Job? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    val hr = event.values[0].toInt()
                    if (hr > 0) _heartRate.value = hr
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    // Normalize magnitude relative to gravity (~9.81)
                    val mag = sqrt(x * x + y * y + z * z)
                    val deviation = kotlin.math.abs(mag - 9.81f) / 9.81f
                    _movementLevel.value = deviation.coerceIn(0f, 1f)
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        // Root detection
        isRooted = detectRoot()

        // Check for BT fitness devices
        detectBluetoothDevices()

        // Check hardware heart rate sensor
        hasHardwareHeartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null

        // Determine data source
        _dataSource.value = when {
            hasBtDevice -> "BT_DEVICE"
            hasHardwareHeartRate -> "PHONE_SENSORS"
            else -> "SIMULATED"
        }

        // Read battery temperature as skin temp proxy
        readBatteryTemperature()
    }

    private fun detectRoot(): Boolean {
        try {
            if (File("/system/xbin/su").exists() || File("/system/bin/su").exists()) return true
        } catch (_: Exception) {}
        try {
            if (Build.TAGS?.contains("test-keys") == true) return true
        } catch (_: Exception) {}
        try {
            // su exec skipped — blocks indefinitely on stock devices
        } catch (_: Exception) {}
        return false
    }

    @SuppressLint("MissingPermission")
    private fun detectBluetoothDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
            val bonded = adapter.bondedDevices ?: return
            val fitnessKeywords = listOf("polar", "garmin", "fitbit", "mi band", "band")
            val allPaired = bonded.mapNotNull { it.name }
            _pairedDevices.value = allPaired

            val matched = allPaired.firstOrNull { name ->
                fitnessKeywords.any { name.lowercase().contains(it) }
            }
            if (matched != null) {
                hasBtDevice = true
                _connectedDeviceName.value = matched
            }
        } catch (_: SecurityException) {
            // Missing BLUETOOTH_CONNECT permission
        } catch (_: Exception) {}
    }

    private fun readBatteryTemperature() {
        try {
            val ctx = getApplication<Application>()
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            if (tempTenths > 0) {
                _skinTemp.value = tempTenths / 10f
            }
        } catch (_: Exception) {}
    }

    fun startScan() {
        if (scanJob?.isActive == true) return

        when (_dataSource.value) {
            "PHONE_SENSORS" -> startLiveSensors()
            "BT_DEVICE" -> startBtSimulation()
            else -> startSimulation()
        }
    }

    private fun startLiveSensors() {
        // Register hardware sensors
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Periodic updates for derived values
        scanJob = viewModelScope.launch {
            // Initialize heart rate if sensor hasn't fired yet
            if (_heartRate.value == 0) _heartRate.value = 72
            while (true) {
                readBatteryTemperature()
                // Derive stress from heart rate + movement
                val hrFactor = ((_heartRate.value - 60f) / 40f).coerceIn(0f, 1f)
                val mvFactor = _movementLevel.value
                _stressIndex.value = ((hrFactor * 0.7f + mvFactor * 0.3f)).coerceIn(0f, 1f)
                // Blood oxygen stays relatively stable
                _bloodOxygen.value = (_bloodOxygen.value + Random.nextFloat() * 0.4f - 0.2f).coerceIn(95f, 99.9f)
                delay(2000L)
            }
        }
    }

    private fun startBtSimulation() {
        // Simulate data as if reading from BT device
        scanJob = viewModelScope.launch {
            _heartRate.value = 72
            while (true) {
                _heartRate.value = (_heartRate.value + Random.nextInt(-2, 3)).coerceIn(55, 110)
                _bloodOxygen.value = (_bloodOxygen.value + Random.nextFloat() * 0.4f - 0.2f).coerceIn(95f, 99.9f)
                readBatteryTemperature()
                _stressIndex.value = (_stressIndex.value + Random.nextFloat() * 0.08f - 0.04f).coerceIn(0f, 1f)
                _movementLevel.value = (_movementLevel.value + Random.nextFloat() * 0.1f - 0.05f).coerceIn(0f, 1f)
                delay(2000L)
            }
        }
        // Also register accelerometer if available
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun startSimulation() {
        scanJob = viewModelScope.launch {
            _heartRate.value = 72
            if (_skinTemp.value == 0f) _skinTemp.value = 36.6f
            while (true) {
                _heartRate.value = (_heartRate.value + Random.nextInt(-3, 4)).coerceIn(60, 100)
                _bloodOxygen.value = (_bloodOxygen.value + Random.nextFloat() * 0.6f - 0.3f).coerceIn(95f, 99.9f)
                _skinTemp.value = (_skinTemp.value + Random.nextFloat() * 0.2f - 0.1f).coerceIn(36f, 37.5f)
                _stressIndex.value = (_stressIndex.value + Random.nextFloat() * 0.1f - 0.05f).coerceIn(0f, 1f)
                _movementLevel.value = (_movementLevel.value + Random.nextFloat() * 0.1f - 0.05f).coerceIn(0f, 1f)
                delay(2000L)
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        try {
            sensorManager.unregisterListener(sensorListener)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
