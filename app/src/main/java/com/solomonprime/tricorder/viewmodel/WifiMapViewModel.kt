package com.solomonprime.tricorder.viewmodel

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.solomonprime.tricorder.model.WifiMapPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for WiFi signal mapping.
 * Combines WiFi scan results with GPS location to build
 * a spatial map of signal strength observations.
 */
class WifiMapViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val MAX_POINTS = 500
        private const val SCAN_INTERVAL_MS = 15_000L
    }

    private val wifiManager: WifiManager =
        application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    // --- Public state ---

    private val _points = MutableStateFlow<List<WifiMapPoint>>(emptyList())
    val points = _points.asStateFlow()

    private val _isMapping = MutableStateFlow(false)
    val isMapping = _isMapping.asStateFlow()

    private val _pointCount = MutableStateFlow(0)
    val pointCount = _pointCount.asStateFlow()

    // --- Internal state ---

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private val ringBuffer = ArrayDeque<WifiMapPoint>(MAX_POINTS)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { loc ->
                currentLatitude = loc.latitude
                currentLongitude = loc.longitude
            }
        }
    }

    @Suppress("DEPRECATION")
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION && _isMapping.value) {
                recordScanResults()
            }
        }
    }

    private var receiverRegistered = false

    // --- Public API ---

    fun startMapping() {
        if (_isMapping.value) return
        _isMapping.value = true
        startLocationUpdates()
        registerWifiReceiver()
        triggerScanLoop()
    }

    fun stopMapping() {
        _isMapping.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        unregisterWifiReceiver()
    }

    fun clearMap() {
        ringBuffer.clear()
        _points.value = emptyList()
        _pointCount.value = 0
    }

    // --- Internals ---

    private fun startLocationUpdates() {
        val context = getApplication<Application>().applicationContext
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun registerWifiReceiver() {
        if (receiverRegistered) return
        val context = getApplication<Application>().applicationContext
        context.registerReceiver(
            wifiScanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
        receiverRegistered = true
    }

    private fun unregisterWifiReceiver() {
        if (!receiverRegistered) return
        try {
            getApplication<Application>().applicationContext.unregisterReceiver(wifiScanReceiver)
        } catch (_: IllegalArgumentException) {}
        receiverRegistered = false
    }

    @Suppress("DEPRECATION")
    private fun triggerScanLoop() {
        viewModelScope.launch {
            while (_isMapping.value) {
                try {
                    wifiManager.startScan()
                } catch (_: Exception) {}
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun recordScanResults() {
        val lat = currentLatitude ?: return
        val lon = currentLongitude ?: return
        val now = System.currentTimeMillis()

        try {
            val results = wifiManager.scanResults
            for (scanResult in results) {
                val point = WifiMapPoint(
                    ssid = scanResult.SSID ?: "",
                    bssid = scanResult.BSSID ?: "",
                    rssi = scanResult.level,
                    latitude = lat,
                    longitude = lon,
                    timestamp = now
                )
                if (ringBuffer.size >= MAX_POINTS) {
                    ringBuffer.removeFirst()
                }
                ringBuffer.addLast(point)
            }
            _points.value = ringBuffer.toList()
            _pointCount.value = ringBuffer.size
        } catch (_: SecurityException) {}
    }

    override fun onCleared() {
        super.onCleared()
        stopMapping()
    }
}
