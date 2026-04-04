package com.solomonprime.tricorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.GeophysicalViewModel

@Composable
fun GeophysicalScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: GeophysicalViewModel = viewModel()
) {
    val location by viewModel.location.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val altitude by viewModel.altitude.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val heading by viewModel.heading.collectAsState()
    
    val accelerometerData by viewModel.accelerometerData.collectAsState()
    val magnetometerData by viewModel.magnetometerData.collectAsState()
    val gyroscopeData by viewModel.gyroscopeData.collectAsState()
    val gravityData by viewModel.gravityData.collectAsState()
    val rotationData by viewModel.rotationData.collectAsState()
    
    val hasGyroscope by viewModel.hasGyroscope.collectAsState()
    val hasGravity by viewModel.hasGravitySensor.collectAsState()
    val hasRotation by viewModel.hasRotationVector.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LcarsSectionHeader(title = "Geophysical Analysis", color = LcarsBlue)
        
        LcarsScanningIndicator(isActive = true, color = LcarsBlue)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Location data
        LcarsDataCard(
            title = "Latitude",
            value = latitude?.let { "%.6f".format(it) } ?: "---",
            unit = "°",
            accentColor = LcarsBlue
        )
        
        LcarsDataCard(
            title = "Longitude",
            value = longitude?.let { "%.6f".format(it) } ?: "---",
            unit = "°",
            accentColor = LcarsBlue
        )
        
        LcarsDataCard(
            title = "Altitude",
            value = altitude?.let { "%.1f".format(it) } ?: "---",
            unit = "m",
            accentColor = LcarsPurple
        )
        
        LcarsDataCard(
            title = "Velocity",
            value = speed?.let { "%.1f".format(it * 3.6f) } ?: "---",
            unit = "km/h",
            accentColor = LcarsOrange
        )
        
        LcarsDataCard(
            title = "Heading",
            value = heading?.let { "%.0f°  %s".format(it, compassDirection(it)) } ?: "---",
            unit = "",
            accentColor = LcarsTan
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Accelerometer
        LcarsAxisDisplay(
            title = "Accelerometer",
            x = accelerometerData[0],
            y = accelerometerData[1],
            z = accelerometerData[2],
            unit = "m/s²",
            accentColor = LcarsOrange
        )
        
        // Magnetometer
        LcarsAxisDisplay(
            title = "Magnetometer",
            x = magnetometerData[0],
            y = magnetometerData[1],
            z = magnetometerData[2],
            unit = "μT",
            accentColor = LcarsPink
        )
        
        // Gyroscope (if available)
        if (hasGyroscope) {
            LcarsAxisDisplay(
                title = "Gyroscope",
                x = gyroscopeData[0],
                y = gyroscopeData[1],
                z = gyroscopeData[2],
                unit = "rad/s",
                accentColor = LcarsPurple
            )
        }
        
        // Gravity (if available)
        if (hasGravity) {
            LcarsAxisDisplay(
                title = "Gravity Vector",
                x = gravityData[0],
                y = gravityData[1],
                z = gravityData[2],
                unit = "m/s²",
                accentColor = LcarsBlue
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sensor status
        LcarsSectionHeader(title = "Motion Sensors", color = LcarsTan)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LcarsStatusIndicator(label = "ACCEL", isActive = true)
            LcarsStatusIndicator(label = "MAG", isActive = true)
            LcarsStatusIndicator(label = "GYRO", isActive = hasGyroscope)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LcarsStatusIndicator(label = "GRAV", isActive = hasGravity)
            LcarsStatusIndicator(label = "ROT", isActive = hasRotation)
            LcarsStatusIndicator(label = "GPS", isActive = latitude != null)
        }
    }
}

private fun compassDirection(degrees: Float): String {
    val normalized = ((degrees % 360f) + 360f) % 360f
    return when {
        normalized < 22.5f  -> "N"
        normalized < 67.5f  -> "NE"
        normalized < 112.5f -> "E"
        normalized < 157.5f -> "SE"
        normalized < 202.5f -> "S"
        normalized < 247.5f -> "SW"
        normalized < 292.5f -> "W"
        normalized < 337.5f -> "NW"
        else                -> "N"
    }
}
