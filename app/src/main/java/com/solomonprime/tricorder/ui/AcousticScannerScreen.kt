package com.solomonprime.tricorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.LcarsBlack
import com.solomonprime.tricorder.ui.theme.LcarsOrange
import com.solomonprime.tricorder.ui.theme.LcarsTan
import com.solomonprime.tricorder.viewmodel.AcousticViewModel

@Composable
fun AcousticScannerScreen(
    viewModel: AcousticViewModel = viewModel()
) {
    val decibels by viewModel.decibels.collectAsState()
    val waveform by viewModel.waveform.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startRecording()
        onDispose {
            viewModel.stopRecording()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "ACOUSTIC SCANNER",
            color = LcarsOrange,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LcarsDataCard(
                title = "AMPLITUDE",
                value = String.format("%.1f dB", decibels),
                accentColor = LcarsTan,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            LcarsDataCard(
                title = "FREQ BAND",
                value = "BROAD",
                accentColor = LcarsTan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Waveform Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF111111))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (waveform.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val stepX = width / (waveform.size - 1).coerceAtLeast(1)
                
                val path = Path()
                waveform.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = height - (value * height) // Invert Y so 0 is bottom
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = LcarsOrange,
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}
