package com.solomonprime.tricorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.BioScannerViewModel

@Composable
fun BioScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: BioScannerViewModel = viewModel()
) {
    val heartRate by viewModel.heartRate.collectAsState()
    val bloodOxygen by viewModel.bloodOxygen.collectAsState()
    val skinTemp by viewModel.skinTemp.collectAsState()
    val stressIndex by viewModel.stressIndex.collectAsState()

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
        LcarsSectionHeader(title = "Biometric Analysis", color = LcarsOrange)

        LcarsScanningIndicator(isActive = true, color = LcarsOrange)

        Spacer(modifier = Modifier.height(16.dp))

        LcarsDataCard(
            title = "Heart Rate",
            value = "$heartRate",
            unit = "bpm",
            accentColor = LcarsRed
        )

        LcarsDataCard(
            title = "Blood Oxygen",
            value = "%.1f".format(bloodOxygen),
            unit = "%",
            accentColor = LcarsBlue
        )

        LcarsDataCard(
            title = "Skin Temp",
            value = "%.1f".format(skinTemp),
            unit = "°C",
            accentColor = LcarsOrange
        )

        LcarsDataCard(
            title = "Stress Index",
            value = "%.2f".format(stressIndex),
            unit = "",
            accentColor = LcarsPurple
        )
    }
}
