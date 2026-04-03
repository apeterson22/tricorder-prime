package com.solomonprime.tricorder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.EnvironmentalViewModel
import com.solomonprime.tricorder.viewmodel.GeophysicalViewModel
import com.solomonprime.tricorder.viewmodel.EmSpectrumViewModel
import com.solomonprime.tricorder.viewmodel.BioScannerViewModel
import com.solomonprime.tricorder.viewmodel.RadiationViewModel

@Composable
fun DashboardScreen(
    envViewModel: EnvironmentalViewModel = viewModel<EnvironmentalViewModel>(),
    geoViewModel: GeophysicalViewModel = viewModel<GeophysicalViewModel>(),
    emViewModel: EmSpectrumViewModel = viewModel<EmSpectrumViewModel>(),
    bioViewModel: BioScannerViewModel = viewModel<BioScannerViewModel>(),
    radViewModel: RadiationViewModel = viewModel<RadiationViewModel>()
) {
    // Collect state from ViewModels
    val temperature by envViewModel.temperature.collectAsState()
    val humidity by envViewModel.humidity.collectAsState()

    val altitude by geoViewModel.altitude.collectAsState()

    val magneticField by emViewModel.magneticField.collectAsState()

    val heartRate by bioViewModel.heartRate.collectAsState()
    val bloodOxygen by bioViewModel.bloodOxygen.collectAsState()

    val cpm by radViewModel.cpm.collectAsState()
    val microSievert by radViewModel.microSievert.collectAsState()
    val alertLevel by radViewModel.alertLevel.collectAsState()

    // Alert flash animation
    val isAlert = alertLevel != RadiationViewModel.AlertLevel.NOMINAL
    val infiniteTransition = rememberInfiniteTransition(label = "alert")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertFlash"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(8.dp)
    ) {
        // === HEADER BAR ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp))
                .background(LcarsOrange)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SENSOR FUSION",
                color = LcarsBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            LcarsScanningIndicator(isActive = true, color = LcarsBlue)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // === SENSOR CARDS GRID ===
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ENV
            item {
                LcarsDataCard(
                    title = "ENV TEMP",
                    value = temperature?.let { "%.1f".format(it) } ?: "--",
                    unit = "°C",
                    accentColor = LcarsOrange
                )
            }
            // GEO
            item {
                LcarsDataCard(
                    title = "GEO ALT",
                    value = altitude?.let { "%.0f".format(it) } ?: "--",
                    unit = "m",
                    accentColor = LcarsBlue
                )
            }
            // EM — magneticField is floatArrayOf; compute magnitude
            item {
                val magMagnitude = kotlin.math.sqrt(
                    magneticField[0] * magneticField[0] +
                    magneticField[1] * magneticField[1] +
                    magneticField[2] * magneticField[2]
                )
                LcarsDataCard(
                    title = "EM FIELD",
                    value = "%.1f".format(magMagnitude),
                    unit = "μT",
                    accentColor = LcarsPink
                )
            }
            // ACO
            item {
                LcarsDataCard(
                    title = "ACO STATUS",
                    value = "MIC ACTIVE",
                    unit = "",
                    accentColor = LcarsRed
                )
            }
            // BIO — heartRate is Int
            item {
                LcarsDataCard(
                    title = "BIO HR",
                    value = if (heartRate > 0) "$heartRate" else "--",
                    unit = "bpm",
                    accentColor = LcarsPurple
                )
            }
            // RAD
            item {
                LcarsDataCard(
                    title = "RAD CPM",
                    value = "%.0f".format(cpm),
                    unit = "cpm",
                    accentColor = LcarsTan
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // === SYSTEM HEALTH STRIP ===
        val healthColor = if (isAlert) LcarsRed.copy(alpha = alertAlpha) else LcarsBlue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(healthColor)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SYSTEM HEALTH",
                color = LcarsBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = if (isAlert) "⚠ ALERT: ${alertLevel.name}" else "ALL SENSORS NOMINAL",
                color = LcarsBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
