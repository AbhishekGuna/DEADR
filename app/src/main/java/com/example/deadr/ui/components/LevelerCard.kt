package com.example.deadr.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadr.ui.theme.CyanPrimary
import com.example.deadr.ui.theme.ErrorRed
import com.example.deadr.ui.theme.SuccessGreen
import com.example.deadr.ui.theme.TextPrimary
import com.example.deadr.ui.theme.TextSecondary
import com.example.deadr.ui.theme.WarningAmber
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A visual leveler tool that displays pitch and roll angles.
 * Shows a bubble-style indicator that moves based on phone orientation.
 * Turns green when the phone is level (within ±2°).
 */
@Composable
fun LevelerCard(
    pitchDegrees: Float,
    rollDegrees: Float,
    modifier: Modifier = Modifier
) {
    // Determine if the phone is level (within ±2°)
    val isLevel = abs(pitchDegrees) <= 2f && abs(rollDegrees) <= 2f
    
    // Animate the indicator position
    val animatedPitch by animateFloatAsState(
        targetValue = pitchDegrees.coerceIn(-45f, 45f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "pitch"
    )
    val animatedRoll by animateFloatAsState(
        targetValue = rollDegrees.coerceIn(-45f, 45f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "roll"
    )
    
    // Color changes based on level status
    val indicatorColor by animateColorAsState(
        targetValue = when {
            isLevel -> SuccessGreen
            abs(pitchDegrees) > 15f || abs(rollDegrees) > 15f -> ErrorRed
            abs(pitchDegrees) > 8f || abs(rollDegrees) > 8f -> WarningAmber
            else -> CyanPrimary
        },
        animationSpec = tween(300),
        label = "color"
    )
    
    AccentGlassCard(
        accentColor = indicatorColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Level",
                        tint = indicatorColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Leveler",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Level indicator badge
                if (isLevel) {
                    GlassSurface(
                        cornerRadius = 8.dp,
                        alpha = 0.3f
                    ) {
                        Text(
                            text = "✓ LEVEL",
                            color = SuccessGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bubble Level Indicator
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val outerRadius = size.minDimension / 2 - 4.dp.toPx()
                    val innerRadius = outerRadius * 0.2f
                    
                    // Outer circle (target zone)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = outerRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Middle reference circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = outerRadius * 0.5f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Center crosshair
                    val crosshairSize = 10.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(centerX - crosshairSize, centerY),
                        end = Offset(centerX + crosshairSize, centerY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(centerX, centerY - crosshairSize),
                        end = Offset(centerX, centerY + crosshairSize),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Calculate bubble position based on tilt
                    // Pitch moves up/down, Roll moves left/right
                    val maxOffset = outerRadius - innerRadius - 4.dp.toPx()
                    val bubbleOffsetX = (animatedRoll / 45f) * maxOffset
                    val bubbleOffsetY = (animatedPitch / 45f) * maxOffset
                    
                    val bubbleX = centerX + bubbleOffsetX
                    val bubbleY = centerY + bubbleOffsetY
                    
                    // Bubble glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                indicatorColor.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(bubbleX, bubbleY),
                            radius = innerRadius * 2
                        ),
                        radius = innerRadius * 2,
                        center = Offset(bubbleX, bubbleY)
                    )
                    
                    // Bubble
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                indicatorColor,
                                indicatorColor.copy(alpha = 0.7f)
                            ),
                            center = Offset(bubbleX - innerRadius * 0.3f, bubbleY - innerRadius * 0.3f),
                            radius = innerRadius
                        ),
                        radius = innerRadius,
                        center = Offset(bubbleX, bubbleY)
                    )
                    
                    // Bubble highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = innerRadius * 0.4f,
                        center = Offset(bubbleX - innerRadius * 0.3f, bubbleY - innerRadius * 0.3f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Angle readings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AngleDisplay(
                    label = "Pitch",
                    angle = pitchDegrees,
                    accentColor = indicatorColor
                )
                AngleDisplay(
                    label = "Roll",
                    angle = rollDegrees,
                    accentColor = indicatorColor
                )
            }
        }
    }
}

@Composable
private fun AngleDisplay(
    label: String,
    angle: Float,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = String.format("%.1f", angle),
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "°",
                style = MaterialTheme.typography.titleMedium,
                color = accentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
