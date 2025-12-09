package com.example.deadr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deadr.ui.components.AccentGlassCard
import com.example.deadr.ui.components.GlassCard
import com.example.deadr.ui.components.PathCanvas
import com.example.deadr.ui.components.StepDistanceCard
import com.example.deadr.ui.theme.CyanPrimary
import com.example.deadr.ui.theme.MagentaSecondary
import com.example.deadr.ui.theme.StatBearing
import com.example.deadr.ui.theme.StatPosition
import com.example.deadr.ui.theme.StatSteps
import com.example.deadr.ui.theme.SuccessGreen
import com.example.deadr.ui.theme.TextSecondary
import com.example.deadr.ui.theme.WarningAmber

/**
 * Main Dead Reckoning screen with path visualization and statistics.
 */
@Composable
fun DeadReckoningScreen(
    pathPoints: List<Pair<Float, Float>>,
    currentHeading: Float,
    stepCount: Int,
    currentX: Double,
    currentY: Double,
    isTracking: Boolean,
    sensorMode: String,
    onStartReset: () -> Unit,
    slamFusionEnabled: Boolean = false,
    strideLengthMeters: Float = 0.75f,
    modifier: Modifier = Modifier
) {
    val bearingDegrees = Math.toDegrees(currentHeading.toDouble()).toFloat()
    val normalizedBearing = (bearingDegrees + 360) % 360
    
    val animatedBearing by animateFloatAsState(
        targetValue = normalizedBearing,
        animationSpec = tween(200),
        label = "bearing"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // Path canvas as background
        PathCanvas(
            pathPoints = pathPoints,
            currentHeading = currentHeading,
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Mode indicators row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sensor mode indicator
                GlassCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = if (isTracking) SuccessGreen else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sensorMode.take(20),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                
                // SLAM Fusion indicator
                if (slamFusionEnabled) {
                    GlassCard(
                        glowColor = SuccessGreen,
                        isSelected = true
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SuccessGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SLAM",
                                style = MaterialTheme.typography.labelMedium,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bearing card
                AccentGlassCard(
                    accentColor = StatBearing,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = StatBearing,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(animatedBearing)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${normalizedBearing.toInt()}°",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Bearing",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                
                // Steps card
                AccentGlassCard(
                    accentColor = StatSteps,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stepCount.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Steps",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Position card
            AccentGlassCard(
                accentColor = StatPosition,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "X",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatPosition
                        )
                        Text(
                            text = String.format("%.2f m", currentX),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Y",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatPosition
                        )
                        Text(
                            text = String.format("%.2f m", currentY),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatPosition
                        )
                        Text(
                            text = String.format("%.2f m", kotlin.math.sqrt(currentX * currentX + currentY * currentY)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Step Distance Card
            StepDistanceCard(
                stepCount = stepCount,
                strideLengthMeters = strideLengthMeters
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start/Reset button
            Button(
                onClick = onStartReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) MagentaSecondary else CyanPrimary
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTracking) "Reset Path" else "Start Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
