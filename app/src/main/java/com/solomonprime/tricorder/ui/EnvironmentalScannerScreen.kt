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
import com.solomonprime.tricorder.viewmodel.EnvironmentalViewModel

@Composable
fun EnvironmentalScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: EnvironmentalViewModel = viewModel()
) {
    val pressure by viewModel.pressure.collectAsState()
    val illuminance by viewModel.illuminance.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val humidity by viewModel.humidity.collectAsState()
    val proximity by viewModel.proximity.collectAsState()
    
    val hasPressure by viewModel.hasPressureSensor.collectAsState()
    val hasLight by viewModel.hasLightSensor.collectAsState()
    val hasTemp by viewModel.hasTemperatureSensor.collectAsState()
    val hasHumidity by viewModel.hasHumiditySensor.collectAsState()
    val hasProximity by viewModel.hasProximitySensor.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LcarsSectionHeader(title = "Environmental Analysis", color = LcarsOrange)
        
        LcarsScanningIndicator(isActive = true, color = LcarsOrange)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Atmospheric Pressure
        LcarsDataCard(
            title = "Atmo Pressure",
            value = pressure?.let { "%.1f".format(it) } ?: if (hasPressure) "---" else "N/A",
            unit = "hPa",
            accentColor = LcarsOrange
        )
        
        // Illuminance
        LcarsDataCard(
            title = "Illuminance",
            value = illuminance?.let { "%.0f".format(it) } ?: if (hasLight) "---" else "N/A",
            unit = "lux",
            accentColor = LcarsTan
        )
        
        // Temperature (if available)
        LcarsDataCard(
            title = "Ambient Temp",
            value = temperature?.let { "%.1f".format(it) } ?: if (hasTemp) "---" else "N/A",
            unit = "°C",
            accentColor = LcarsRed
        )
        
        // Humidity (if available)
        LcarsDataCard(
            title = "Rel Humidity",
            value = humidity?.let { "%.0f".format(it) } ?: if (hasHumidity) "---" else "N/A",
            unit = "%",
            accentColor = LcarsBlue
        )
        
        // Proximity
        LcarsDataCard(
            title = "Proximity",
            value = proximity?.let { "%.1f".format(it) } ?: if (hasProximity) "---" else "N/A",
            unit = "cm",
            accentColor = LcarsPurple
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sensor status indicators
        LcarsSectionHeader(title = "Sensor Status", color = LcarsBlue)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LcarsStatusIndicator(label = "BARO", isActive = hasPressure)
            LcarsStatusIndicator(label = "LUX", isActive = hasLight)
            LcarsStatusIndicator(label = "TEMP", isActive = hasTemp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LcarsStatusIndicator(label = "HUM", isActive = hasHumidity)
            LcarsStatusIndicator(label = "PROX", isActive = hasProximity)
        }
        
        // Pressure gauge if available
        if (hasPressure && pressure != null) {
            Spacer(modifier = Modifier.height(16.dp))
            LcarsGauge(
                value = pressure ?: 1013.25f,
                minValue = 950f,
                maxValue = 1050f,
                label = "Barometric Range",
                unit = "hPa",
                accentColor = LcarsOrange
            )
        }
        
        // Light gauge if available
        if (hasLight && illuminance != null) {
            Spacer(modifier = Modifier.height(8.dp))
            LcarsGauge(
                value = (illuminance ?: 0f).coerceAtMost(10000f),
                minValue = 0f,
                maxValue = 10000f,
                label = "Light Intensity",
                unit = "lux",
                accentColor = LcarsTan
            )
        }
    }
}
