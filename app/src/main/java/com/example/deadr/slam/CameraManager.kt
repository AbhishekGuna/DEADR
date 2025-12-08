package com.example.deadr.slam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera manager for SLAM using CameraX.
 * Handles camera lifecycle and frame processing for visual odometry.
 */
class CameraManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CameraManager"
        private const val ANALYSIS_WIDTH = 640
        private const val ANALYSIS_HEIGHT = 480
    }
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null
    
    private val visualOdometry = VisualOdometry()
    
    // State flows for UI observation
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _currentFeatures = MutableStateFlow<List<VisualOdometry.FeaturePoint>>(emptyList())
    val currentFeatures: StateFlow<List<VisualOdometry.FeaturePoint>> = _currentFeatures.asStateFlow()
    
    private val _lastMotionEstimate = MutableStateFlow<VisualOdometry.MotionEstimate?>(null)
    val lastMotionEstimate: StateFlow<VisualOdometry.MotionEstimate?> = _lastMotionEstimate.asStateFlow()
    
    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()
    
    // New: Current pose from visual odometry
    private val _currentPose = MutableStateFlow(VisualOdometry.Pose())
    val currentPose: StateFlow<VisualOdometry.Pose> = _currentPose.asStateFlow()
    
    // New: Landmark count
    private val _landmarkCount = MutableStateFlow(0)
    val landmarkCount: StateFlow<Int> = _landmarkCount.asStateFlow()
    
    // New: FPS tracking
    private var lastFrameTime = 0L
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()
    
    // Callback for motion updates
    var onMotionEstimate: ((VisualOdometry.MotionEstimate) -> Unit)? = null
    
    /**
     * Start the camera for SLAM processing.
     * @param lifecycleOwner Activity or Fragment lifecycle owner
     * @param previewView Optional PreviewView for camera preview display
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null
    ) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                _isRunning.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?
    ) {
        val provider = cameraProvider ?: return
        
        // Unbind any existing use cases
        provider.unbindAll()
        
        // Camera selector - use back camera
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        
        // Preview use case (optional)
        val preview = previewView?.let { view ->
            Preview.Builder()
                .build()
                .also { preview ->
                    preview.setSurfaceProvider(view.surfaceProvider)
                }
        }
        
        // Image analysis for SLAM
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                    processFrame(imageProxy)
                }
            }
        
        try {
            // Bind use cases to camera
            if (preview != null) {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } else {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalyzer
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }
    
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // Calculate FPS
            val currentTime = System.currentTimeMillis()
            if (lastFrameTime > 0) {
                val dt = currentTime - lastFrameTime
                if (dt > 0) {
                    _fps.value = (1000 / dt).toInt()
                }
            }
            lastFrameTime = currentTime
            
            // Convert ImageProxy to Bitmap for processing
            val bitmap = imageProxy.toBitmap()
            
            // Rotate based on image rotation
            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
            
            // Process with visual odometry
            val motion = visualOdometry.processFrame(rotatedBitmap)
            
            // Update state
            _frameCount.value++
            _currentFeatures.value = visualOdometry.getCurrentFeatures()
            _currentPose.value = visualOdometry.getCurrentPose()
            _landmarkCount.value = visualOdometry.getLandmarkCount()
            
            motion?.let {
                _lastMotionEstimate.value = it
                onMotionEstimate?.invoke(it)
            }
            
            // Clean up
            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Stop the camera and release resources.
     */
    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
            visualOdometry.reset()
            _isRunning.value = false
            _frameCount.value = 0
            _currentFeatures.value = emptyList()
            _lastMotionEstimate.value = null
            _currentPose.value = VisualOdometry.Pose()
            _landmarkCount.value = 0
            _fps.value = 0
            lastFrameTime = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }
    
    /**
     * Reset visual odometry state without stopping camera.
     */
    fun resetOdometry() {
        visualOdometry.reset()
        _frameCount.value = 0
        _lastMotionEstimate.value = null
        _currentPose.value = VisualOdometry.Pose()
        _landmarkCount.value = 0
    }
    
    /**
     * Get visual odometry instance for direct access.
     */
    fun getVisualOdometry(): VisualOdometry = visualOdometry
}
