package com.solomonprime.tricorder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.RadiationViewModel

@Composable
fun RadiationScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: RadiationViewModel = viewModel()
) {
    val cpm by viewModel.cpm.collectAsState()
    val microSievert by viewModel.microSievert.collectAsState()
    val alertLevel by viewModel.alertLevel.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // Alert level color mapping using LCARS colors
    val alertColor by animateColorAsState(
        targetValue = when (alertLevel) {
            "ALERT" -> LcarsRed
            "ELEVATED" -> LcarsOrange
            else -> LcarsBlue  // NOMINAL - using blue as green equivalent in LCARS
        },
        animationSpec = tween(300),
        label = "alertColor"
    )

    // Auto-start scanning on composition
    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LcarsSectionHeader(title = "Radiation Analysis", color = LcarsOrange)

        LcarsScanningIndicator(isActive = isScanning, color = alertColor)

        Spacer(modifier = Modifier.height(16.dp))

        // Large CPM display (prominent)
        LargeCpmDisplay(
            cpm = cpm,
            accentColor = alertColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // µSv/hr reading
        LcarsDataCard(
            title = "Dosage",
            value = "%.4f".format(microSievert),
            unit = "µSv/hr",
            accentColor = LcarsPurple
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Alert level indicator
        AlertLevelIndicator(
            alertLevel = alertLevel,
            color = alertColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Scan control button
        ScanControlButton(
            isScanning = isScanning,
            onToggle = {
                if (isScanning) {
                    viewModel.stopScan()
                } else {
                    viewModel.startScan()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sensor status
        LcarsSectionHeader(title = "Detector Status", color = LcarsTan)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LcarsStatusIndicator(label = "GEIGER", isActive = isScanning)
            LcarsStatusIndicator(label = "CAL", isActive = true)
            LcarsStatusIndicator(label = "LINK", isActive = isScanning)
        }
    }
}

/**
 * Large prominent CPM display in LCARS style
 */
@Composable
private fun LargeCpmDisplay(
    cpm: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COUNTS PER MINUTE",
            color = accentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = cpm.toString(),
            color = LcarsWhite,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "CPM",
            color = LcarsWhite.copy(alpha = 0.7f),
            fontSize = 18.sp
        )
    }
}

/**
 * Alert level indicator with animated color
 */
@Composable
private fun AlertLevelIndicator(
    alertLevel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Label
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "STATUS",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Alert level value
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = alertLevel,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Animated indicator
        AnimatedAlertIndicator(
            isActive = alertLevel != "NOMINAL",
            color = color
        )

        // Right cap
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(color)
        )
    }
}

/**
 * Animated pulsing indicator for elevated/alert states
 */
@Composable
private fun AnimatedAlertIndicator(
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (!isActive) {
        Spacer(modifier = modifier.width(8.dp))
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertAlpha"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = alpha))
    )
}

/**
 * Scan control toggle button
 */
@Composable
private fun ScanControlButton(
    isScanning: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor = if (isScanning) LcarsRed else LcarsBlue

    Button(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (isScanning) "STOP SCAN" else "START SCAN",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LcarsBlack
        )
    }
}
