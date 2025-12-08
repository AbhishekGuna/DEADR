package com.example.deadr.slam

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Map storage system for saving and loading SLAM maps.
 * Maps contain landmarks and path data for positional identification.
 */
class MapStorage(private val context: Context) {
    
    // Data classes for map storage
    data class Landmark(
        val id: String,
        val x: Float,
        val y: Float,
        val featureDescriptor: List<Float>,  // Simplified feature descriptor
        val timestamp: Long
    )
    
    data class PathPoint(
        val x: Float,
        val y: Float,
        val heading: Float,
        val timestamp: Long
    )
    
    data class StoredMap(
        val id: String,
        val name: String,
        val createdAt: Long,
        val landmarks: List<Landmark>,
        val pathPoints: List<PathPoint>,
        val metadata: MapMetadata
    )
    
    data class MapMetadata(
        val totalDistance: Float,
        val duration: Long,   // milliseconds
        val stepCount: Int,
        val averageConfidence: Float
    )
    
    // Current session data
    private val currentLandmarks = mutableListOf<Landmark>()
    private val currentPath = mutableListOf<PathPoint>()
    private var sessionStartTime: Long = 0
    private var landmarkCounter = 0
    
    private val gson = Gson()
    private val mapsDirectory: File
        get() = File(context.filesDir, "slam_maps").also { it.mkdirs() }
    
    /**
     * Start a new mapping session.
     */
    fun startSession() {
        currentLandmarks.clear()
        currentPath.clear()
        sessionStartTime = System.currentTimeMillis()
        landmarkCounter = 0
    }
    
    /**
     * Add a landmark detected during mapping.
     */
    fun addLandmark(x: Float, y: Float, features: List<Float>) {
        currentLandmarks.add(
            Landmark(
                id = "L${landmarkCounter++}",
                x = x,
                y = y,
                featureDescriptor = features,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Add a path point during navigation.
     */
    fun addPathPoint(x: Float, y: Float, heading: Float) {
        currentPath.add(
            PathPoint(
                x = x,
                y = y,
                heading = heading,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Save the current session as a named map.
     */
    fun saveMap(name: String, stepCount: Int, avgConfidence: Float): String {
        val mapId = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        
        val totalDistance = calculateTotalDistance()
        val duration = System.currentTimeMillis() - sessionStartTime
        
        val map = StoredMap(
            id = mapId,
            name = name.ifEmpty { "Map_$mapId" },
            createdAt = System.currentTimeMillis(),
            landmarks = currentLandmarks.toList(),
            pathPoints = currentPath.toList(),
            metadata = MapMetadata(
                totalDistance = totalDistance,
                duration = duration,
                stepCount = stepCount,
                averageConfidence = avgConfidence
            )
        )
        
        val mapFile = File(mapsDirectory, "$mapId.json")
        mapFile.writeText(gson.toJson(map))
        
        return mapId
    }
    
    /**
     * Load a map by its ID.
     */
    fun loadMap(mapId: String): StoredMap? {
        val mapFile = File(mapsDirectory, "$mapId.json")
        return if (mapFile.exists()) {
            try {
                gson.fromJson(mapFile.readText(), StoredMap::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    /**
     * Get list of all saved maps.
     */
    fun getSavedMaps(): List<StoredMap> {
        return mapsDirectory.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), StoredMap::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }
    
    /**
     * Delete a saved map.
     */
    fun deleteMap(mapId: String): Boolean {
        val mapFile = File(mapsDirectory, "$mapId.json")
        return mapFile.delete()
    }
    
    /**
     * Try to match current position against a stored map.
     * Returns the matched position if found, null otherwise.
     */
    fun matchPosition(
        currentLandmarks: List<VisualOdometry.FeaturePoint>,
        map: StoredMap
    ): Pair<Float, Float>? {
        if (map.landmarks.isEmpty() || currentLandmarks.isEmpty()) {
            return null
        }
        
        // Find best matching landmark based on feature similarity
        var bestMatch: Landmark? = null
        var bestScore = Float.MAX_VALUE
        
        for (storedLandmark in map.landmarks) {
            for (currentFeature in currentLandmarks) {
                // Simple proximity-based matching (could be enhanced with descriptor matching)
                val score = calculateMatchScore(storedLandmark, currentFeature)
                if (score < bestScore && score < 100f) {  // Threshold
                    bestScore = score
                    bestMatch = storedLandmark
                }
            }
        }
        
        return bestMatch?.let { Pair(it.x, it.y) }
    }
    
    private fun calculateMatchScore(landmark: Landmark, feature: VisualOdometry.FeaturePoint): Float {
        // Simple score based on feature score similarity and descriptor
        val scoreDiff = kotlin.math.abs((landmark.featureDescriptor.firstOrNull() ?: 0f) - feature.score)
        return scoreDiff
    }
    
    private fun calculateTotalDistance(): Float {
        if (currentPath.size < 2) return 0f
        
        var total = 0f
        for (i in 1 until currentPath.size) {
            val prev = currentPath[i - 1]
            val curr = currentPath[i]
            total += kotlin.math.sqrt(
                (curr.x - prev.x) * (curr.x - prev.x) +
                (curr.y - prev.y) * (curr.y - prev.y)
            )
        }
        return total
    }
    
    /**
     * Get current landmarks count.
     */
    fun getCurrentLandmarkCount(): Int = currentLandmarks.size
    
    /**
     * Get current path points count.
     */
    fun getCurrentPathPointCount(): Int = currentPath.size
}
