package com.solomonprime.tricorder.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.PipeLeakViewModel

/**
 * PIPE LEAK DETECTOR screen
 *
 * Tab label: "PIPE"
 * Uses acoustic 50-500 Hz energy + magnetic variance to detect pipe leaks.
 */
@Composable
fun PipeLeakScreen(viewModel: PipeLeakViewModel = viewModel()) {
    val leakAlert by viewModel.leakAlert.collectAsState()
    val leakProb by viewModel.leakProbability.collectAsState()
    val acousticEnergy by viewModel.acousticEnergyLow.collectAsState()
    val magVariance by viewModel.magneticVariance.collectAsState()
    val calibrated by viewModel.baselineCalibrated.collectAsState()

    // Start scanning on entry, stop on leave
    DisposableEffect(Unit) {
        viewModel.startScan()
        onDispose { viewModel.stopScan() }
    }

    // Pulse animation for MEDIUM / HIGH alerts
    val shouldPulse = leakAlert == "MEDIUM" || leakAlert == "HIGH"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (shouldPulse) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val alertColor = when (leakAlert) {
        "HIGH" -> LcarsRed
        "MEDIUM" -> LcarsOrange
        else -> LcarsBlue // SCANNING & LOW
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // LCARS header
        LcarsSectionHeader(title = "PIPE LEAK DETECTOR", color = LcarsBlue)

        Spacer(modifier = Modifier.height(16.dp))

        // Scanning indicator
        LcarsScanningIndicator(isActive = !calibrated, color = LcarsBlue)

        Spacer(modifier = Modifier.height(12.dp))

        // Calibration status
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (calibrated) LcarsBlue.copy(alpha = 0.3f) else LcarsOrange.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (calibrated) "CALIBRATED" else "CALIBRATING...",
                color = if (calibrated) LcarsBlue else LcarsOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large central alert display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(alertColor.copy(alpha = 0.2f))
                .alpha(pulseAlpha),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = leakAlert,
                    color = alertColor,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "LEAK STATUS",
                    color = alertColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Leak probability gauge
        LcarsGauge(
            value = leakProb,
            minValue = 0f,
            maxValue = 1f,
            label = "LEAK PROBABILITY",
            unit = " ",
            accentColor = alertColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sensor data cards
        LcarsDataCard(
            title = "ACOUSTIC\n50-500Hz",
            value = "%.1f".format(acousticEnergy),
            accentColor = LcarsBlue
        )

        LcarsDataCard(
            title = "MAG\nVARIANCE",
            value = "%.3f".format(magVariance),
            unit = "µT",
            accentColor = LcarsPink
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recalibrate button
        LcarsButton(
            text = "RECALIBRATE",
            onClick = { viewModel.recalibrate() },
            color = LcarsOrange
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Instructions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LcarsBlue.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "Place phone directly against pipe, floor, or wall. Hold still for calibration. HIGH reading may indicate water flow or leak.",
                    color = LcarsWhite.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Best used with external contact microphone for buried/concrete pipes.",
                    color = LcarsWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
