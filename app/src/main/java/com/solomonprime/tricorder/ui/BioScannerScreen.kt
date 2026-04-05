package com.solomonprime.tricorder.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.*
import com.solomonprime.tricorder.viewmodel.BioScannerViewModel
import kotlin.math.PI
import kotlin.math.cos
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
                modifier = Modifier.fillMaxWidth().height(360.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(0.28f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    VitalIndicator("HEART", heartRate.toString(), "BPM", LcarsRed, heartRate > 100 || heartRate < 50)
                    VitalIndicator("SpO2", String.format("%.1f", spo2), "%", LcarsBlue, spo2 < 95)
                    VitalIndicator("TEMP", String.format("%.1f", bodyTemp), "°C", LcarsOrange, bodyTemp > 37.8f || bodyTemp < 35.5f)
                    VitalIndicator("BP", "$bloodPressureSys/$bloodPressureDia", "mmHg", LcarsPurple, bloodPressureSys > 140)
                }
                
                Box(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight()
                        .padding(4.dp)
                ) {
                    AnatomicalBodyScan(
                        scanProgress = scanLine,
                        heartRate = heartRate,
                        temperature = bodyTemp,
                        stressLevel = stressIndex,
                        respiratoryRate = respiratoryRate,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Column(
                    modifier = Modifier.weight(0.28f),
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
private fun AnatomicalBodyScan(
    scanProgress: Float,
    heartRate: Int,
    temperature: Float,
    stressLevel: Float,
    respiratoryRate: Float,
    modifier: Modifier
) {
    val heartPulse = rememberInfiniteTransition(label = "heartPulse")
    val heartScale by heartPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            tween((60000 / heartRate.coerceAtLeast(40)).toInt(), easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "heartScale"
    )
    
    val breathCycle by heartPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((60000 / respiratoryRate.coerceAtLeast(8f)).toInt(), easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breath"
    )
    
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        
        // Background with grid
        drawRect(Color(0xFF0A0A12))
        
        // Subtle grid
        val gridColor = LcarsBlue.copy(alpha = 0.05f)
        for (i in 0..20) {
            val y = h * i / 20f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), 0.5f)
        }
        for (i in 0..10) {
            val x = w * i / 10f
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), 0.5f)
        }
        
        // Scanning beam
        val scanY = h * scanProgress
        val beamGradient = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                LcarsBlue.copy(alpha = 0.1f),
                LcarsBlue.copy(alpha = 0.4f),
                LcarsBlue.copy(alpha = 0.1f),
                Color.Transparent
            ),
            startY = scanY - 40f,
            endY = scanY + 40f
        )
        drawRect(beamGradient)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, LcarsBlue, LcarsBlue, Color.Transparent)
            ),
            start = Offset(0f, scanY),
            end = Offset(w, scanY),
            strokeWidth = 2f
        )
        
        // Body temperature-based color
        val skinTone = when {
            temperature > 38.5f -> Color(0xFFFF6B6B) // Fever - reddish
            temperature > 37.5f -> Color(0xFFFFAA85) // Elevated - warm
            temperature < 35.5f -> Color(0xFF85B0FF) // Hypothermia - bluish
            else -> Color(0xFFE8C4A8) // Normal skin tone
        }
        val skinHighlight = skinTone.copy(alpha = 0.7f)
        val skinShadow = skinTone.copy(alpha = 0.3f)
        val organColor = LcarsBlue.copy(alpha = 0.5f)
        
        // === HEAD ===
        val headCenterY = h * 0.08f
        val headRadiusX = w * 0.10f
        val headRadiusY = w * 0.12f
        
        // Skull shape (oval)
        drawOval(
            color = skinShadow,
            topLeft = Offset(cx - headRadiusX, headCenterY - headRadiusY),
            size = Size(headRadiusX * 2, headRadiusY * 2)
        )
        drawOval(
            color = skinTone,
            topLeft = Offset(cx - headRadiusX + 2, headCenterY - headRadiusY + 2),
            size = Size(headRadiusX * 2 - 4, headRadiusY * 2 - 4),
            style = Stroke(2f)
        )
        
        // Brain (visible through scan effect)
        if (scanProgress > 0.02f && scanProgress < 0.15f) {
            val brainColor = if (stressLevel > 0.6f) LcarsYellow.copy(0.6f) else LcarsBlue.copy(0.4f)
            // Brain hemispheres
            drawOval(
                color = brainColor,
                topLeft = Offset(cx - headRadiusX * 0.7f, headCenterY - headRadiusY * 0.6f),
                size = Size(headRadiusX * 0.65f, headRadiusY * 0.8f)
            )
            drawOval(
                color = brainColor,
                topLeft = Offset(cx + headRadiusX * 0.05f, headCenterY - headRadiusY * 0.6f),
                size = Size(headRadiusX * 0.65f, headRadiusY * 0.8f)
            )
            // Brain stem
            drawOval(
                color = brainColor.copy(alpha = 0.5f),
                topLeft = Offset(cx - w * 0.02f, headCenterY + headRadiusY * 0.3f),
                size = Size(w * 0.04f, headRadiusY * 0.4f)
            )
        }
        
        // === NECK ===
        val neckTop = headCenterY + headRadiusY
        val neckBottom = h * 0.17f
        val neckWidth = w * 0.06f
        drawRect(
            color = skinShadow,
            topLeft = Offset(cx - neckWidth, neckTop),
            size = Size(neckWidth * 2, neckBottom - neckTop)
        )
        // Spine in neck
        drawLine(LcarsBlue.copy(0.3f), Offset(cx, neckTop), Offset(cx, neckBottom), 2f)
        
        // === TORSO ===
        val shoulderY = neckBottom
        val shoulderWidth = w * 0.28f
        val chestBottom = h * 0.42f
        val waistY = h * 0.48f
        val waistWidth = w * 0.18f
        val hipY = h * 0.55f
        val hipWidth = w * 0.22f
        
        // Torso path - realistic body shape
        val torsoPath = Path().apply {
            // Start at left shoulder
            moveTo(cx - shoulderWidth, shoulderY)
            // Shoulder curve
            cubicTo(
                cx - shoulderWidth - w * 0.02f, shoulderY + h * 0.02f,
                cx - shoulderWidth - w * 0.01f, shoulderY + h * 0.05f,
                cx - shoulderWidth + w * 0.02f, shoulderY + h * 0.08f
            )
            // Left side of chest
            lineTo(cx - shoulderWidth + w * 0.04f, chestBottom)
            // Waist curve
            cubicTo(
                cx - waistWidth - w * 0.02f, chestBottom + h * 0.02f,
                cx - waistWidth, waistY,
                cx - waistWidth, waistY
            )
            // Hip curve
            cubicTo(
                cx - waistWidth - w * 0.01f, waistY + h * 0.02f,
                cx - hipWidth, hipY - h * 0.02f,
                cx - hipWidth, hipY
            )
            // Bottom center
            lineTo(cx - w * 0.08f, hipY + h * 0.02f)
            lineTo(cx + w * 0.08f, hipY + h * 0.02f)
            // Right hip
            lineTo(cx + hipWidth, hipY)
            // Right waist
            cubicTo(
                cx + hipWidth, hipY - h * 0.02f,
                cx + waistWidth + w * 0.01f, waistY + h * 0.02f,
                cx + waistWidth, waistY
            )
            // Right chest
            cubicTo(
                cx + waistWidth, waistY,
                cx + waistWidth + w * 0.02f, chestBottom + h * 0.02f,
                cx + shoulderWidth - w * 0.04f, chestBottom
            )
            // Right shoulder
            lineTo(cx + shoulderWidth - w * 0.02f, shoulderY + h * 0.08f)
            cubicTo(
                cx + shoulderWidth + w * 0.01f, shoulderY + h * 0.05f,
                cx + shoulderWidth + w * 0.02f, shoulderY + h * 0.02f,
                cx + shoulderWidth, shoulderY
            )
            // Top of shoulders
            lineTo(cx + neckWidth, shoulderY)
            lineTo(cx - neckWidth, shoulderY)
            close()
        }
        
        // Draw torso fill and outline
        drawPath(torsoPath, skinShadow)
        drawPath(torsoPath, skinTone, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // === RIBCAGE ===
        val ribColor = LcarsBlue.copy(alpha = 0.25f)
        for (i in 0..6) {
            val ribY = shoulderY + h * 0.06f + i * h * 0.035f
            val ribWidth = (shoulderWidth - w * 0.04f) * (1f - i * 0.08f)
            // Left rib
            drawArc(
                color = ribColor,
                startAngle = 0f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(cx - ribWidth, ribY - h * 0.015f),
                size = Size(ribWidth, h * 0.03f),
                style = Stroke(1.5f)
            )
            // Right rib
            drawArc(
                color = ribColor,
                startAngle = 100f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(cx, ribY - h * 0.015f),
                size = Size(ribWidth, h * 0.03f),
                style = Stroke(1.5f)
            )
        }
        
        // === HEART ===
        val heartX = cx - w * 0.06f
        val heartY = h * 0.28f
        val heartSize = w * 0.08f * heartScale
        
        // Heart shape path
        val heartPath = Path().apply {
            val hx = heartX
            val hy = heartY
            val hs = heartSize
            moveTo(hx, hy + hs * 0.3f)
            // Left curve
            cubicTo(hx - hs * 0.5f, hy, hx - hs * 0.5f, hy - hs * 0.4f, hx, hy - hs * 0.2f)
            // Right curve
            cubicTo(hx + hs * 0.5f, hy - hs * 0.4f, hx + hs * 0.5f, hy, hx, hy + hs * 0.3f)
        }
        
        // Heart glow
        drawPath(heartPath, LcarsRed.copy(alpha = 0.3f * heartScale), style = Stroke(8f))
        drawPath(heartPath, LcarsRed.copy(alpha = 0.6f))
        drawPath(heartPath, LcarsRed, style = Stroke(2f))
        
        // === LUNGS ===
        val lungExpand = 1f + breathCycle * 0.08f
        val lungY = h * 0.26f
        val lungWidth = w * 0.10f * lungExpand
        val lungHeight = h * 0.12f * lungExpand
        
        // Left lung
        val leftLungPath = Path().apply {
            moveTo(cx - w * 0.04f, lungY)
            cubicTo(
                cx - w * 0.08f, lungY - lungHeight * 0.2f,
                cx - lungWidth - w * 0.02f, lungY,
                cx - lungWidth, lungY + lungHeight * 0.4f
            )
            cubicTo(
                cx - lungWidth - w * 0.01f, lungY + lungHeight,
                cx - w * 0.06f, lungY + lungHeight,
                cx - w * 0.04f, lungY + lungHeight * 0.8f
            )
            close()
        }
        drawPath(leftLungPath, LcarsBlue.copy(alpha = 0.25f + breathCycle * 0.1f))
        drawPath(leftLungPath, LcarsBlue.copy(alpha = 0.5f), style = Stroke(1.5f))
        
        // Right lung (larger)
        val rightLungPath = Path().apply {
            moveTo(cx + w * 0.02f, lungY)
            cubicTo(
                cx + w * 0.06f, lungY - lungHeight * 0.2f,
                cx + lungWidth + w * 0.03f, lungY,
                cx + lungWidth + w * 0.02f, lungY + lungHeight * 0.4f
            )
            cubicTo(
                cx + lungWidth + w * 0.03f, lungY + lungHeight,
                cx + w * 0.05f, lungY + lungHeight,
                cx + w * 0.02f, lungY + lungHeight * 0.8f
            )
            close()
        }
        drawPath(rightLungPath, LcarsBlue.copy(alpha = 0.25f + breathCycle * 0.1f))
        drawPath(rightLungPath, LcarsBlue.copy(alpha = 0.5f), style = Stroke(1.5f))
        
        // === STOMACH ===
        val stomachY = h * 0.40f
        drawOval(
            color = LcarsOrange.copy(alpha = 0.25f),
            topLeft = Offset(cx - w * 0.06f, stomachY),
            size = Size(w * 0.10f, h * 0.06f)
        )
        drawOval(
            color = LcarsOrange.copy(alpha = 0.4f),
            topLeft = Offset(cx - w * 0.06f, stomachY),
            size = Size(w * 0.10f, h * 0.06f),
            style = Stroke(1f)
        )
        
        // === LIVER ===
        val liverPath = Path().apply {
            moveTo(cx + w * 0.02f, h * 0.38f)
            cubicTo(cx + w * 0.12f, h * 0.36f, cx + w * 0.15f, h * 0.42f, cx + w * 0.10f, h * 0.46f)
            cubicTo(cx + w * 0.06f, h * 0.48f, cx, h * 0.46f, cx + w * 0.02f, h * 0.38f)
        }
        drawPath(liverPath, LcarsTan.copy(alpha = 0.35f))
        drawPath(liverPath, LcarsTan.copy(alpha = 0.5f), style = Stroke(1f))
        
        // === KIDNEYS ===
        // Left kidney
        drawOval(
            color = LcarsPurple.copy(alpha = 0.3f),
            topLeft = Offset(cx - w * 0.14f, h * 0.42f),
            size = Size(w * 0.05f, h * 0.06f)
        )
        // Right kidney
        drawOval(
            color = LcarsPurple.copy(alpha = 0.3f),
            topLeft = Offset(cx + w * 0.09f, h * 0.42f),
            size = Size(w * 0.05f, h * 0.06f)
        )
        
        // === INTESTINES (simplified) ===
        val intestineColor = LcarsTan.copy(alpha = 0.2f)
        for (i in 0..3) {
            val iy = h * 0.48f + i * h * 0.015f
            drawLine(
                intestineColor,
                Offset(cx - w * 0.10f + i * w * 0.01f, iy),
                Offset(cx + w * 0.10f - i * w * 0.01f, iy),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        
        // === SPINE ===
        val spineColor = LcarsBlue.copy(alpha = 0.4f)
        for (i in 0..12) {
            val vy = neckBottom + i * h * 0.028f
            // Vertebra
            drawRoundRect(
                color = spineColor,
                topLeft = Offset(cx - w * 0.025f, vy),
                size = Size(w * 0.05f, h * 0.018f),
                cornerRadius = CornerRadius(4f)
            )
        }
        
        // === ARMS ===
        val armStartY = shoulderY + h * 0.01f
        val elbowY = h * 0.38f
        val wristY = h * 0.52f
        val armWidth = w * 0.045f
        
        // Left arm
        val leftArmPath = Path().apply {
            moveTo(cx - shoulderWidth + w * 0.01f, armStartY)
            // Upper arm
            quadraticBezierTo(
                cx - shoulderWidth - w * 0.08f, armStartY + h * 0.10f,
                cx - shoulderWidth - w * 0.10f, elbowY
            )
            // Forearm
            quadraticBezierTo(
                cx - shoulderWidth - w * 0.12f, elbowY + h * 0.08f,
                cx - shoulderWidth - w * 0.14f, wristY
            )
            // Hand
            lineTo(cx - shoulderWidth - w * 0.16f, wristY + h * 0.04f)
            // Return path (inner arm)
            lineTo(cx - shoulderWidth - w * 0.12f, wristY)
            quadraticBezierTo(
                cx - shoulderWidth - w * 0.08f, elbowY + h * 0.06f,
                cx - shoulderWidth - w * 0.06f, elbowY
            )
            quadraticBezierTo(
                cx - shoulderWidth - w * 0.04f, armStartY + h * 0.08f,
                cx - shoulderWidth + w * 0.03f, armStartY + h * 0.02f
            )
        }
        drawPath(leftArmPath, skinShadow)
        drawPath(leftArmPath, skinTone, style = Stroke(2f))
        
        // Right arm (mirrored)
        val rightArmPath = Path().apply {
            moveTo(cx + shoulderWidth - w * 0.01f, armStartY)
            quadraticBezierTo(
                cx + shoulderWidth + w * 0.08f, armStartY + h * 0.10f,
                cx + shoulderWidth + w * 0.10f, elbowY
            )
            quadraticBezierTo(
                cx + shoulderWidth + w * 0.12f, elbowY + h * 0.08f,
                cx + shoulderWidth + w * 0.14f, wristY
            )
            lineTo(cx + shoulderWidth + w * 0.16f, wristY + h * 0.04f)
            lineTo(cx + shoulderWidth + w * 0.12f, wristY)
            quadraticBezierTo(
                cx + shoulderWidth + w * 0.08f, elbowY + h * 0.06f,
                cx + shoulderWidth + w * 0.06f, elbowY
            )
            quadraticBezierTo(
                cx + shoulderWidth + w * 0.04f, armStartY + h * 0.08f,
                cx + shoulderWidth - w * 0.03f, armStartY + h * 0.02f
            )
        }
        drawPath(rightArmPath, skinShadow)
        drawPath(rightArmPath, skinTone, style = Stroke(2f))
        
        // === LEGS ===
        val legTop = hipY + h * 0.01f
        val kneeY = h * 0.72f
        val ankleY = h * 0.92f
        val legSep = w * 0.08f
        val thighWidth = w * 0.08f
        val calfWidth = w * 0.05f
        
        // Left leg
        val leftLegPath = Path().apply {
            moveTo(cx - legSep - thighWidth, legTop)
            // Outer thigh
            quadraticBezierTo(cx - legSep - thighWidth - w * 0.01f, kneeY - h * 0.05f, cx - legSep - calfWidth, kneeY)
            // Outer calf
            quadraticBezierTo(cx - legSep - calfWidth - w * 0.01f, ankleY - h * 0.05f, cx - legSep - calfWidth * 0.8f, ankleY)
            // Foot
            lineTo(cx - legSep - calfWidth * 1.5f, ankleY + h * 0.03f)
            lineTo(cx - legSep + calfWidth * 0.3f, ankleY + h * 0.03f)
            lineTo(cx - legSep + calfWidth * 0.5f, ankleY)
            // Inner calf
            quadraticBezierTo(cx - legSep + calfWidth * 0.3f, ankleY - h * 0.05f, cx - legSep + calfWidth * 0.5f, kneeY)
            // Inner thigh
            quadraticBezierTo(cx - legSep + thighWidth * 0.3f, kneeY - h * 0.05f, cx - legSep + thighWidth * 0.5f, legTop)
            close()
        }
        drawPath(leftLegPath, skinShadow)
        drawPath(leftLegPath, skinTone, style = Stroke(2f))
        
        // Right leg
        val rightLegPath = Path().apply {
            moveTo(cx + legSep + thighWidth, legTop)
            quadraticBezierTo(cx + legSep + thighWidth + w * 0.01f, kneeY - h * 0.05f, cx + legSep + calfWidth, kneeY)
            quadraticBezierTo(cx + legSep + calfWidth + w * 0.01f, ankleY - h * 0.05f, cx + legSep + calfWidth * 0.8f, ankleY)
            lineTo(cx + legSep + calfWidth * 1.5f, ankleY + h * 0.03f)
            lineTo(cx + legSep - calfWidth * 0.3f, ankleY + h * 0.03f)
            lineTo(cx + legSep - calfWidth * 0.5f, ankleY)
            quadraticBezierTo(cx + legSep - calfWidth * 0.3f, ankleY - h * 0.05f, cx + legSep - calfWidth * 0.5f, kneeY)
            quadraticBezierTo(cx + legSep - thighWidth * 0.3f, kneeY - h * 0.05f, cx + legSep - thighWidth * 0.5f, legTop)
            close()
        }
        drawPath(rightLegPath, skinShadow)
        drawPath(rightLegPath, skinTone, style = Stroke(2f))
        
        // Femur bones (visible through scan)
        if (scanProgress > 0.55f && scanProgress < 0.95f) {
            val boneColor = LcarsBlue.copy(alpha = 0.3f)
            // Left femur
            drawLine(boneColor, Offset(cx - legSep, legTop + h * 0.02f), Offset(cx - legSep - w * 0.01f, kneeY - h * 0.02f), 4f, StrokeCap.Round)
            // Left tibia/fibula
            drawLine(boneColor, Offset(cx - legSep - w * 0.01f, kneeY + h * 0.01f), Offset(cx - legSep, ankleY - h * 0.02f), 3f, StrokeCap.Round)
            // Right femur
            drawLine(boneColor, Offset(cx + legSep, legTop + h * 0.02f), Offset(cx + legSep + w * 0.01f, kneeY - h * 0.02f), 4f, StrokeCap.Round)
            // Right tibia/fibula
            drawLine(boneColor, Offset(cx + legSep + w * 0.01f, kneeY + h * 0.01f), Offset(cx + legSep, ankleY - h * 0.02f), 3f, StrokeCap.Round)
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
