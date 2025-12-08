package com.example.deadr.ui.screens

import android.Manifest
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.deadr.slam.CameraManager
import com.example.deadr.slam.VisualOdometry
import com.example.deadr.ui.components.AccentGlassCard
import com.example.deadr.ui.components.GlassCard
import com.example.deadr.ui.theme.CyanPrimary
import com.example.deadr.ui.theme.DeepSpace
import com.example.deadr.ui.theme.ErrorRed
import com.example.deadr.ui.theme.MagentaSecondary
import com.example.deadr.ui.theme.StatConfidence
import com.example.deadr.ui.theme.SuccessGreen
import com.example.deadr.ui.theme.TextSecondary
import com.example.deadr.ui.theme.WarningAmber
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Camera Map screen for SLAM functionality.
 * Shows camera preview with feature point visualization.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraMapScreen(
    cameraManager: CameraManager?,
    isMapping: Boolean,
    featurePoints: List<VisualOdometry.FeaturePoint>,
    frameCount: Int,
    confidence: Float,
    landmarkCount: Int,
    onStartMapping: () -> Unit,
    onStopMapping: () -> Unit,
    onSaveMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    // Handle camera lifecycle
    DisposableEffect(cameraManager, isMapping) {
        if (cameraPermissionState.status.isGranted && isMapping && cameraManager != null && previewView != null) {
            cameraManager.startCamera(lifecycleOwner, previewView)
        }
        onDispose {
            cameraManager?.stopCamera()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when {
            !cameraPermissionState.status.isGranted -> {
                // Permission not granted UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepSpace)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (cameraPermissionState.status.shouldShowRationale) {
                            "Camera access is needed for SLAM mapping. This allows the app to detect visual features for position tracking."
                        } else {
                            "Please grant camera permission to use SLAM features."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
            
            cameraManager == null -> {
                // Camera not available
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepSpace)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Camera Not Available",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }
            
            else -> {
                // Camera preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Feature points overlay with obstacle/environment color coding
                if (isMapping && featurePoints.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val time = System.currentTimeMillis() / 1000f
                        
                        for ((idx, point) in featurePoints.withIndex()) {
                            // Normalize to screen coordinates (640x480 analysis resolution)
                            val x = (point.x / 640f) * size.width
                            val y = (point.y / 480f) * size.height
                            
                            // Pulsing animation
                            val pulse = (kotlin.math.sin(time * 2 + idx * 0.05f) * 0.15f + 0.85f)
                            
                            // Color based on feature type
                            val isObstacle = point.type == VisualOdometry.FeatureType.OBSTACLE
                            val baseColor = if (isObstacle) ErrorRed else CyanPrimary
                            
                            // Draw outer glow
                            drawCircle(
                                color = baseColor.copy(alpha = 0.2f * pulse),
                                radius = 12f,
                                center = Offset(x, y)
                            )
                            
                            // Draw main point
                            val radius = if (isObstacle) 4f else 3f
                            drawCircle(
                                color = baseColor.copy(alpha = 0.9f * pulse),
                                radius = radius * pulse,
                                center = Offset(x, y)
                            )
                            
                            // Draw ring for obstacles
                            if (isObstacle) {
                                drawCircle(
                                    color = baseColor.copy(alpha = 0.6f * pulse),
                                    radius = (radius + 3f) * pulse,
                                    center = Offset(x, y),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                                )
                            }
                        }
                    }
                }
                
                // Overlay UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Recording indicator
                    AnimatedVisibility(
                        visible = isMapping,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        GlassCard {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MAPPING",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "$frameCount frames",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Features card
                        AccentGlassCard(
                            accentColor = CyanPrimary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = featurePoints.size.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Features",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        // Landmarks card
                        AccentGlassCard(
                            accentColor = MagentaSecondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = landmarkCount.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Landmarks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        // Confidence card
                        AccentGlassCard(
                            accentColor = StatConfidence,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${(confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Confidence",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isMapping) {
                            // Stop button
                            Button(
                                onClick = onStopMapping,
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Stop", fontWeight = FontWeight.Bold)
                            }
                            
                            // Save button
                            Button(
                                onClick = onSaveMap,
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save Map", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Start mapping button
                            Button(
                                onClick = onStartMapping,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Start Mapping", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
