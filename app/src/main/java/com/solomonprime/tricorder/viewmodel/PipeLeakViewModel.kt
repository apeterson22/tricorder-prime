package com.solomonprime.tricorder.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Pipe Leak Detector ViewModel
 *
 * Combines acoustic energy in the 50-500 Hz band with magnetic field variance
 * to estimate the probability of a water leak in a pressurized pipe.
 *
 * Scientific basis:
 * - Turbulent flow and pipe vibration from leaks produce broadband noise (50-1500 Hz)
 * - Magnetometer detects subtle flux changes near water-filled ferrous/copper pipes
 * - Phone must be placed directly against the pipe, wall, or floor
 */
class PipeLeakViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    // Audio config (matches AcousticViewModel pattern)
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var analysisJob: Job? = null

    // Sensor
    private var sensorManager: SensorManager? = null
    private var magSensor: Sensor? = null

    // --- Exposed state ---

    private val _acousticEnergyLow = MutableStateFlow(0f)
    val acousticEnergyLow: StateFlow<Float> = _acousticEnergyLow.asStateFlow()

    private val _magneticVariance = MutableStateFlow(0f)
    val magneticVariance: StateFlow<Float> = _magneticVariance.asStateFlow()

    private val _leakProbability = MutableStateFlow(0f)
    val leakProbability: StateFlow<Float> = _leakProbability.asStateFlow()

    private val _leakAlert = MutableStateFlow("SCANNING")
    val leakAlert: StateFlow<String> = _leakAlert.asStateFlow()

    private val _baselineCalibrated = MutableStateFlow(false)
    val baselineCalibrated: StateFlow<Boolean> = _baselineCalibrated.asStateFlow()

    // --- Internal ---

    // Baseline values captured during calibration
    private var baselineAcoustic = 0f
    private var baselineMagnetic = 0f
    private var calibrationStartMs = 0L
    private val calibrationDurationMs = 5000L

    // Rolling magnetic magnitude history (last ~3 seconds at sensor rate)
    private val magHistory = mutableListOf<Float>()
    private val magHistoryMaxSize = 150 // ~3s at 50 Hz

    // Simple high-pass state for bandpass approximation
    private var prevSample = 0f

    // ---- Lifecycle ----

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (recordingJob?.isActive == true) return

        _baselineCalibrated.value = false
        _leakAlert.value = "SCANNING"
        calibrationStartMs = System.currentTimeMillis()
        baselineAcoustic = 0f
        baselineMagnetic = 0f
        magHistory.clear()

        // Start magnetometer
        sensorManager = getApplication<Application>()
            .getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        magSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Start audio
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                recordingJob = viewModelScope.launch(Dispatchers.IO) {
                    val buffer = ShortArray(bufferSize)
                    // Accumulate calibration samples
                    val calAcousticSamples = mutableListOf<Float>()

                    while (isActive) {
                        val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (readSize > 0) {
                            val energy = computeBandEnergy(buffer, readSize)
                            _acousticEnergyLow.value = energy

                            val elapsed = System.currentTimeMillis() - calibrationStartMs
                            if (!_baselineCalibrated.value && elapsed < calibrationDurationMs) {
                                calAcousticSamples.add(energy)
                            } else if (!_baselineCalibrated.value) {
                                // Calibration complete
                                baselineAcoustic = if (calAcousticSamples.isNotEmpty())
                                    calAcousticSamples.average().toFloat() else 1f
                                if (baselineAcoustic < 1e-6f) baselineAcoustic = 1e-6f
                                synchronized(magHistory) {
                                    baselineMagnetic = magStdDev()
                                    if (baselineMagnetic < 1e-6f) baselineMagnetic = 1e-6f
                                }
                                _baselineCalibrated.value = true
                            }

                            if (_baselineCalibrated.value) {
                                updateLeakScore()
                            }
                        }
                        delay(50)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopScan() {
        recordingJob?.cancel()
        recordingJob = null
        analysisJob?.cancel()
        analysisJob = null
        sensorManager?.unregisterListener(this)
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _leakAlert.value = "SCANNING"
    }

    fun recalibrate() {
        stopScan()
        startScan()
    }

    // ---- Audio processing ----

    /**
     * Compute RMS energy in the 50-500 Hz band using a simple bandpass approximation:
     * - High-pass via first-order difference (removes DC / sub-50 Hz)
     * - Low-pass via running average of adjacent differences (attenuates > ~500 Hz)
     */
    private fun computeBandEnergy(buffer: ShortArray, size: Int): Float {
        var sum = 0.0
        var prev = prevSample
        var prevHp = 0f

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()
            // High-pass: difference filter removes DC & very low freqs
            val hp = sample - prev * 0.98f
            prev = sample
            // Simple low-pass: average with previous high-passed sample (attenuates high freqs)
            val bp = (hp + prevHp) * 0.5f
            prevHp = hp
            sum += bp * bp
        }
        prevSample = prev
        return sqrt(sum / size).toFloat()
    }

    // ---- Magnetometer ----

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_MAGNETIC_FIELD) return
        val mag = sqrt(
            event.values[0] * event.values[0] +
            event.values[1] * event.values[1] +
            event.values[2] * event.values[2]
        )
        synchronized(magHistory) {
            magHistory.add(mag)
            if (magHistory.size > magHistoryMaxSize) magHistory.removeAt(0)
            _magneticVariance.value = magStdDev()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /** Standard deviation of magnetic magnitude history */
    private fun magStdDev(): Float {
        if (magHistory.size < 2) return 0f
        val mean = magHistory.average().toFloat()
        val variance = magHistory.sumOf { ((it - mean) * (it - mean)).toDouble() } / magHistory.size
        return sqrt(variance).toFloat()
    }

    // ---- Leak scoring ----

    private fun updateLeakScore() {
        // Normalize against baseline
        val normAcoustic = (_acousticEnergyLow.value / baselineAcoustic).coerceIn(0f, 10f) / 10f
        val normMagnetic = (_magneticVariance.value / baselineMagnetic).coerceIn(0f, 10f) / 10f

        val prob = (normAcoustic * 0.6f + normMagnetic * 0.4f).coerceIn(0f, 1f)
        _leakProbability.value = prob
        _leakAlert.value = when {
            prob > 0.6f -> "HIGH"
            prob > 0.3f -> "MEDIUM"
            else -> "LOW"
        }
    }

    // ---- Cleanup ----

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
