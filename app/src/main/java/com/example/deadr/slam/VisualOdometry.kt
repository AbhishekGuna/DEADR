package com.example.deadr.slam

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Visual Odometry system for SLAM without ARCore.
 * Uses FAST-like corner detection, patch-based feature tracking, and robust pose estimation.
 * 
 * Flow per frame:
 * 1. Convert frame to grayscale
 * 2. Detect corner features (FAST-like with 16-point circle)
 * 3. Apply non-maximum suppression via grid cells
 * 4. Classify features as obstacles based on clustering
 * 5. Track features from previous frame using SSD patch matching
 * 6. Estimate pose change using median displacement (robust to outliers)
 * 7. Update global pose and return motion estimate
 */
class VisualOdometry {
    
    // Feature point data class
    data class FeaturePoint(
        val x: Float,
        val y: Float,
        val score: Float,
        val type: FeatureType = FeatureType.OBSTACLE,
        val depth: Float? = null
    )
    
    enum class FeatureType {
        OBSTACLE,
        ENVIRONMENT
    }
    
    // Matched feature pair between frames
    data class FeatureMatch(
        val prev: FeaturePoint,
        val curr: FeaturePoint,
        val ssd: Float  // Sum of Squared Differences
    )
    
    // Motion estimate from visual odometry
    data class MotionEstimate(
        val deltaX: Float,
        val deltaY: Float,
        val rotation: Float,  // radians
        val confidence: Float, // 0-1
        val matchCount: Int
    )
    
    // Landmark for map storage
    data class Landmark(
        val id: Long,
        var x: Float,
        var y: Float,
        var quality: Int = 1
    )
    
    // Current pose in world coordinates
    data class Pose(
        var x: Float = 0f,
        var y: Float = 0f,
        var heading: Float = 0f  // radians
    )
    
    // State
    private var previousFrame: IntArray? = null
    private var previousFeatures: List<FeaturePoint> = emptyList()
    private var previousWidth: Int = 0
    private var previousHeight: Int = 0
    
    private val currentPose = Pose()
    private val landmarks = mutableListOf<Landmark>()
    private var landmarkIdCounter = 0L
    
    // Configuration - from reference implementation
    companion object {
        // FAST detector parameters
        private const val FAST_THRESHOLD = 20
        private const val GRID_NMS_SIZE = 6
        private const val CLUSTER_SIZE = 35
        
        // Feature limits
        private const val MAX_FEATURES = 1500
        private const val MAX_FEATURES_TO_TRACK = 300
        
        // Descriptor parameters
        private const val PATCH_SIZE = 3  // ±3 = 7x7 patch
        
        // Tracking parameters
        private const val SEARCH_RADIUS = 20
        private const val SEARCH_STEP = 3
        private const val SSD_THRESHOLD = 3000f
        private const val MIN_MATCHES_FOR_POSE = 5
        
        // Scale parameters
        private const val PIXEL_TO_METER = 0.001f
        
        // Map parameters
        private const val MAX_LANDMARKS = 500
        private const val LANDMARK_MATCH_THRESHOLD = 0.01f  // 10mm
        
        // FAST circle offsets (16 points)
        private val FAST_CIRCLE = arrayOf(
            intArrayOf(-3, 0), intArrayOf(-3, 1), intArrayOf(-2, 2), intArrayOf(-1, 3),
            intArrayOf(0, 3), intArrayOf(1, 3), intArrayOf(2, 2), intArrayOf(3, 1),
            intArrayOf(3, 0), intArrayOf(3, -1), intArrayOf(2, -2), intArrayOf(1, -3),
            intArrayOf(0, -3), intArrayOf(-1, -3), intArrayOf(-2, -2), intArrayOf(-3, -1)
        )
        
        // Grayscale conversion weights (standard luminance)
        private const val R_WEIGHT = 0.299f
        private const val G_WEIGHT = 0.587f
        private const val B_WEIGHT = 0.114f
    }
    
    /**
     * Process a new camera frame and estimate motion.
     * Returns motion estimate if enough features are matched.
     */
    fun processFrame(frame: Bitmap): MotionEstimate? {
        val width = frame.width
        val height = frame.height
        
        // Convert to grayscale array
        val grayFrame = convertToGrayscale(frame)
        
        // Detect features using FAST-like detector
        val currentFeatures = extractFeatures(grayFrame, width, height)
        
        // If this is the first frame, just store and return
        if (previousFrame == null || previousFeatures.isEmpty()) {
            previousFrame = grayFrame
            previousFeatures = currentFeatures
            previousWidth = width
            previousHeight = height
            return null
        }
        
        // Track features between frames
        val matches = trackFeatures(
            previousFrame!!, previousWidth, previousHeight,
            grayFrame, width, height,
            previousFeatures
        )
        
        // Estimate pose from matches
        val motionEstimate = if (matches.size >= MIN_MATCHES_FOR_POSE) {
            estimatePose(matches, width, height)
        } else {
            MotionEstimate(0f, 0f, 0f, 0f, matches.size)
        }
        
        // Update global pose
        if (motionEstimate.confidence > 0.1f) {
            updatePose(motionEstimate)
        }
        
        // Update map with current features
        updateMap(currentFeatures)
        
        // Store current as previous for next frame
        previousFrame = grayFrame
        previousFeatures = currentFeatures
        previousWidth = width
        previousHeight = height
        
        return motionEstimate
    }
    
    /**
     * Convert bitmap to grayscale intensity array.
     */
    private fun convertToGrayscale(bitmap: Bitmap): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            gray[i] = (R_WEIGHT * r + G_WEIGHT * g + B_WEIGHT * b).toInt()
        }
        return gray
    }
    
    /**
     * Extract corner features using FAST-like detector.
     * Uses 16-point circle, grid-based NMS, and spatial clustering for obstacle classification.
     */
    private fun extractFeatures(gray: IntArray, width: Int, height: Int): List<FeaturePoint> {
        val grid = mutableMapOf<String, FeatureCandidate>()
        
        // FAST-like corner detection with grid sampling (step=3 for performance)
        for (y in 10 until height - 10 step 3) {
            for (x in 10 until width - 10 step 3) {
                val centerIdx = y * width + x
                val centerIntensity = gray[centerIdx]
                
                var brighterCount = 0
                var darkerCount = 0
                
                // Check 16 circle pixels
                for (offset in FAST_CIRCLE) {
                    val dx = offset[0]
                    val dy = offset[1]
                    val checkIdx = (y + dy) * width + (x + dx)
                    
                    if (checkIdx >= 0 && checkIdx < gray.size) {
                        val intensity = gray[checkIdx]
                        if (intensity > centerIntensity + FAST_THRESHOLD) brighterCount++
                        if (intensity < centerIntensity - FAST_THRESHOLD) darkerCount++
                    }
                }
                
                // Corner if >= 12 pixels are consistently brighter or darker
                if (brighterCount >= 12 || darkerCount >= 12) {
                    val score = max(brighterCount, darkerCount).toFloat()
                    val gridKey = "${x / GRID_NMS_SIZE}_${y / GRID_NMS_SIZE}"
                    
                    // Non-maximum suppression within grid cell
                    val existing = grid[gridKey]
                    if (existing == null || existing.score < score) {
                        grid[gridKey] = FeatureCandidate(x, y, score, brighterCount, darkerCount)
                    }
                }
            }
        }
        
        // Extract raw candidates
        val candidates = grid.values.toList()
        
        // Spatial clustering for obstacle classification
        val clusterMap = mutableMapOf<String, MutableList<FeatureCandidate>>()
        for (feat in candidates) {
            val clusterKey = "${feat.x / CLUSTER_SIZE}_${feat.y / CLUSTER_SIZE}"
            clusterMap.getOrPut(clusterKey) { mutableListOf() }.add(feat)
        }
        
        // Calculate cluster statistics
        val clusterSizes = clusterMap.values.map { it.size }
        val totalFeatures = candidates.size
        val avgClusterSize = if (clusterMap.isNotEmpty()) totalFeatures.toFloat() / clusterMap.size else 1f
        val sortedSizes = clusterSizes.sorted()
        val medianClusterSize = if (sortedSizes.isNotEmpty()) sortedSizes[sortedSizes.size / 2] else 1
        
        // Dynamic threshold for obstacle detection
        val obstacleDensityThreshold = max(2.5f, avgClusterSize * 1.4f)
        
        // Classify features and build result list (focus on obstacles)
        val features = mutableListOf<FeaturePoint>()
        
        for (feat in candidates) {
            val clusterKey = "${feat.x / CLUSTER_SIZE}_${feat.y / CLUSTER_SIZE}"
            val clusterDensity = clusterMap[clusterKey]?.size ?: 0
            
            var isObstacle = false
            
            // Classification rules (from reference)
            // 1. Dense clusters
            if (clusterDensity >= obstacleDensityThreshold * 1.1f) isObstacle = true
            
            // 2. High-contrast edges (balanced brighter/darker counts)
            if (!isObstacle && feat.score >= 15 && 
                abs(feat.brighterCount - feat.darkerCount) <= 2) {
                isObstacle = true
            }
            
            // 3. Center-frame vertical features (likely objects)
            if (!isObstacle && 
                feat.x > width * 0.3f && feat.x < width * 0.7f &&
                feat.y > height * 0.2f && feat.y < height * 0.8f &&
                clusterDensity >= medianClusterSize * 1.4f) {
                isObstacle = true
            }
            
            // 4. Strong corners
            if (!isObstacle && feat.score >= 16) isObstacle = true
            
            // 5. Very dense clusters
            if (!isObstacle && clusterDensity > medianClusterSize * 2.2f) isObstacle = true
            
            // Add feature (keep all for tracking, but mark type)
            val type = if (isObstacle) FeatureType.OBSTACLE else FeatureType.ENVIRONMENT
            features.add(FeaturePoint(feat.x.toFloat(), feat.y.toFloat(), feat.score, type))
        }
        
        return features.take(MAX_FEATURES)
    }
    
    /**
     * Compute patch-based descriptor for a feature point.
     * Returns flattened 7x7 patch intensities.
     */
    private fun computeDescriptor(gray: IntArray, width: Int, height: Int, x: Int, y: Int): IntArray? {
        if (x < PATCH_SIZE || x >= width - PATCH_SIZE || 
            y < PATCH_SIZE || y >= height - PATCH_SIZE) {
            return null
        }
        
        val descriptor = IntArray((2 * PATCH_SIZE + 1) * (2 * PATCH_SIZE + 1))
        var idx = 0
        
        for (dy in -PATCH_SIZE..PATCH_SIZE) {
            for (dx in -PATCH_SIZE..PATCH_SIZE) {
                val pixelIdx = (y + dy) * width + (x + dx)
                descriptor[idx++] = gray[pixelIdx]
            }
        }
        
        return descriptor
    }
    
    /**
     * Track features between frames using SSD patch matching.
     * Searches within SEARCH_RADIUS with coarse SEARCH_STEP for performance.
     */
    private fun trackFeatures(
        prevGray: IntArray, prevWidth: Int, prevHeight: Int,
        currGray: IntArray, currWidth: Int, currHeight: Int,
        prevFeatures: List<FeaturePoint>
    ): List<FeatureMatch> {
        val matches = mutableListOf<FeatureMatch>()
        
        // Sample features to limit tracked count for performance
        val sampleStep = max(1, prevFeatures.size / MAX_FEATURES_TO_TRACK)
        
        for (i in prevFeatures.indices step sampleStep) {
            val prevFeat = prevFeatures[i]
            val prevX = prevFeat.x.toInt()
            val prevY = prevFeat.y.toInt()
            
            // Compute descriptor for previous feature
            val prevDesc = computeDescriptor(prevGray, prevWidth, prevHeight, prevX, prevY)
                ?: continue
            
            var bestMatch: Pair<Int, Int>? = null
            var bestSsd = Float.MAX_VALUE
            
            // Search in current frame with coarse step
            for (dy in -SEARCH_RADIUS..SEARCH_RADIUS step SEARCH_STEP) {
                for (dx in -SEARCH_RADIUS..SEARCH_RADIUS step SEARCH_STEP) {
                    val currX = prevX + dx
                    val currY = prevY + dy
                    
                    // Bounds check
                    if (currX < PATCH_SIZE || currX >= currWidth - PATCH_SIZE ||
                        currY < PATCH_SIZE || currY >= currHeight - PATCH_SIZE) {
                        continue
                    }
                    
                    // Compute descriptor for candidate match
                    val currDesc = computeDescriptor(currGray, currWidth, currHeight, currX, currY)
                        ?: continue
                    
                    // Compute SSD (Sum of Squared Differences)
                    var ssd = 0f
                    for (j in prevDesc.indices) {
                        val diff = prevDesc[j] - currDesc[j]
                        ssd += diff * diff
                    }
                    
                    if (ssd < bestSsd) {
                        bestSsd = ssd
                        bestMatch = Pair(currX, currY)
                    }
                }
            }
            
            // Accept match if SSD is below threshold
            if (bestMatch != null && bestSsd < SSD_THRESHOLD) {
                val currFeat = FeaturePoint(
                    bestMatch.first.toFloat(),
                    bestMatch.second.toFloat(),
                    prevFeat.score,
                    prevFeat.type
                )
                matches.add(FeatureMatch(prevFeat, currFeat, bestSsd))
            }
        }
        
        return matches
    }
    
    /**
     * Estimate pose change from matched features.
     * Uses median displacement for robustness to outliers.
     * Estimates rotation from angular flow.
     */
    private fun estimatePose(matches: List<FeatureMatch>, width: Int, height: Int): MotionEstimate {
        if (matches.size < MIN_MATCHES_FOR_POSE) {
            return MotionEstimate(0f, 0f, 0f, 0f, matches.size)
        }
        
        // Compute displacements
        val displacements = matches.map { m ->
            Pair(m.curr.x - m.prev.x, m.curr.y - m.prev.y)
        }
        
        // Use median for robustness (from reference)
        val sortedByDx = displacements.sortedBy { it.first }
        val sortedByDy = displacements.sortedBy { it.second }
        val medianDx = sortedByDx[sortedByDx.size / 2].first
        val medianDy = sortedByDy[sortedByDy.size / 2].second
        
        // Estimate rotation from feature flow
        val centerX = width / 2f
        val centerY = height / 2f
        var rotationSum = 0f
        var rotationCount = 0
        
        for (m in matches) {
            val angle1 = atan2(m.prev.y - centerY, m.prev.x - centerX)
            val angle2 = atan2(m.curr.y - centerY, m.curr.x - centerX)
            var dAngle = angle2 - angle1
            
            // Normalize to [-PI, PI]
            while (dAngle > Math.PI) dAngle -= (2 * Math.PI).toFloat()
            while (dAngle < -Math.PI) dAngle += (2 * Math.PI).toFloat()
            
            // Only use small angle changes (filter outliers)
            if (abs(dAngle) < 0.5f) {
                rotationSum += dAngle
                rotationCount++
            }
        }
        
        val dHeading = if (rotationCount > 0) rotationSum / rotationCount else 0f
        
        // Convert pixels to meters (negated because camera motion is opposite to scene flow)
        val deltaX = -medianDx * PIXEL_TO_METER
        val deltaY = medianDy * PIXEL_TO_METER
        
        // Calculate confidence based on match quality and count
        val avgSsd = matches.map { it.ssd }.average().toFloat()
        val matchRatio = matches.size.toFloat() / MAX_FEATURES_TO_TRACK
        val ssdConfidence = 1f / (1f + avgSsd * 0.0001f)
        val confidence = (matchRatio.coerceIn(0f, 1f) * ssdConfidence).coerceIn(0f, 1f)
        
        return MotionEstimate(deltaX, deltaY, dHeading, confidence, matches.size)
    }
    
    /**
     * Update global pose with motion estimate.
     */
    private fun updatePose(motion: MotionEstimate) {
        // Transform motion to world coordinates using current heading
        val worldDx = motion.deltaX * cos(currentPose.heading) - motion.deltaY * sin(currentPose.heading)
        val worldDy = motion.deltaX * sin(currentPose.heading) + motion.deltaY * cos(currentPose.heading)
        
        currentPose.x += worldDx
        currentPose.y += worldDy
        currentPose.heading += motion.rotation
        
        // Normalize heading to [0, 2PI]
        while (currentPose.heading < 0) currentPose.heading += (2 * Math.PI).toFloat()
        while (currentPose.heading >= 2 * Math.PI) currentPose.heading -= (2 * Math.PI).toFloat()
    }
    
    /**
     * Update map landmarks with current features.
     * Transforms features to world coordinates and matches against existing landmarks.
     */
    private fun updateMap(features: List<FeaturePoint>) {
        for (feat in features) {
            // Transform feature to world coordinates (approximate)
            val worldX = currentPose.x + feat.x * PIXEL_TO_METER * cos(currentPose.heading)
            val worldY = currentPose.y + feat.x * PIXEL_TO_METER * sin(currentPose.heading)
            
            // Try to match with existing landmark
            var matched = false
            for (landmark in landmarks) {
                val dist = hypot(landmark.x - worldX, landmark.y - worldY)
                if (dist < LANDMARK_MATCH_THRESHOLD) {
                    // Update existing landmark with weighted average
                    val weight = landmark.quality.toFloat()
                    landmark.x = (landmark.x * weight + worldX) / (weight + 1f)
                    landmark.y = (landmark.y * weight + worldY) / (weight + 1f)
                    landmark.quality = min(landmark.quality + 1, 10)
                    matched = true
                    break
                }
            }
            
            // Add new landmark if not matched and under limit
            if (!matched && landmarks.size < MAX_LANDMARKS) {
                landmarks.add(Landmark(
                    id = landmarkIdCounter++,
                    x = worldX,
                    y = worldY,
                    quality = 1
                ))
            }
        }
    }
    
    // =====================
    // Public API
    // =====================
    
    /**
     * Get detected features for visualization.
     */
    fun getCurrentFeatures(): List<FeaturePoint> = previousFeatures.toList()
    
    /**
     * Get current pose.
     */
    fun getCurrentPose(): Pose = currentPose.copy()
    
    /**
     * Get current landmarks.
     */
    fun getLandmarks(): List<Landmark> = landmarks.toList()
    
    /**
     * Get landmark count.
     */
    fun getLandmarkCount(): Int = landmarks.size
    
    /**
     * Reset the visual odometry state.
     */
    fun reset() {
        previousFrame = null
        previousFeatures = emptyList()
        previousWidth = 0
        previousHeight = 0
        currentPose.x = 0f
        currentPose.y = 0f
        currentPose.heading = 0f
        landmarks.clear()
        landmarkIdCounter = 0
    }
    
    // Helper class for feature detection
    private data class FeatureCandidate(
        val x: Int,
        val y: Int,
        val score: Float,
        val brighterCount: Int,
        val darkerCount: Int
    )
}
