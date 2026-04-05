package com.solomonprime.tricorder.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.BioScannerViewModel
import kotlin.math.sin

@Composable
fun BioScannerScreen(
    viewModel: BioScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    
    var hasBodySensorPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBodySensorPermission = permissions[Manifest.permission.BODY_SENSORS] == true
        if (hasBodySensorPermission) {
            viewModel.startScan()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasBodySensorPermission) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACTIVITY_RECOGNITION
            ))
        }
    }
    
    val heartRate by viewModel.heartRate.collectAsState()
    val spo2 by viewModel.bloodOxygen.collectAsState()
    val stressIndex by viewModel.stressIndex.collectAsState()
    val bodyTemp by viewModel.skinTemp.collectAsState()
    val movementLevel by viewModel.movementLevel.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()
    val connectedDevice by viewModel.connectedDeviceName.collectAsState()
    val bloodPressureSys by viewModel.bloodPressureSystolic.collectAsState()
    val bloodPressureDia by viewModel.bloodPressureDiastolic.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val calories by viewModel.caloriesBurned.collectAsState()
    val sleepHours by viewModel.sleepHours.collectAsState()
    
    DisposableEffect(hasBodySensorPermission) {
        if (hasBodySensorPermission) {
            viewModel.startScan()
        }
        onDispose { viewModel.stopScan() }
    }
    
    val respiratoryRate = (heartRate / 4f).coerceIn(12f, 24f)
    val hrv = (50 + (100 - heartRate) * 0.8f).coerceIn(20f, 100f)
    
    val infiniteTransition = rememberInfiniteTransition(label = "bodyScan")
    val scanLine by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "scanLine"
    )

    LcarsScreenScaffold(title = "BIO SCANNER", headerColor = LcarsRed) {
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            DataSourceBanner(
                dataSource = dataSource,
                deviceName = connectedDevice,
                onConnectClick = { viewModel.scanForDevices() }
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(0.3f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    VitalIndicator("HEART", heartRate.toString(), "BPM", LcarsRed, heartRate > 100 || heartRate < 50)
                    VitalIndicator("SpO2", String.format("%.1f", spo2), "%", LcarsBlue, spo2 < 95)
                    VitalIndicator("TEMP", String.format("%.1f", bodyTemp), "°C", LcarsOrange, bodyTemp > 37.8f || bodyTemp < 35.5f)
                    VitalIndicator("BP", "$bloodPressureSys/$bloodPressureDia", "mmHg", LcarsPurple, bloodPressureSys > 140)
                }
                
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .padding(4.dp)
                ) {
                    HumanBodyScan(
                        scanProgress = scanLine,
                        heartRate = heartRate,
                        temperature = bodyTemp,
                        stressLevel = stressIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Column(
                    modifier = Modifier.weight(0.3f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    VitalIndicator("STRESS", String.format("%.0f", stressIndex * 100), "%", getStressColor(stressIndex), stressIndex > 0.7f)
                    VitalIndicator("HRV", String.format("%.0f", hrv), "ms", LcarsYellow, hrv < 30)
                    VitalIndicator("RESP", String.format("%.0f", respiratoryRate), "/min", LcarsTan, respiratoryRate > 20)
                    VitalIndicator("MOVE", String.format("%.0f", movementLevel * 100), "%", LcarsBlue, false)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text("CARDIAC MONITOR", color = LcarsRed.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            EcgWaveform(
                heartRate = heartRate.toFloat(),
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LcarsDarkPanel)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActivityStat(icon = "🚶", value = steps.toString(), label = "STEPS")
                ActivityStat(icon = "🔥", value = String.format("%.0f", calories), label = "KCAL")
                ActivityStat(icon = "😴", value = String.format("%.1f", sleepHours), label = "SLEEP")
            }
            
            Spacer(Modifier.height(12.dp))
            
            LcarsBarGraph(
                data = listOf(
                    "HR" to (heartRate.toFloat() / 200f).coerceIn(0f, 1f),
                    "SpO2" to (spo2 / 100f),
                    "STRS" to stressIndex,
                    "TEMP" to ((bodyTemp - 35f) / 5f).coerceIn(0f, 1f),
                    "HRV" to (hrv / 100f).coerceIn(0f, 1f)
                )
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DataSourceBanner(dataSource: String, deviceName: String?, onConnectClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(LcarsDarkPanel).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = when (dataSource) {
                    "HEALTH_CONNECT" -> "⚕️ HEALTH CONNECT"
                    "GOOGLE_FIT" -> "🏃 GOOGLE FIT"
                    "SAMSUNG_HEALTH" -> "💙 SAMSUNG HEALTH"
                    "WEARABLE" -> "⌚ ${deviceName ?: "WEARABLE"}"
                    "PHONE_SENSORS" -> "📲 PHONE SENSORS"
                    else -> "🔄 SIMULATED"
                },
                color = when (dataSource) { "HEALTH_CONNECT", "GOOGLE_FIT", "SAMSUNG_HEALTH", "WEARABLE" -> LcarsBlue; "PHONE_SENSORS" -> LcarsYellow; else -> LcarsTan.copy(0.6f) },
                fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(LcarsBlue.copy(0.2f)).clickable(onClick = onConnectClick).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("SCAN DEVICES", color = LcarsBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun VitalIndicator(label: String, value: String, unit: String, color: Color, isAlert: Boolean) {
    val alertPulse = rememberInfiniteTransition(label = "alert")
    val alpha by alertPulse.animateFloat(initialValue = 0.7f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alertPulse")
    Column(
        modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(4.dp)).background(if (isAlert) color.copy(alpha = alpha * 0.15f) else LcarsDarkPanel)
            .border(width = if (isAlert) 1.dp else 0.dp, color = if (isAlert) color.copy(alpha = alpha) else Color.Transparent, shape = RoundedCornerShape(4.dp)).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = color.copy(0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Text(value, color = if (isAlert) color.copy(alpha = alpha) else color, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(unit, color = LcarsTan.copy(0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun HumanBodyScan(scanProgress: Float, heartRate: Int, temperature: Float, stressLevel: Float, modifier: Modifier) {
    val heartPulse = rememberInfiniteTransition(label = "heartPulse")
    val heartScale by heartPulse.animateFloat(initialValue = 1f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween((60000 / heartRate.coerceAtLeast(40)).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "heartScale")
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val cx = w / 2f
        drawRect(LcarsDarkPanel.copy(alpha = 0.3f))
        val scanY = h * scanProgress
        drawLine(brush = Brush.horizontalGradient(listOf(Color.Transparent, LcarsBlue.copy(0.8f), LcarsBlue, LcarsBlue.copy(0.8f), Color.Transparent)), start = Offset(0f, scanY), end = Offset(w, scanY), strokeWidth = 4f)
        val bodyColor = when { temperature > 38f -> LcarsRed.copy(0.6f); temperature > 37.5f -> LcarsOrange.copy(0.6f); stressLevel > 0.7f -> LcarsYellow.copy(0.6f); else -> LcarsBlue.copy(0.4f) }
        val headRadius = w * 0.12f; val headY = h * 0.1f
        drawCircle(bodyColor, headRadius, Offset(cx, headY), style = Stroke(2f)); drawCircle(bodyColor.copy(0.2f), headRadius, Offset(cx, headY))
        drawLine(bodyColor, Offset(cx, headY + headRadius), Offset(cx, h * 0.18f), 2f)
        val shoulderY = h * 0.2f; val shoulderWidth = w * 0.35f
        val torsoPath = Path().apply { moveTo(cx - shoulderWidth, shoulderY); lineTo(cx + shoulderWidth, shoulderY); lineTo(cx + shoulderWidth * 0.7f, h * 0.55f); lineTo(cx + shoulderWidth * 0.8f, h * 0.6f); lineTo(cx - shoulderWidth * 0.8f, h * 0.6f); lineTo(cx - shoulderWidth * 0.7f, h * 0.55f); close() }
        drawPath(torsoPath, bodyColor.copy(0.15f)); drawPath(torsoPath, bodyColor, style = Stroke(2f))
        val heartX = cx - w * 0.08f; val heartY = h * 0.32f
        drawCircle(LcarsRed.copy(0.6f), w * 0.04f * heartScale, Offset(heartX, heartY)); drawCircle(LcarsRed, w * 0.02f * heartScale, Offset(heartX, heartY))
        val lungExpand = 1f + sin(scanProgress * 6.28f) * 0.1f
        drawOval(LcarsBlue.copy(0.3f), Offset(cx - w * 0.22f, h * 0.25f), Size(w * 0.12f * lungExpand, h * 0.15f))
        drawOval(LcarsBlue.copy(0.3f), Offset(cx + w * 0.1f, h * 0.25f), Size(w * 0.12f * lungExpand, h * 0.15f))
        val armY = shoulderY + 5f
        drawLine(bodyColor, Offset(cx - shoulderWidth, armY), Offset(cx - shoulderWidth - w * 0.1f, h * 0.35f), 2f, StrokeCap.Round)
        drawLine(bodyColor, Offset(cx - shoulderWidth - w * 0.1f, h * 0.35f), Offset(cx - shoulderWidth - w * 0.15f, h * 0.5f), 2f, StrokeCap.Round)
        drawLine(bodyColor, Offset(cx + shoulderWidth, armY), Offset(cx + shoulderWidth + w * 0.1f, h * 0.35f), 2f, StrokeCap.Round)
        drawLine(bodyColor, Offset(cx + shoulderWidth + w * 0.1f, h * 0.35f), Offset(cx + shoulderWidth + w * 0.15f, h * 0.5f), 2f, StrokeCap.Round)
        val hipY = h * 0.6f; val legW = shoulderWidth * 0.4f
        drawLine(bodyColor, Offset(cx - legW, hipY), Offset(cx - legW - w * 0.02f, h * 0.8f), 2f, StrokeCap.Round)
        drawLine(bodyColor, Offset(cx - legW - w * 0.02f, h * 0.8f), Offset(cx - legW, h * 0.95f), 2f, StrokeCap.Round)
        drawLine(bodyColor, Offset(cx + legW, hipY), Offset(cx + legW + w * 0.02f, h * 0.8f), 2f, StrokeCap.Round)
        drawLine(bodyColor, Offset(cx + legW + w * 0.02f, h * 0.8f), Offset(cx + legW, h * 0.95f), 2f, StrokeCap.Round)
        if (stressLevel > 0.5f) { val brainColor = if (stressLevel > 0.7f) LcarsRed else LcarsYellow; for (i in 0..5) { val angle = i * 60f + scanProgress * 360f; val rad = Math.toRadians(angle.toDouble()); val bx = cx + (headRadius * 0.7f * kotlin.math.cos(rad)).toFloat(); val by = headY + (headRadius * 0.7f * kotlin.math.sin(rad)).toFloat(); drawCircle(brainColor.copy(0.5f), 3f, Offset(bx, by)) } }
    }
}

@Composable
private fun ActivityStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(value, color = LcarsTan, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, color = LcarsTan.copy(0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable private fun getStressColor(stress: Float) = when { stress < 0.3f -> LcarsBlue; stress < 0.5f -> LcarsTan; stress < 0.7f -> LcarsYellow; stress < 0.85f -> LcarsOrange; else -> LcarsRed }

@Composable
private fun EcgWaveform(heartRate: Float, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "ecg")
    val scroll by inf.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween((60000 / heartRate.coerceAtLeast(30f)).toInt(), easing = LinearEasing)), label = "ecgScroll")
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val midY = h / 2f
        drawRect(LcarsDarkPanel)
        for (i in 1..3) drawLine(LcarsRed.copy(0.06f), Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1f)
        val path = Path(); val cycleW = w * 0.5f; val off = scroll * cycleW
        fun ecgY(x: Float): Float { val phase = ((x + off) % cycleW) / cycleW; return when { phase < 0.1f -> midY; phase < 0.15f -> midY - h * 0.08f; phase < 0.2f -> midY; phase < 0.22f -> midY + h * 0.05f; phase < 0.28f -> midY - h * 0.4f; phase < 0.32f -> midY + h * 0.12f; phase < 0.4f -> midY; phase < 0.5f -> midY - h * 0.1f; else -> midY } }
        for (i in 0..(w / 2f).toInt()) { val x = i * 2f; val y = ecgY(x); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        drawPath(path, LcarsRed, style = Stroke(2f)); drawPath(path, LcarsRed.copy(0.15f), style = Stroke(6f))
    }
}
