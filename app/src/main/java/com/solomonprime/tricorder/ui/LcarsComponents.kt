package com.solomonprime.tricorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomonprime.tricorder.ui.theme.*

@Composable
fun LcarsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LcarsOrange,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 8.dp, bottomEnd = 8.dp))
            .background(if (enabled) color else Color.Gray)
            .clickable(onClick = onClick, enabled = enabled)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = LcarsBlack,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun LcarsNavigationBar(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(LcarsBlack)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decorative elbow piece
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 28.dp))
                .background(LcarsOrange)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        items.forEachIndexed { index, item ->
            val isSelected = item == selectedItem
            val color = when (item) {
                "ENV" -> if (isSelected) LcarsOrange else LcarsTan
                "GEO" -> if (isSelected) LcarsBlue else LcarsPurple
                "EM" -> if (isSelected) LcarsPink else LcarsPurple
                "ACO" -> if (isSelected) LcarsRed else LcarsTan
                "BIO" -> if (isSelected) LcarsBlue else LcarsTan
                "RAD" -> if (isSelected) LcarsPink else LcarsTan
                else -> if (isSelected) LcarsOrange else LcarsTan
            }
            
            LcarsNavButton(
                text = item,
                onClick = { onItemSelected(item) },
                color = color,
                isSelected = isSelected
            )
            
            if (index < items.lastIndex) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // End decorative cap
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(LcarsTan)
        )
    }
}

@Composable
private fun LcarsNavButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = LcarsBlack,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = if (isSelected) 18.sp else 16.sp
        )
    }
}
