package com.solomonprime.tricorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomonprime.tricorder.model.WifiMapPoint
import com.solomonprime.tricorder.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.viewmodel.WifiMapViewModel

@Composable
fun WifiMapScreen(viewModel: WifiMapViewModel = viewModel()) {
    val points by viewModel.points.collectAsState()
    val isMapping by viewModel.isMapping.collectAsState()
    val pointCount by viewModel.pointCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WIFI SIGNAL MAP",
                color = LcarsOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$pointCount PTS",
                color = LcarsTan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isMapping) LcarsOrange else LcarsPurple,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isMapping) "MAPPING" else "IDLE",
                    color = LcarsBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Heatmap canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(LcarsBlack)
        ) {
            if (points.isEmpty()) {
                Text(
                    text = "NO DATA — Start mapping and move around",
                    color = LcarsTan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                WifiHeatmapCanvas(points = points)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LcarsButton(
                text = if (isMapping) "STOP MAPPING" else "START MAPPING",
                onClick = { if (isMapping) viewModel.stopMapping() else viewModel.startMapping() },
                color = if (isMapping) LcarsPurple else LcarsOrange,
                modifier = Modifier.weight(1f)
            )
            LcarsButton(
                text = "CLEAR MAP",
                onClick = { viewModel.clearMap() },
                color = LcarsTan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Walk around the area to map signal coverage",
            color = LcarsTan.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun WifiHeatmapCanvas(points: List<WifiMapPoint>) {
    // Pre-compute bounding box and strongest per network
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }

    val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
    val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

    // Strongest point per SSID for labeling
    val strongestPerNetwork = points
        .filter { it.ssid.isNotEmpty() }
        .groupBy { it.ssid }
        .mapValues { (_, pts) -> pts.maxByOrNull { it.rssi }!! }

    val orangeColor = LcarsOrange
    val tanColor = LcarsTan
    val purpleColor = LcarsPurple

    Canvas(modifier = Modifier.fillMaxSize()) {
        val padding = 24f
        val drawW = size.width - padding * 2
        val drawH = size.height - padding * 2

        // Draw each point
        for (point in points) {
            val nx = ((point.longitude - minLon) / lonRange).toFloat()
            val ny = (1f - ((point.latitude - minLat) / latRange).toFloat()) // invert Y
            val cx = padding + nx * drawW
            val cy = padding + ny * drawH

            val color = when {
                point.rssi > -50 -> orangeColor
                point.rssi > -70 -> tanColor
                else -> purpleColor
            }

            // Radius: stronger signal = larger (range ~6..20)
            val radius = ((point.rssi + 100).coerceIn(0, 60) / 60f * 14f + 6f)

            drawCircle(
                color = color.copy(alpha = 0.65f),
                radius = radius,
                center = Offset(cx, cy)
            )
        }

        // Draw SSID labels at strongest points
        for ((ssid, point) in strongestPerNetwork) {
            val nx = ((point.longitude - minLon) / lonRange).toFloat()
            val ny = (1f - ((point.latitude - minLat) / latRange).toFloat())
            val cx = padding + nx * drawW
            val cy = padding + ny * drawH

            drawContext.canvas.nativeCanvas.drawText(
                ssid,
                cx + 14f,
                cy - 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(0xFF, 0x99, 0x00) // LcarsOrange approx
                    textSize = 28f
                    isAntiAlias = true
                }
            )
        }
    }
}
