package com.solomonprime.tricorder.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomonprime.tricorder.data.ConnectedDeviceManager
import com.solomonprime.tricorder.ui.theme.*

@Composable
fun DashboardScreen(
    deviceManager: ConnectedDeviceManager? = null,
    sensorData: Map<String, Float> = emptyMap(),
    uptimeSeconds: Int = 0
) {
    val radarPoints = remember(sensorData) {
        sensorData.entries.mapIndexed { i, (_, v) ->
            (i * 60f) to (v / 100f).coerceIn(0f, 1f)
        }
    }

    LcarsScreenScaffold(title = "TRICORDER") {
        // Uptime flip counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SYSTEM UPTIME",
                    color = LcarsTan.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                LcarsFlipCounter(value = uptimeSeconds, digits = 6)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "STATUS: NOMINAL",
                    color = LcarsBlue,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                LcarsStatusGrid(rows = 2, cols = 4)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Central radar sweep
        LcarsRadarSweep(
            dataPoints = radarPoints,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.CenterHorizontally)
                .padding(8.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Surrounding data cards in LCARS-style curved arrangement
        val cardData = listOf(
            "ENV" to (sensorData["temperature"] ?: 22.5f),
            "GEO" to (sensorData["altitude"] ?: 125f),
            "ACO" to (sensorData["dbLevel"] ?: 42f),
            "RAD" to (sensorData["cpm"] ?: 18f),
            "BIO" to (sensorData["heartRate"] ?: 72f),
            "EM" to (sensorData["emField"] ?: 0.3f)
        )

        // Top row - 3 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            cardData.take(3).forEach { (label, value) ->
                LcarsDashCard(label = label, value = value)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Bottom row - 3 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            cardData.drop(3).forEach { (label, value) ->
                LcarsDashCard(label = label, value = value)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bar graph summary
        LcarsBarGraph(
            data = cardData.map { (l, v) -> l to (v / 100f).coerceIn(0f, 1f) }
        )
    }
}

@Composable
private fun LcarsDashCard(label: String, value: Float) {
    val color = LcarsPalette[label.hashCode().and(0x7FFFFFFF) % LcarsPalette.size]
    Box(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
            .background(LcarsDarkPanel)
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            LcarsAnimatedValue(value = value, color = color, decimalPlaces = 1)
        }
    }
}
