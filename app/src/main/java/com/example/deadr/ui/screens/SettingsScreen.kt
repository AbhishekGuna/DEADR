package com.example.deadr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deadr.slam.MapStorage
import com.example.deadr.ui.components.AccentGlassCard
import com.example.deadr.ui.components.GlassCard
import com.example.deadr.ui.theme.CyanPrimary
import com.example.deadr.ui.theme.DeepSpace
import com.example.deadr.ui.theme.ErrorRed
import com.example.deadr.ui.theme.MagentaSecondary
import com.example.deadr.ui.theme.SuccessGreen
import com.example.deadr.ui.theme.TextSecondary
import com.example.deadr.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen for calibration, map management, and sensor configuration.
 */
@Composable
fun SettingsScreen(
    sensorMode: String,
    onSensorModeChange: (String) -> Unit,
    savedMaps: List<MapStorage.StoredMap>,
    onDeleteMap: (String) -> Unit,
    onLoadMap: (String) -> Unit,
    stepThreshold: Float,
    onStepThresholdChange: (Float) -> Unit,
    slamFusionEnabled: Boolean = false,
    onSlamFusionToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // SLAM Fusion Toggle - Main feature toggle
        item {
            AccentGlassCard(
                accentColor = if (slamFusionEnabled) SuccessGreen else CyanPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = if (slamFusionEnabled) SuccessGreen else CyanPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SLAM Fusion",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (slamFusionEnabled) 
                                "Camera + IMU fusion active" 
                            else 
                                "IMU-only dead reckoning",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (slamFusionEnabled) SuccessGreen else TextSecondary
                        )
                    }
                    Switch(
                        checked = slamFusionEnabled,
                        onCheckedChange = onSlamFusionToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SuccessGreen,
                            checkedTrackColor = SuccessGreen.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
        
        // Info text when SLAM is enabled
        if (slamFusionEnabled) {
            item {
                Text(
                    text = "📷 Visual odometry corrections are being applied to your position. Walk and compare accuracy with SLAM OFF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        
        // Sensor Configuration Section
        item {
            SettingsSection(
                title = "Sensor Configuration",
                icon = Icons.Default.Sensors,
                accentColor = CyanPrimary,
                isExpanded = expandedSection == "sensors",
                onToggle = { expandedSection = if (expandedSection == "sensors") null else "sensors" }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sensor Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    
                    SensorModeOption(
                        title = "High-Accuracy",
                        description = "Uses Rotation Vector sensor (recommended)",
                        isSelected = sensorMode == "High-Accuracy (Rotation Vector)",
                        onClick = { onSensorModeChange("High-Accuracy (Rotation Vector)") }
                    )
                    
                    SensorModeOption(
                        title = "Manual Fusion",
                        description = "Uses Accelerometer + Gyroscope + Magnetometer",
                        isSelected = sensorMode == "Low-Accuracy (Manual Fusion)",
                        onClick = { onSensorModeChange("Low-Accuracy (Manual Fusion)") }
                    )
                }
            }
        }
        
        // Calibration Section
        item {
            SettingsSection(
                title = "Step Detection",
                icon = Icons.Default.Tune,
                accentColor = MagentaSecondary,
                isExpanded = expandedSection == "calibration",
                onToggle = { expandedSection = if (expandedSection == "calibration") null else "calibration" }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Peak Threshold",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = stepThreshold,
                            onValueChange = onStepThresholdChange,
                            valueRange = 8f..15f,
                            steps = 13,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MagentaSecondary,
                                activeTrackColor = MagentaSecondary,
                                inactiveTrackColor = MagentaSecondary.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = String.format("%.1f", stepThreshold),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "Lower values = more sensitive step detection.\nHigher values = fewer false positives.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        
        // Saved Maps Section
        item {
            SettingsSection(
                title = "Saved Maps",
                icon = Icons.Default.Map,
                accentColor = SuccessGreen,
                isExpanded = expandedSection == "maps",
                onToggle = { expandedSection = if (expandedSection == "maps") null else "maps" }
            ) {
                if (savedMaps.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No saved maps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column {
                        savedMaps.forEachIndexed { index, map ->
                            MapItem(
                                map = map,
                                onLoad = { onLoadMap(map.id) },
                                onDelete = { showDeleteDialog = map.id }
                            )
                            if (index < savedMaps.size - 1) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // About Section
        item {
            SettingsSection(
                title = "About",
                icon = Icons.Default.Info,
                accentColor = WarningAmber,
                isExpanded = expandedSection == "about",
                onToggle = { expandedSection = if (expandedSection == "about") null else "about" }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AboutItem("App Version", "1.0.0")
                    AboutItem("SLAM Mode", "Visual-Inertial Odometry")
                    AboutItem("ARCore", "Not Required")
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "DEADR uses dead reckoning with sensor fusion and camera-based visual odometry to track your position without GPS or ARCore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        
        item {
            Spacer(Modifier.height(80.dp)) // Bottom padding for nav bar
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { mapId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Map?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMap(mapId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DeepSpace,
            titleContentColor = Color.White,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    AccentGlassCard(
        accentColor = accentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SensorModeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = CyanPrimary,
                unselectedColor = TextSecondary
            )
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) Color.White else TextSecondary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun MapItem(
    map: MapStorage.StoredMap,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = map.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(map.createdAt))} • ${map.metadata.stepCount} steps",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        IconButton(onClick = onLoad) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Load",
                tint = SuccessGreen
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = ErrorRed
            )
        }
    }
}

@Composable
private fun AboutItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
