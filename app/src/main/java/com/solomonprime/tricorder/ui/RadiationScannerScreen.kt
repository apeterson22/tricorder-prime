package com.solomonprime.tricorder.ui
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.RadiationViewModel
import com.solomonprime.tricorder.viewmodel.RadiationViewModel.AlertLevel
import com.solomonprime.tricorder.viewmodel.RadiationViewModel.DataSource

/**
 * RadiationScannerScreen - LCARS-styled radiation scanner interface
 * Displays CPM, µSv/hr, field anomaly, and alert status with visual effects
 */
@Composable
fun RadiationScannerScreen(
    viewModel: RadiationViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Collect state flows
    val cpm by viewModel.cpm.collectAsState()
    val microSievert by viewModel.microSievert.collectAsState()
    val alertLevel by viewModel.alertLevel.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val fieldAnomaly by viewModel.fieldAnomaly.collectAsState()

    // Alert color pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "alert_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Determine alert color based on level
    val baseAlertColor = when (alertLevel) {
        AlertLevel.NOMINAL -> LcarsBlue
        AlertLevel.ELEVATED -> LcarsOrange
        AlertLevel.ALERT -> LcarsRed
    }

    // Apply pulsing only for non-nominal states
    val alertColor by animateColorAsState(
        targetValue = if (alertLevel != AlertLevel.NOMINAL) {
            baseAlertColor.copy(alpha = pulseAlpha)
        } else {
            baseAlertColor
        },
        label = "alert_color"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(16.dp)
    ) {
        // Header
        LcarsSectionHeader(
            title = "RADIATION SCANNER",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Row: Data Source Badge + Root Status + Connected Device
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Data Source Badge
            DataSourceBadge(dataSource = dataSource)

            // Root Status Indicator
            RootStatusIndicator(isRooted = viewModel.isRooted)
        }

        // Connected Device (if any)
        connectedDevice?.let { device ->
            Spacer(modifier = Modifier.height(8.dp))
            ConnectedDeviceDisplay(deviceName = device)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Readings Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // CPM Display
            LcarsDataCard(
                title = "CPM",
                value = String.format("%.1f", cpm),
                unit = "cpm",
                accentColor = alertColor,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // µSv/hr Display
            LcarsDataCard(
                title = "µSv/hr",
                value = String.format("%.4f", microSievert),
                unit = "µSv/hr",
                accentColor = alertColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Field Anomaly Gauge
        Text(
            text = "FIELD ANOMALY",
            color = LcarsTan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LcarsGauge(
            value = fieldAnomaly,
            minValue = 0f,
            maxValue = 1f,
            label = "Anomaly",
            unit = "%",
            accentColor = LcarsRed,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Alert Level Display
        AlertLevelDisplay(
            alertLevel = alertLevel,
            color = alertColor,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Scan Control Button
        LcarsButton(
            text = if (isScanning) "STOP SCAN" else "START SCAN",
            onClick = {
                if (isScanning) {
                    viewModel.stopScanning()
                } else {
                    viewModel.startScanning()
                }
            },
            color = if (isScanning) LcarsRed else LcarsBlue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

/**
 * Data Source Badge - Shows the current data source with LCARS styling
 */
@Composable
private fun DataSourceBadge(
    dataSource: DataSource,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (dataSource) {
        DataSource.LIVE_SENSOR -> "LIVE SENSOR" to LcarsBlue
        DataSource.BT_DEVICE -> "BT DEVICE" to LcarsPurple
        DataSource.ROOT_ENHANCED -> "ROOT ENHANCED" to LcarsOrange
        DataSource.SIMULATED -> "SIMULATED" to LcarsTan
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = LcarsBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Root Status Indicator - Shows whether device is rooted
 */
@Composable
private fun RootStatusIndicator(
    isRooted: Boolean,
    modifier: Modifier = Modifier
) {
    val (text, color) = if (isRooted) {
        "ROOT" to LcarsOrange
    } else {
        "STANDARD" to LcarsTan
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Connected Device Display - Shows paired Bluetooth dosimeter name
 */
@Composable
private fun ConnectedDeviceDisplay(
    deviceName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, LcarsPurple, RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CONNECTED:",
            color = LcarsPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = deviceName,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

/**
 * Alert Level Display - Shows current alert status with appropriate styling
 */
@Composable
private fun AlertLevelDisplay(
    alertLevel: AlertLevel,
    color: Color,
    modifier: Modifier = Modifier
) {
    val statusText = when (alertLevel) {
        AlertLevel.NOMINAL -> "NOMINAL - SAFE"
        AlertLevel.ELEVATED -> "ELEVATED - CAUTION"
        AlertLevel.ALERT -> "ALERT - DANGER"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(2.dp, color, RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
