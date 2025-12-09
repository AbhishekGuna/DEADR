package com.example.deadr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deadr.ui.theme.CyanPrimary
import com.example.deadr.ui.theme.ErrorRed
import com.example.deadr.ui.theme.MagentaSecondary
import com.example.deadr.ui.theme.SuccessGreen
import com.example.deadr.ui.theme.TextPrimary
import com.example.deadr.ui.theme.TextSecondary
import com.example.deadr.ui.theme.WarningAmber

/**
 * IMU Diagnostics Panel for debugging and demo purposes.
 * Shows live numeric values, sampling rate, sparkline graphs, and motion status.
 */
@Composable
fun ImuDiagnosticsPanel(
    accelX: Float,
    accelY: Float,
    accelZ: Float,
    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,
    samplingRateHz: Float,
    isMoving: Boolean,
    accelMagnitudeHistory: List<Float>,
    gyroMagnitudeHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val motionBadgeColor by animateColorAsState(
        targetValue = if (isMoving) SuccessGreen else WarningAmber,
        animationSpec = tween(300),
        label = "badge"
    )
    
    AccentGlassCard(
        accentColor = CyanPrimary,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with motion badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Diagnostics",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMU Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Motion badge
                GlassSurface(
                    cornerRadius = 8.dp,
                    alpha = 0.3f
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isMoving) Icons.Default.DirectionsWalk else Icons.Default.PauseCircle,
                            contentDescription = null,
                            tint = motionBadgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMoving) "MOVING" else "STILL",
                            color = motionBadgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Sampling rate
            Text(
                text = "Sampling: ${String.format("%.1f", samplingRateHz)} Hz",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Live IMU Values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Accelerometer
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACCELEROMETER (m/s²)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImuValueRow("X", accelX, CyanPrimary)
                    ImuValueRow("Y", accelY, CyanPrimary)
                    ImuValueRow("Z", accelZ, CyanPrimary)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Gyroscope
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GYROSCOPE (rad/s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MagentaSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ImuValueRow("X", gyroX, MagentaSecondary)
                    ImuValueRow("Y", gyroY, MagentaSecondary)
                    ImuValueRow("Z", gyroZ, MagentaSecondary)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sparkline graphs
            Text(
                text = "SIGNAL HISTORY (Last 10s)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Accel Mag",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Sparkline(
                        data = accelMagnitudeHistory,
                        color = CyanPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Gyro Mag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MagentaSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Sparkline(
                        data = gyroMagnitudeHistory,
                        color = MagentaSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImuValueRow(
    axis: String,
    value: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = axis,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = String.format("%+.3f", value),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Simple sparkline graph for displaying time-series data.
 */
@Composable
fun Sparkline(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Canvas(modifier = modifier) {
            // Draw empty placeholder line
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.dp.toPx()
            )
        }
        return
    }
    
    val minValue = data.minOrNull() ?: 0f
    val maxValue = data.maxOrNull() ?: 1f
    val range = (maxValue - minValue).coerceAtLeast(0.1f)
    
    Canvas(modifier = modifier) {
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val paddingY = 4.dp.toPx()
        val availableHeight = size.height - paddingY * 2
        
        // Draw the path
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedValue = (value - minValue) / range
            val y = paddingY + availableHeight * (1 - normalizedValue)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Draw line
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw glow effect
        drawPath(
            path = path,
            color = color.copy(alpha = 0.3f),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
