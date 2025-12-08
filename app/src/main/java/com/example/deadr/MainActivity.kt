package com.example.deadr

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.deadr.slam.CameraManager
import com.example.deadr.slam.MapStorage
import com.example.deadr.slam.VisualOdometry
import com.example.deadr.ui.navigation.GlassBottomNavBar
import com.example.deadr.ui.navigation.Screen
import com.example.deadr.ui.navigation.screens
import com.example.deadr.ui.screens.CameraMapScreen
import com.example.deadr.ui.screens.DeadReckoningScreen
import com.example.deadr.ui.screens.SettingsScreen
import com.example.deadr.ui.theme.DEADRTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1
        private const val ALPHA = 0.98f
        private const val NS_TO_S = 1.0f / 1_000_000_000.0f
        private const val DEFAULT_PEAK_THRESHOLD = 11.5f
        private const val TROUGH_THRESHOLD = 8.5f
        private const val DEBOUNCE_TIME_MS = 250
    }

    // Sensor Manager
    private lateinit var sensorManager: SensorManager

    // Sensor References
    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    // Sensor Mode
    private enum class SensorMode { NONE, MODERN, MANUAL }
    private var currentMode = SensorMode.NONE

    // Sensor data arrays
    private val accelData = FloatArray(3)
    private val gyroData = FloatArray(3)
    private val magData = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // State variables (using mutable state for Compose)
    private var _currentX = mutableDoubleStateOf(0.0)
    private var _currentY = mutableDoubleStateOf(0.0)
    private var _stepCount = mutableIntStateOf(0)
    private var _fusedAzimuth = mutableFloatStateOf(0.0f)
    private var _isTracking = mutableStateOf(false)
    private var _sensorModeText = mutableStateOf("Initializing...")
    private var _pathPoints = mutableStateListOf<Pair<Float, Float>>()
    private var _peakThreshold = mutableFloatStateOf(DEFAULT_PEAK_THRESHOLD)

    // Step detection state
    private var lastStepTimestamp: Long = 0
    private var lastGyroTimestamp: Long = 0
    private var isPeak: Boolean = false
    private var isReady = false

    // SLAM components
    private var cameraManager: CameraManager? = null
    private var mapStorage: MapStorage? = null
    private var _isMapping = mutableStateOf(false)
    private var _featurePoints = mutableStateListOf<VisualOdometry.FeaturePoint>()
    private var _frameCount = mutableIntStateOf(0)
    private var _confidence = mutableFloatStateOf(0f)
    private var _savedMaps = mutableStateListOf<MapStorage.StoredMap>()
    
    // SLAM Fusion toggle - when ON, uses visual odometry to correct dead reckoning
    private var _slamFusionEnabled = mutableStateOf(false)
    
    // Visual odometry correction accumulator
    // Stores camera-based motion estimates between steps for fusion
    private var voAccumulatedDx = 0f
    private var voAccumulatedDy = 0f
    private var voAccumulatedHeading = 0f
    private var voAccumulatedConfidence = 0f
    private var voSampleCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SLAM components
        try {
            cameraManager = CameraManager(this)
            mapStorage = MapStorage(this)
            refreshSavedMaps()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SLAM components", e)
        }

        // Check and request permissions
        if (!checkPermissions()) {
            requestPermissions()
        }

        // Initialize sensors
        initializeSensors()

        setContent {
            DEADRTheme {
                MainApp()
            }
        }
    }

    @Composable
    private fun MainApp() {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.DeadReckoning) }
        
        // Observe sensor state
        val currentX by _currentX
        val currentY by _currentY
        val stepCount by _stepCount
        val fusedAzimuth by _fusedAzimuth
        val isTracking by _isTracking
        val sensorMode by _sensorModeText
        val pathPoints = _pathPoints.toList()
        val peakThreshold by _peakThreshold
        
        // SLAM state
        val isMapping by _isMapping
        val featurePoints = _featurePoints.toList()
        val frameCount by _frameCount
        val confidence by _confidence
        val savedMaps = _savedMaps.toList()
        val slamFusionEnabled by _slamFusionEnabled
        
        // Lifecycle observer for sensor registration
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> registerSensors()
                    Lifecycle.Event.ON_PAUSE -> unregisterSensors()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        
        // Collect camera manager state
        LaunchedEffect(cameraManager) {
            cameraManager?.let { cm ->
                cm.currentFeatures.collect { features ->
                    _featurePoints.clear()
                    _featurePoints.addAll(features)
                }
            }
        }
        
        LaunchedEffect(cameraManager) {
            cameraManager?.let { cm ->
                cm.frameCount.collect { count ->
                    _frameCount.intValue = count
                }
            }
        }
        
        LaunchedEffect(cameraManager) {
            cameraManager?.let { cm ->
                cm.lastMotionEstimate.collect { motion ->
                    motion?.let {
                        _confidence.floatValue = it.confidence
                        
                        // Accumulate visual odometry for SLAM fusion
                        if (_slamFusionEnabled.value && it.confidence > 0.2f) {
                            voAccumulatedDx += it.deltaX
                            voAccumulatedDy += it.deltaY
                            voAccumulatedHeading += it.rotation
                            voAccumulatedConfidence += it.confidence
                            voSampleCount++
                        }
                        
                        // Add landmark if confidence is high enough
                        if (it.confidence > 0.5f && isMapping) {
                            mapStorage?.addLandmark(
                                currentX.toFloat(),
                                currentY.toFloat(),
                                featurePoints.map { fp -> fp.score }
                            )
                        }
                    }
                }
            }
        }

        Scaffold(
            bottomBar = {
                GlassBottomNavBar(
                    currentRoute = currentScreen.route,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        Screen.DeadReckoning -> {
                            DeadReckoningScreen(
                                pathPoints = pathPoints,
                                currentHeading = fusedAzimuth,
                                stepCount = stepCount,
                                currentX = currentX,
                                currentY = currentY,
                                isTracking = isTracking,
                                sensorMode = sensorMode,
                                onStartReset = { resetState() },
                                slamFusionEnabled = slamFusionEnabled
                            )
                        }
                        Screen.CameraMap -> {
                            CameraMapScreen(
                                cameraManager = cameraManager,
                                isMapping = isMapping,
                                featurePoints = featurePoints,
                                frameCount = frameCount,
                                confidence = confidence,
                                landmarkCount = mapStorage?.getCurrentLandmarkCount() ?: 0,
                                onStartMapping = { startMapping() },
                                onStopMapping = { stopMapping() },
                                onSaveMap = { saveCurrentMap() }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                sensorMode = sensorMode,
                                onSensorModeChange = { /* Sensor mode is auto-detected */ },
                                savedMaps = savedMaps,
                                onDeleteMap = { mapId -> deleteMap(mapId) },
                                onLoadMap = { mapId -> loadMap(mapId) },
                                stepThreshold = peakThreshold,
                                onStepThresholdChange = { threshold ->
                                    _peakThreshold.floatValue = threshold
                                },
                                slamFusionEnabled = slamFusionEnabled,
                                onSlamFusionToggle = { enabled ->
                                    _slamFusionEnabled.value = enabled
                                    if (enabled && !_isMapping.value) {
                                        // Auto-start camera when enabling fusion
                                        startMapping()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val activityRecognitionPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        val fineLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val cameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        return activityRecognitionPermission == PackageManager.PERMISSION_GRANTED &&
                fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissionsToRequest.add(Manifest.permission.CAMERA)

        ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "All permissions granted")
            } else {
                Log.w(TAG, "Some permissions were denied")
            }
        }
    }

    private fun initializeSensors() {
        try {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            when {
                rotationSensor != null && accelerometer != null -> {
                    currentMode = SensorMode.MODERN
                    _sensorModeText.value = "High-Accuracy (Rotation Vector)"
                    Log.d(TAG, "Using MODERN sensor mode")
                }
                accelerometer != null && gyroscope != null && magnetometer != null -> {
                    currentMode = SensorMode.MANUAL
                    _sensorModeText.value = "Low-Accuracy (Manual Fusion)"
                    Log.d(TAG, "Using MANUAL sensor mode")
                }
                else -> {
                    currentMode = SensorMode.NONE
                    _sensorModeText.value = "No sensors available"
                    Log.e(TAG, "No suitable sensors found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize sensors", e)
            currentMode = SensorMode.NONE
            _sensorModeText.value = "Sensor initialization failed"
        }
    }

    private fun registerSensors() {
        if (!::sensorManager.isInitialized) return
        
        try {
            when (currentMode) {
                SensorMode.MODERN -> {
                    rotationSensor?.let {
                        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                    accelerometer?.let {
                        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                }
                SensorMode.MANUAL -> {
                    accelerometer?.let {
                        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                    gyroscope?.let {
                        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                    magnetometer?.let {
                        sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    }
                }
                SensorMode.NONE -> { /* Do nothing */ }
            }
            _isTracking.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register sensors", e)
        }
    }

    private fun unregisterSensors() {
        if (::sensorManager.isInitialized) {
            try {
                sensorManager.unregisterListener(this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister sensors", e)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        try {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    _fusedAzimuth.floatValue = (orientationAngles[0] + 2 * PI.toFloat()) % (2 * PI.toFloat())
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelData, 0, minOf(3, event.values.size))
                    detectStep()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    System.arraycopy(event.values, 0, gyroData, 0, minOf(3, event.values.size))
                    processGyro(event.timestamp)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magData, 0, minOf(3, event.values.size))
                    isReady = true
                }
            }
            
            if (currentMode == SensorMode.MANUAL && isReady) {
                updateOrientation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sensor data", e)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be ignored for this implementation
    }

    private fun processGyro(timestamp: Long) {
        if (lastGyroTimestamp == 0L) {
            lastGyroTimestamp = timestamp
            return
        }
        val dt = (timestamp - lastGyroTimestamp) * NS_TO_S
        val gyroZ = gyroData[2]
        val deltaAzimuth = gyroZ * dt
        var newAzimuth = (ALPHA * (_fusedAzimuth.floatValue + deltaAzimuth) + (1 - ALPHA) * orientationAngles[0])
        newAzimuth = (newAzimuth + 2 * PI.toFloat()) % (2 * PI.toFloat())
        _fusedAzimuth.floatValue = newAzimuth
        lastGyroTimestamp = timestamp
    }

    private fun updateOrientation() {
        try {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magData)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating orientation", e)
        }
    }

    private fun detectStep() {
        val mag = sqrt(accelData[0].pow(2) + accelData[1].pow(2) + accelData[2].pow(2))
        val currentTime = System.currentTimeMillis()
        val threshold = _peakThreshold.floatValue
        
        if (mag > threshold && !isPeak && (currentTime - lastStepTimestamp > DEBOUNCE_TIME_MS)) {
            isPeak = true
        } else if (mag < TROUGH_THRESHOLD && isPeak) {
            isPeak = false
            _stepCount.intValue++
            val timeSinceLastStepMs = (currentTime - lastStepTimestamp).coerceAtLeast(1)
            lastStepTimestamp = currentTime
            onStepDetected(timeSinceLastStepMs)
        }
    }

    private fun onStepDetected(timeSinceLastStepMs: Long) {
        val frequency = 1000.0 / timeSinceLastStepMs
        val strideLength: Double = when {
            frequency < 1.5 -> 0.60
            frequency < 2.0 -> 0.75
            else -> 1.00
        }
        
        val bearing = _fusedAzimuth.floatValue
        
        // Calculate IMU-based motion (dead reckoning)
        val imuDeltaX = strideLength * sin(bearing)
        val imuDeltaY = strideLength * cos(bearing)
        
        // Apply SLAM fusion if enabled and we have visual odometry data
        val finalDeltaX: Double
        val finalDeltaY: Double
        
        if (_slamFusionEnabled.value && voSampleCount > 0) {
            // Calculate average visual odometry motion
            val avgVoDx = voAccumulatedDx / voSampleCount
            val avgVoDy = voAccumulatedDy / voSampleCount
            val avgConfidence = (voAccumulatedConfidence / voSampleCount).coerceIn(0f, 1f)
            
            // Adaptive weighting based on confidence
            // High confidence (0.7+) = trust camera more, Low confidence = trust IMU more
            val cameraWeight = (avgConfidence * 0.6f).coerceIn(0f, 0.5f)  // Max 50% camera influence
            val imuWeight = 1f - cameraWeight
            
            // Fuse: weighted combination of IMU and camera motion
            // Scale VO to match step-based estimation (camera gives smaller continuous motion)
            val voScale = 10f  // Scale factor to match stride-based motion
            finalDeltaX = imuWeight * imuDeltaX + cameraWeight * (avgVoDx * voScale)
            finalDeltaY = imuWeight * imuDeltaY + cameraWeight * (avgVoDy * voScale)
            
            // Also apply heading correction from visual odometry
            if (avgConfidence > 0.5f) {
                val headingCorrection = voAccumulatedHeading / voSampleCount
                _fusedAzimuth.floatValue += (headingCorrection * cameraWeight * 0.3f)
            }
            
            Log.d(TAG, "SLAM Fusion: conf=$avgConfidence, cameraW=$cameraWeight, " +
                    "imu=(${ "%.3f".format(imuDeltaX) },${ "%.3f".format(imuDeltaY) }), " +
                    "vo=(${ "%.3f".format(avgVoDx) },${ "%.3f".format(avgVoDy) }), " +
                    "final=(${ "%.3f".format(finalDeltaX) },${ "%.3f".format(finalDeltaY) })")
            
            // Reset accumulator for next step
            voAccumulatedDx = 0f
            voAccumulatedDy = 0f
            voAccumulatedHeading = 0f
            voAccumulatedConfidence = 0f
            voSampleCount = 0
        } else {
            // No SLAM fusion - pure IMU dead reckoning
            finalDeltaX = imuDeltaX
            finalDeltaY = imuDeltaY
        }
        
        _currentX.doubleValue += finalDeltaX
        _currentY.doubleValue += finalDeltaY
        
        // Add to path
        _pathPoints.add(Pair(_currentX.doubleValue.toFloat(), _currentY.doubleValue.toFloat()))
        
        // Add path point to map storage if mapping
        if (_isMapping.value) {
            mapStorage?.addPathPoint(
                _currentX.doubleValue.toFloat(),
                _currentY.doubleValue.toFloat(),
                bearing
            )
        }
    }
    
    private fun resetState() {
        Log.d(TAG, "Resetting state")
        _stepCount.intValue = 0
        _currentX.doubleValue = 0.0
        _currentY.doubleValue = 0.0
        lastStepTimestamp = 0L
        _pathPoints.clear()
        _pathPoints.add(Pair(0f, 0f))
        _fusedAzimuth.floatValue = 0.0f
        lastGyroTimestamp = 0L
        isReady = false
        isPeak = false
        
        if (currentMode == SensorMode.MANUAL) {
            updateOrientation()
            _fusedAzimuth.floatValue = orientationAngles[0]
        }
    }

    // SLAM functions
    private fun startMapping() {
        mapStorage?.startSession()
        _isMapping.value = true
        Log.d(TAG, "Started mapping session")
    }

    private fun stopMapping() {
        cameraManager?.stopCamera()
        _isMapping.value = false
        Log.d(TAG, "Stopped mapping session")
    }

    private fun saveCurrentMap() {
        try {
            val mapId = mapStorage?.saveMap(
                name = "Map ${System.currentTimeMillis()}",
                stepCount = _stepCount.intValue,
                avgConfidence = _confidence.floatValue
            )
            Log.d(TAG, "Saved map: $mapId")
            refreshSavedMaps()
            stopMapping()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save map", e)
        }
    }

    private fun deleteMap(mapId: String) {
        try {
            mapStorage?.deleteMap(mapId)
            refreshSavedMaps()
            Log.d(TAG, "Deleted map: $mapId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete map", e)
        }
    }

    private fun loadMap(mapId: String) {
        try {
            val map = mapStorage?.loadMap(mapId)
            map?.let {
                Log.d(TAG, "Loaded map: ${it.name} with ${it.landmarks.size} landmarks")
                // Could implement map matching here
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load map", e)
        }
    }

    private fun refreshSavedMaps() {
        try {
            val maps = mapStorage?.getSavedMaps() ?: emptyList()
            _savedMaps.clear()
            _savedMaps.addAll(maps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh saved maps", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.stopCamera()
    }
}
