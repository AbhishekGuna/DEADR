package com.example.deadr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deadr.ui.theme.CyanPrimary
import com.example.deadr.ui.theme.MagentaSecondary
import com.example.deadr.ui.theme.SuccessGreen
import com.example.deadr.ui.theme.TextPrimary
import com.example.deadr.ui.theme.TextSecondary

/**
 * Compact card showing step count and estimated distance.
 * Uses step count and configurable stride length to estimate travel distance.
 */
@Composable
fun StepDistanceCard(
    stepCount: Int,
    strideLengthMeters: Float,
    modifier: Modifier = Modifier
) {
    val distanceMeters = stepCount * strideLengthMeters
    
    // Animate the distance for smooth updates
    val animatedDistance by animateFloatAsState(
        targetValue = distanceMeters,
        animationSpec = tween(300),
        label = "distance"
    )
    
    AccentGlassCard(
        accentColor = SuccessGreen,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Steps section
            StepDistanceStat(
                icon = Icons.Default.DirectionsWalk,
                value = stepCount.toString(),
                unit = "steps",
                color = MagentaSecondary
            )
            
            // Vertical divider
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .padding(vertical = 4.dp)
            )
            
            // Distance section
            StepDistanceStat(
                icon = Icons.Default.Straighten,
                value = if (animatedDistance >= 1000) {
                    String.format("%.2f", animatedDistance / 1000)
                } else {
                    String.format("%.1f", animatedDistance)
                },
                unit = if (animatedDistance >= 1000) "km" else "m",
                color = SuccessGreen
            )
        }
    }
}

@Composable
private fun StepDistanceStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * Extended step distance card with more details.
 * Shows steps, distance, stride length, and walking pace estimate.
 */
@Composable
fun StepDistanceCardExpanded(
    stepCount: Int,
    strideLengthMeters: Float,
    walkingTimeSeconds: Long,
    modifier: Modifier = Modifier
) {
    val distanceMeters = stepCount * strideLengthMeters
    
    // Calculate pace (steps per minute)
    val stepsPerMinute = if (walkingTimeSeconds > 0) {
        (stepCount * 60.0 / walkingTimeSeconds).toFloat()
    } else 0f
    
    // Calculate speed (m/s)
    val speedMs = if (walkingTimeSeconds > 0) {
        distanceMeters / walkingTimeSeconds
    } else 0f
    
    AccentGlassCard(
        accentColor = SuccessGreen,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = "Steps",
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Step Counter",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stepCount.toString(),
                    label = "Steps",
                    color = MagentaSecondary
                )
                StatItem(
                    value = if (distanceMeters >= 1000) {
                        String.format("%.2f", distanceMeters / 1000)
                    } else {
                        String.format("%.1f", distanceMeters)
                    },
                    label = if (distanceMeters >= 1000) "km" else "meters",
                    color = SuccessGreen
                )
                StatItem(
                    value = String.format("%.0f", stepsPerMinute),
                    label = "steps/min",
                    color = CyanPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Secondary info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stride: ${String.format("%.2f", strideLengthMeters)} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "Speed: ${String.format("%.2f", speedMs)} m/s",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
