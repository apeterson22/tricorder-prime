package com.solomonprime.tricorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val movementLevel by viewModel.movementLevel.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val connectedDevice by viewModel.connectedDeviceName.collectAsState()

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

        // Data source badge
        val (badgeText, badgeColor) = when (dataSource) {
            "PHONE_SENSORS" -> "● LIVE SENSORS" to LcarsOrange
            "BT_DEVICE" -> "● BT DEVICE" to LcarsBlue
            else -> "◌ SIMULATED" to LcarsTan
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Root status
            val rootColor = if (viewModel.isRooted) LcarsOrange else LcarsTan
            Text(
                text = if (viewModel.isRooted) "ROOT: ACTIVE" else "ROOT: INACTIVE",
                color = rootColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Connected device name
        if (connectedDevice != null) {
            Text(
                text = "LINKED: ${connectedDevice!!.uppercase()}",
                color = LcarsBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        LcarsScanningIndicator(isActive = true, color = LcarsOrange)

        Spacer(modifier = Modifier.height(12.dp))

        // Core biometric cards
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

        Spacer(modifier = Modifier.height(8.dp))

        // Movement level gauge
        LcarsGauge(
            value = movementLevel,
            minValue = 0f,
            maxValue = 1f,
            label = "Movement Level",
            unit = "",
            accentColor = LcarsBlue
        )

        // Paired devices list
        if (pairedDevices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LcarsSectionHeader(title = "Paired Devices", color = LcarsTan)
            pairedDevices.forEach { name ->
                val isConnected = name == connectedDevice
                Text(
                    text = (if (isConnected) "▸ " else "  ") + name,
                    color = if (isConnected) LcarsBlue else LcarsTan,
                    fontSize = 12.sp,
                    fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
        }
    }
}
