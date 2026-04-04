package com.solomonprime.tricorder.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomonprime.tricorder.ui.theme.*

/**
 * LCARS-styled data card for displaying sensor readings
 */
@Composable
fun LcarsDataCard(
    title: String,
    value: String,
    unit: String = "",
    accentColor: Color = LcarsOrange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar with rounded end
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .background(accentColor)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Title section
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title.uppercase(),
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Value section
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(LcarsBlack)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    color = LcarsWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = LcarsWhite.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
        
        // Right cap
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(accentColor)
        )
    }
}

/**
 * LCARS-styled section header with decorative elements
 */
@Composable
fun LcarsSectionHeader(
    title: String,
    color: Color = LcarsOrange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decorative elbow
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 24.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .background(color)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = title.uppercase(),
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Horizontal line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(color.copy(alpha = 0.5f))
        )
        
        // End cap
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 24.dp)
                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                .background(color)
        )
    }
}

/**
 * Animated scanning line effect for active sensors
 */
@Composable
fun LcarsScanningIndicator(
    isActive: Boolean = true,
    color: Color = LcarsBlue,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanPosition"
    )
    
    if (isActive) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            val lineWidth = size.width * 0.3f
            val startX = (size.width + lineWidth) * scanPosition - lineWidth
            
            drawRect(
                color = color,
                topLeft = Offset(startX, 0f),
                size = Size(lineWidth, size.height)
            )
        }
    }
}

/**
 * LCARS-styled gauge for displaying values within a range
 */
@Composable
fun LcarsGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    label: String,
    unit: String = "",
    accentColor: Color = LcarsOrange,
    modifier: Modifier = Modifier
) {
    val normalizedValue = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label.uppercase(),
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (unit.isEmpty()) "%.1f".format(value) else "%.1f %s".format(value, unit),
                color = LcarsWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LcarsBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalizedValue)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "%.0f".format(minValue),
                color = LcarsWhite.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            Text(
                text = "%.0f".format(maxValue),
                color = LcarsWhite.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * LCARS-styled 3-axis display for accelerometer/magnetometer data
 */
@Composable
fun LcarsAxisDisplay(
    title: String,
    x: Float,
    y: Float,
    z: Float,
    unit: String = "",
    accentColor: Color = LcarsBlue,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        LcarsSectionHeader(title = title, color = accentColor)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AxisValue(label = "X", value = x, unit = unit, color = LcarsRed)
            AxisValue(label = "Y", value = y, unit = unit, color = LcarsOrange)
            AxisValue(label = "Z", value = z, unit = unit, color = LcarsBlue)
        }
    }
}

@Composable
private fun AxisValue(
    label: String,
    value: Float,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "%.2f".format(value),
            color = LcarsWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                color = LcarsWhite.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * LCARS-styled status indicator
 */
@Composable
fun LcarsStatusIndicator(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isActive) LcarsOrange else LcarsRed)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            color = if (isActive) LcarsOrange else LcarsRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
