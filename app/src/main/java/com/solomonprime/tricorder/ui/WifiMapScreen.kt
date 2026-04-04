package com.solomonprime.tricorder.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.model.WifiMapPoint
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.WifiMapViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WifiMapScreen(viewModel: WifiMapViewModel = viewModel()) {
    val points by viewModel.points.collectAsState()
    val isMapping by viewModel.isMapping.collectAsState()
    val pointCount by viewModel.pointCount.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val boundaries by viewModel.roomBoundaryPoints.collectAsState()
    val scale by viewModel.mapScale.collectAsState()
    val heading by viewModel.headingDegrees.collectAsState()

    // Gesture state for pan + pinch
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var gestureScale by remember { mutableFloatStateOf(1f) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        gestureScale = (gestureScale * zoomChange).coerceIn(0.5f, 4f)
        panOffset += offsetChange
    }

    // Pulsing animation for current position dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Stats
    val areaCovered = remember(points) { computeAreaCovered(points) }
    val strongestSsid = remember(points) {
        points.filter { it.ssid.isNotEmpty() }.maxByOrNull { it.rssi }?.ssid ?: "—"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(12.dp)
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
            Spacer(modifier = Modifier.weight(1f))
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

        Spacer(modifier = Modifier.height(4.dp))

        // Stats bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatLabel("POINTS", "$pointCount")
            StatLabel("AREA", String.format("%.1f m²", areaCovered))
            StatLabel("BEST", strongestSsid)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Map canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0A0A1A), RoundedCornerShape(8.dp))
                .transformable(state = transformState)
        ) {
            if (points.isEmpty() && !isMapping) {
                Text(
                    text = "NO DATA — Start mapping and move around",
                    color = LcarsTan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                WifiRoomMapCanvas(
                    points = points,
                    currentPos = currentPos,
                    boundaries = boundaries,
                    heading = heading,
                    scale = scale * gestureScale,
                    panOffset = panOffset,
                    pulseRadius = pulseRadius,
                    pulseAlpha = pulseAlpha
                )
            }

            // Zoom buttons overlay — top right
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LcarsButton(
                    text = "+",
                    onClick = { viewModel.zoomIn() },
                    color = LcarsTan,
                    modifier = Modifier.size(40.dp)
                )
                LcarsButton(
                    text = "−",
                    onClick = { viewModel.zoomOut() },
                    color = LcarsTan,
                    modifier = Modifier.size(40.dp)
                )
                LcarsButton(
                    text = "⟳",
                    onClick = {
                        viewModel.resetView()
                        gestureScale = 1f
                        panOffset = Offset.Zero
                    },
                    color = LcarsPurple,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                text = "CLEAR",
                onClick = { viewModel.clearMap() },
                color = LcarsTan,
                modifier = Modifier.weight(0.5f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = LcarsOrange, label = "> -50 dBm")
            LegendDot(color = LcarsTan, label = "-50 to -70")
            LegendDot(color = LcarsPurple, label = "< -70 dBm")
            LegendDot(color = LcarsBlue, label = "You")
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Walk slowly — mapping room boundaries from signal changes",
            color = LcarsTan.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = LcarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = label, color = LcarsTan.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = LcarsTan.copy(alpha = 0.8f), fontSize = 10.sp)
    }
}

@Composable
private fun WifiRoomMapCanvas(
    points: List<WifiMapPoint>,
    currentPos: Pair<Float, Float>,
    boundaries: List<Pair<Float, Float>>,
    heading: Float,
    scale: Float,
    panOffset: Offset,
    pulseRadius: Float,
    pulseAlpha: Float
) {
    val orangeColor = LcarsOrange
    val tanColor = LcarsTan
    val purpleColor = LcarsPurple
    val blueColor = LcarsBlue

    // Strongest point per SSID for labeling
    val strongestPerNetwork = remember(points) {
        points.filter { it.ssid.isNotEmpty() }
            .groupBy { it.ssid }
            .mapValues { (_, pts) -> pts.maxByOrNull { it.rssi }!! }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height
        val centerX = cw / 2f + panOffset.x
        val centerY = ch / 2f + panOffset.y

        // Pixels per meter
        val ppm = 40f * scale

        // --- Grid lines (1m spacing) ---
        val gridColor = Color(0xFF333344)
        val gridRange = 50 // ±50 meters
        for (i in -gridRange..gridRange) {
            val gx = centerX + i * ppm
            val gy = centerY + i * ppm
            if (gx in 0f..cw) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, ch), strokeWidth = 0.5f)
            }
            if (gy in 0f..ch) {
                drawLine(gridColor, Offset(0f, gy), Offset(cw, gy), strokeWidth = 0.5f)
            }
        }

        // --- Path trace (dotted line connecting scan points in order) ---
        if (points.size >= 2) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val x0 = centerX + p0.relativeX * ppm
                val y0 = centerY - p0.relativeY * ppm
                val x1 = centerX + p1.relativeX * ppm
                val y1 = centerY - p1.relativeY * ppm
                drawLine(
                    color = tanColor.copy(alpha = 0.3f),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
            }
        }

        // --- Wall / boundary indicators ---
        if (boundaries.size >= 2) {
            val sorted = boundaries.sortedBy { it.first * 1000 + it.second }
            for (i in 1 until sorted.size) {
                val (bx0, by0) = sorted[i - 1]
                val (bx1, by1) = sorted[i]
                // Only connect nearby boundary points (within 3m)
                val dx = bx1 - bx0
                val dy = by1 - by0
                if (dx * dx + dy * dy < 9f) {
                    drawLine(
                        color = orangeColor,
                        start = Offset(centerX + bx0 * ppm, centerY - by0 * ppm),
                        end = Offset(centerX + bx1 * ppm, centerY - by1 * ppm),
                        strokeWidth = 4f
                    )
                }
            }
        }
        // Also draw individual boundary markers
        for ((bx, by) in boundaries) {
            drawCircle(
                color = orangeColor.copy(alpha = 0.4f),
                radius = 5f,
                center = Offset(centerX + bx * ppm, centerY - by * ppm)
            )
        }

        // --- WiFi signal points ---
        for (point in points) {
            val px = centerX + point.relativeX * ppm
            val py = centerY - point.relativeY * ppm

            val color = when {
                point.rssi > -50 -> orangeColor
                point.rssi > -70 -> tanColor
                else -> purpleColor
            }

            val radius = ((point.rssi + 100).coerceIn(0, 60) / 60f * 12f + 4f) * scale.coerceIn(0.5f, 2f)

            drawCircle(
                color = color.copy(alpha = 0.6f),
                radius = radius,
                center = Offset(px, py)
            )
        }

        // --- SSID labels at strongest point per network ---
        for ((ssid, point) in strongestPerNetwork) {
            val px = centerX + point.relativeX * ppm
            val py = centerY - point.relativeY * ppm
            drawContext.canvas.nativeCanvas.drawText(
                ssid,
                px + 12f,
                py - 8f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(0xFF, 0x99, 0x00)
                    textSize = (24f * scale.coerceIn(0.6f, 1.5f))
                    isAntiAlias = true
                }
            )
        }

        // --- Current position: pulsing dot + heading arrow ---
        val curX = centerX + currentPos.first * ppm
        val curY = centerY - currentPos.second * ppm

        // Pulse ring
        drawCircle(
            color = blueColor.copy(alpha = pulseAlpha),
            radius = pulseRadius * scale.coerceIn(0.7f, 2f),
            center = Offset(curX, curY)
        )
        // Solid center
        drawCircle(
            color = blueColor,
            radius = 6f * scale.coerceIn(0.7f, 2f),
            center = Offset(curX, curY)
        )

        // Heading arrow
        val arrowLen = 22f * scale.coerceIn(0.7f, 2f)
        val headingRad = Math.toRadians(heading.toDouble())
        // Note: canvas Y is inverted, so use -sin for Y
        val ax = curX + (cos(headingRad) * arrowLen).toFloat()
        val ay = curY - (sin(headingRad) * arrowLen).toFloat()
        drawLine(
            color = blueColor,
            start = Offset(curX, curY),
            end = Offset(ax, ay),
            strokeWidth = 3f
        )
    }
}

/**
 * Estimate covered area from the bounding box of relative positions.
 */
private fun computeAreaCovered(points: List<WifiMapPoint>): Float {
    if (points.size < 2) return 0f
    val xs = points.map { it.relativeX }
    val ys = points.map { it.relativeY }
    val w = (xs.max() - xs.min()).coerceAtLeast(0f)
    val h = (ys.max() - ys.min()).coerceAtLeast(0f)
    return w * h
}
