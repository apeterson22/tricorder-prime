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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solomonprime.tricorder.ui.theme.LcarsBlack
import com.solomonprime.tricorder.ui.theme.LcarsBlue
import com.solomonprime.tricorder.ui.theme.LcarsOrange
import com.solomonprime.tricorder.ui.theme.LcarsRed
import com.solomonprime.tricorder.ui.theme.LcarsTan
import com.solomonprime.tricorder.viewmodel.AcousticViewModel

@Composable
fun AcousticScannerScreen(
    viewModel: AcousticViewModel = viewModel()
) {
    val decibels by viewModel.decibels.collectAsState()
    val waveform by viewModel.waveform.collectAsState()
    val freqBand by viewModel.freqBand.collectAsState()
    val dominantFrequency by viewModel.dominantFrequency.collectAsState()
    val snrDb by viewModel.snrDb.collectAsState()

    // Determine waveform color based on decibel level
    val waveformColor = when {
        decibels < 50f -> LcarsBlue
        decibels <= 75f -> LcarsOrange
        else -> LcarsRed
    }

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

        // First row: Amplitude and Freq Band
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LcarsDataCard(
                title = "AMPLITUDE",
                value = String.format("%.1f dB", decibels),
                color = LcarsTan,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            LcarsDataCard(
                title = "FREQ BAND",
                value = freqBand,
                color = LcarsTan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Second row: Dominant Frequency and SNR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LcarsDataCard(
                title = "DOMINANT FREQ",
                value = if (dominantFrequency > 0) String.format("%.0f Hz", dominantFrequency) else "-- Hz",
                color = LcarsBlue,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            LcarsDataCard(
                title = "SNR",
                value = String.format("%.1f dB", snrDb),
                color = LcarsBlue,
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
                    color = waveformColor,
                    style = Stroke(width = 4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description text
        Text(
            text = "ACOUSTIC ANALYSIS — Real-time microphone waveform. Amplitude shows sound pressure in dB. Dominant frequency band indicates primary sound source.",
            color = LcarsTan.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

/**
 * LCARS-style data card for displaying titled values.
 */
@Composable
fun LcarsDataCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = color.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
