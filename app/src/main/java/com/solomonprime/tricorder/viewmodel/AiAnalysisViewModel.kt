package com.solomonprime.tricorder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AI Analysis ViewModel — Rule-based sensor analyst engine.
 *
 * Monitors all sensor ViewModels every 10 seconds and generates
 * natural-language insights with severity classification.
 */
class AiAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    data class AiInsight(
        val title: String,
        val body: String,
        val severity: String, // "info", "warning", "alert"
        val timestamp: Long,
        val sensorSource: String
    )

    private val _insights = MutableStateFlow<List<AiInsight>>(emptyList())
    val insights: StateFlow<List<AiInsight>> = _insights.asStateFlow()

    private val _currentSummary = MutableStateFlow("MONITORING... All readings nominal.")
    val currentSummary: StateFlow<String> = _currentSummary.asStateFlow()

    // --- Sensor references (set externally after VM creation) ---
    var radCpm: (() -> Float)? = null
    var acoDecibels: (() -> Float)? = null
    var acoFreqBand: (() -> String)? = null
    var envPressure: (() -> Float?)? = null
    var geoHeading: (() -> Float?)? = null
    var bioHeartRate: (() -> Int)? = null
    var pipeLeakScore: (() -> Float)? = null

    // Pressure history for drop detection
    private val pressureHistory = mutableListOf<Pair<Long, Float>>() // (timestamp, hPa)

    // Heading history for variance detection
    private val headingHistory = mutableListOf<Pair<Long, Float>>() // (timestamp, degrees)

    private var analysisRunning = false

    fun startAnalysis() {
        if (analysisRunning) return
        analysisRunning = true
        viewModelScope.launch {
            while (analysisRunning) {
                analyzeAllSensors()
                delay(10_000L)
            }
        }
    }

    fun stopAnalysis() {
        analysisRunning = false
    }

    fun clearInsights() {
        _insights.value = emptyList()
        _currentSummary.value = "MONITORING... All readings nominal."
    }

    private fun analyzeAllSensors() {
        val now = System.currentTimeMillis()
        val newInsights = mutableListOf<AiInsight>()

        // --- RAD: CPM > 30 ---
        radCpm?.invoke()?.let { cpm ->
            if (cpm > 30f) {
                newInsights += AiInsight(
                    title = "Elevated Radiation",
                    body = "CPM at ${cpm.toInt()}. Elevated background radiation detected. Possible source: medical equipment, granite, or cosmic ray burst. Stay alert.",
                    severity = if (cpm > 100f) "alert" else "warning",
                    timestamp = now,
                    sensorSource = "RAD"
                )
            }
        }

        // --- ACO: dB > 80 ---
        acoDecibels?.invoke()?.let { db ->
            if (db > 80f) {
                val band = acoFreqBand?.invoke() ?: "unknown"
                newInsights += AiInsight(
                    title = "High Acoustic Energy",
                    body = "Sound level at ${db.toInt()} dB in $band range. Possible source: machinery, HVAC, or structural vibration.",
                    severity = if (db > 100f) "alert" else "warning",
                    timestamp = now,
                    sensorSource = "ACO"
                )
            }
        }

        // --- ENV: Pressure drop > 5 hPa in 10 min ---
        envPressure?.invoke()?.let { pressure ->
            pressureHistory.add(now to pressure)
            // Trim entries older than 10 minutes
            pressureHistory.removeAll { (ts, _) -> now - ts > 600_000L }
            if (pressureHistory.size >= 2) {
                val oldest = pressureHistory.first().second
                val drop = oldest - pressure
                if (drop > 5f) {
                    newInsights += AiInsight(
                        title = "Rapid Pressure Drop",
                        body = "Barometric pressure dropped ${String.format("%.1f", drop)} hPa in ${((now - pressureHistory.first().first) / 60_000)}min. Possible approaching storm system.",
                        severity = "warning",
                        timestamp = now,
                        sensorSource = "ENV"
                    )
                }
            }
        }

        // --- GEO: Magnetic heading variance > 15° in 5s ---
        geoHeading?.invoke()?.let { heading ->
            headingHistory.add(now to heading)
            headingHistory.removeAll { (ts, _) -> now - ts > 5_000L }
            if (headingHistory.size >= 2) {
                val min = headingHistory.minOf { it.second }
                val max = headingHistory.maxOf { it.second }
                val variance = max - min
                if (variance > 15f) {
                    newInsights += AiInsight(
                        title = "Magnetic Interference",
                        body = "Heading variance of ${String.format("%.1f", variance)}° in 5s. Nearby motors, electrical panels, or buried cables possible.",
                        severity = "warning",
                        timestamp = now,
                        sensorSource = "GEO"
                    )
                }
            }
        }

        // --- BIO: Heart rate > 100 ---
        bioHeartRate?.invoke()?.let { hr ->
            if (hr > 100) {
                newInsights += AiInsight(
                    title = "Elevated Heart Rate",
                    body = "Heart rate at $hr BPM. Elevated heart rate detected. Consider reducing physical activity.",
                    severity = if (hr > 150) "alert" else "warning",
                    timestamp = now,
                    sensorSource = "BIO"
                )
            }
        }

        // --- PIPE: leakScore > 0.5 ---
        pipeLeakScore?.invoke()?.let { score ->
            if (score > 0.5f) {
                newInsights += AiInsight(
                    title = "Possible Leak Detected",
                    body = "Leak probability at ${(score * 100).toInt()}%. Acoustic/magnetic signature consistent with water flow or leak detected.",
                    severity = if (score > 0.8f) "alert" else "warning",
                    timestamp = now,
                    sensorSource = "PIPE"
                )
            }
        }

        // Merge new insights, cap at 10
        if (newInsights.isNotEmpty()) {
            _insights.value = (newInsights + _insights.value).take(10)
        }

        // Update summary
        updateSummary()
    }

    private fun updateSummary() {
        val current = _insights.value
        if (current.isEmpty()) {
            _currentSummary.value = "MONITORING... All readings nominal."
            return
        }

        val alertCount = current.count { it.severity == "alert" }
        val warningCount = current.count { it.severity == "warning" }
        val sources = current.map { it.sensorSource }.distinct().joinToString(", ")

        _currentSummary.value = when {
            alertCount > 0 -> "⚠ $alertCount ALERT${if (alertCount > 1) "S" else ""}, $warningCount warning${if (warningCount != 1) "s" else ""} — Active sensors: $sources"
            warningCount > 0 -> "$warningCount warning${if (warningCount != 1) "s" else ""} detected — Sensors: $sources"
            else -> "MONITORING... All readings nominal."
        }
    }
}
