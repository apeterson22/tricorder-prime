package com.solomonprime.tricorder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.BioScannerViewModel

@Composable
fun BioScannerScreen(
    viewModel: BioScannerViewModel = viewModel()
) {
    // Collect real-time state from ViewModel
    val heartRate by viewModel.heartRate.collectAsState()
    val spo2 by viewModel.bloodOxygen.collectAsState()
    val stressIndex by viewModel.stressIndex.collectAsState()
    val bodyTemp by viewModel.skinTemp.collectAsState()
    val movementLevel by viewModel.movementLevel.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()
    
    // Start/stop scanning based on lifecycle
    DisposableEffect(Unit) {
        viewModel.startScan()
        onDispose { viewModel.stopScan() }
    }
    
    // Calculate respiratory rate from heart rate (approximation)
    val respiratoryRate = (heartRate / 4f).coerceIn(12f, 24f)

    LcarsScreenScaffold(title = "BIO SCANNER", headerColor = LcarsRed) {

        // Data source indicator
        Text(
            text = when (dataSource) {
                "WEARABLE" -> "📱 CONNECTED WEARABLE"
                "PHONE_SENSORS" -> "📲 PHONE SENSORS"
                else -> "🔄 SIMULATED DATA"
            },
            color = when (dataSource) {
                "WEARABLE" -> LcarsBlue
                "PHONE_SENSORS" -> LcarsYellow
                else -> LcarsTan.copy(0.6f)
            },
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LcarsAnimatedValue(value = heartRate.toFloat(), label = "HEART RATE", unit = "BPM", color = LcarsRed, decimalPlaces = 0)
            LcarsAnimatedValue(value = bodyTemp, label = "BODY TEMP", unit = "°C", color = LcarsOrange)
        }

        Spacer(Modifier.height(12.dp))

        // ECG waveform
        Text("CARDIAC MONITOR", color = LcarsRed.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        EcgWaveform(
            heartRate = heartRate.toFloat(),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // SpO2 blood drop
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SpO2", color = LcarsBlue.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                BloodDropGauge(
                    value = spo2 / 100f,
                    modifier = Modifier.size(60.dp, 80.dp)
                )
                LcarsAnimatedValue(value = spo2, unit = "%", color = LcarsBlue, decimalPlaces = 0)
            }

            // Stress aura
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("STRESS", color = LcarsPurple.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                StressAura(
                    stressIndex = stressIndex,
                    modifier = Modifier.size(80.dp)
                )
                LcarsAnimatedValue(value = stressIndex, unit = "", color = LcarsPurple, decimalPlaces = 0)
            }

            // Respiratory
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RESP", color = LcarsYellow.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                LcarsAnimatedValue(value = respiratoryRate, label = "", unit = "/min", color = LcarsYellow, decimalPlaces = 0)
            }
        }

        Spacer(Modifier.height(12.dp))

        LcarsBarGraph(
            data = listOf(
                "HR" to (heartRate.toFloat() / 200f).coerceIn(0f, 1f),
                "SpO2" to (spo2 / 100f),
                "STRS" to (stressIndex / 100f),
                "TEMP" to ((bodyTemp - 35f) / 5f).coerceIn(0f, 1f),
                "RESP" to (respiratoryRate / 40f).coerceIn(0f, 1f)
            )
        )
    }
}

@Composable
private fun EcgWaveform(heartRate: Float, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecg")
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((60000 / heartRate.coerceAtLeast(30f)).toInt(), easing = LinearEasing)
        ),
        label = "ecgScroll"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        drawRect(LcarsDarkPanel)

        // Grid
        for (i in 1..3) drawLine(LcarsRed.copy(0.06f), Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1f)

        // ECG pattern: flat-P-flat-QRS-flat-T-flat repeated
        val path = Path()
        val cycleWidth = w * 0.5f // half screen per cycle
        val offset = scrollOffset * cycleWidth

        fun ecgY(x: Float): Float {
            val phase = ((x + offset) % cycleWidth) / cycleWidth
            return when {
                phase < 0.1f -> midY // baseline
                phase < 0.15f -> midY - h * 0.08f // P wave
                phase < 0.2f -> midY
                phase < 0.22f -> midY + h * 0.05f // Q
                phase < 0.28f -> midY - h * 0.4f  // R peak
                phase < 0.32f -> midY + h * 0.12f // S
                phase < 0.4f -> midY
                phase < 0.5f -> midY - h * 0.1f  // T wave
                else -> midY
            }
        }

        val steps = (w / 2f).toInt()
        for (i in 0..steps) {
            val x = i * 2f
            val y = ecgY(x)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, LcarsRed, style = Stroke(2f))
        drawPath(path, LcarsRed.copy(alpha = 0.15f), style = Stroke(6f))
    }
}

@Composable
private fun BloodDropGauge(value: Float, modifier: Modifier) {
    val animatedFill by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.6f),
        label = "blood"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Drop shape path
        val dropPath = Path().apply {
            moveTo(cx, 0f) // top point
            cubicTo(cx + w * 0.5f, h * 0.4f, cx + w * 0.4f, h * 0.9f, cx, h)
            cubicTo(cx - w * 0.4f, h * 0.9f, cx - w * 0.5f, h * 0.4f, cx, 0f)
            close()
        }

        // Outline
        drawPath(dropPath, LcarsRed.copy(alpha = 0.3f), style = Stroke(2f))

        // Fill from bottom using clip
        clipRect(top = h * (1f - animatedFill), bottom = h) {
            drawPath(dropPath, LcarsRed.copy(alpha = 0.6f), style = Fill)
        }
    }
}

@Composable
private fun StressAura(stressIndex: Float, modifier: Modifier) {
    val normalizedStress = (stressIndex / 100f).coerceIn(0f, 1f)
    val auraColor by animateColorAsState(
        targetValue = when {
            normalizedStress < 0.3f -> LcarsBlue
            normalizedStress < 0.6f -> LcarsYellow
            normalizedStress < 0.8f -> LcarsOrange
            else -> LcarsRed
        },
        animationSpec = tween(1000),
        label = "aura"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "stressAura")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((2000 * (1f - normalizedStress * 0.7f)).toInt(), easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "auraPulse"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy)

        // Outer aura
        drawCircle(auraColor.copy(alpha = 0.1f * pulse), r, Offset(cx, cy))
        drawCircle(auraColor.copy(alpha = 0.2f * pulse), r * 0.7f, Offset(cx, cy))
        drawCircle(auraColor.copy(alpha = 0.4f * pulse), r * 0.4f, Offset(cx, cy))

        // Center indicator
        drawCircle(auraColor, r * 0.15f, Offset(cx, cy))
    }
}
