package com.example.deadr.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.deadr.ui.theme.CyanPrimary
import com.example.deadr.ui.theme.DeepSpace
import com.example.deadr.ui.theme.GridLine
import com.example.deadr.ui.theme.GridMajor
import com.example.deadr.ui.theme.MagentaSecondary
import com.example.deadr.ui.theme.PathGlow
import com.example.deadr.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Compose Canvas for animated path visualization with glow effects.
 * Supports zoom and pan gestures for exploring the path.
 */
@Composable
fun PathCanvas(
    pathPoints: List<Pair<Float, Float>>,
    currentHeading: Float,  // in radians
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    animatePath: Boolean = true
) {
    // Transform state for zoom/pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Animation progress (0 to 1)
    val pathProgress = remember { Animatable(0f) }
    
    // Animate path drawing when points change
    LaunchedEffect(pathPoints.size) {
        if (animatePath && pathPoints.isNotEmpty()) {
            pathProgress.snapTo(0f)
            pathProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = LinearEasing
                )
            )
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        val centerX = size.width / 2 + offsetX
        val centerY = size.height / 2 + offsetY
        val gridSize = 50f * scale
        
        // Draw grid
        if (showGrid) {
            drawGrid(centerX, centerY, gridSize)
        }
        
        // Draw path with glow
        if (pathPoints.isNotEmpty()) {
            drawPathWithGlow(
                pathPoints = pathPoints,
                centerX = centerX,
                centerY = centerY,
                scale = scale,
                progress = if (animatePath) pathProgress.value else 1f
            )
            
            // Draw current position marker
            val currentPos = pathPoints.last()
            val screenX = centerX + currentPos.first * scale * 30f
            val screenY = centerY - currentPos.second * scale * 30f  // Y is inverted
            
            drawPositionMarker(screenX, screenY, currentHeading, scale)
        } else {
            // Draw origin marker when no path
            drawOriginMarker(centerX, centerY, scale)
        }
    }
}

private fun DrawScope.drawGrid(centerX: Float, centerY: Float, gridSize: Float) {
    val width = size.width
    val height = size.height
    
    // Calculate grid offsets
    val startX = (centerX % gridSize) - gridSize
    val startY = (centerY % gridSize) - gridSize
    
    // Draw minor grid lines
    var x = startX
    while (x < width + gridSize) {
        drawLine(
            color = GridLine,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += gridSize
    }
    
    var y = startY
    while (y < height + gridSize) {
        drawLine(
            color = GridLine,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
    
    // Draw major grid lines (every 5th line)
    val majorGridSize = gridSize * 5
    val majorStartX = (centerX % majorGridSize) - majorGridSize
    val majorStartY = (centerY % majorGridSize) - majorGridSize
    
    x = majorStartX
    while (x < width + majorGridSize) {
        drawLine(
            color = GridMajor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 2f
        )
        x += majorGridSize
    }
    
    y = majorStartY
    while (y < height + majorGridSize) {
        drawLine(
            color = GridMajor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 2f
        )
        y += majorGridSize
    }
    
    // Draw axis lines through center
    drawLine(
        color = CyanPrimary.copy(alpha = 0.5f),
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = 2f
    )
    drawLine(
        color = MagentaSecondary.copy(alpha = 0.5f),
        start = Offset(centerX, 0f),
        end = Offset(centerX, height),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawPathWithGlow(
    pathPoints: List<Pair<Float, Float>>,
    centerX: Float,
    centerY: Float,
    scale: Float,
    progress: Float
) {
    if (pathPoints.size < 2) return
    
    val scaleFactor = scale * 30f
    val pointsToDraw = (pathPoints.size * progress).toInt().coerceAtLeast(2)
    
    // Convert to screen coordinates
    val screenPoints = pathPoints.take(pointsToDraw).map { (x, y) ->
        Offset(centerX + x * scaleFactor, centerY - y * scaleFactor)
    }
    
    // Create path
    val path = Path().apply {
        moveTo(screenPoints[0].x, screenPoints[0].y)
        for (i in 1 until screenPoints.size) {
            lineTo(screenPoints[i].x, screenPoints[i].y)
        }
    }
    
    // Draw outer glow
    drawPath(
        path = path,
        color = PathGlow.copy(alpha = 0.2f),
        style = Stroke(
            width = 20f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    // Draw medium glow
    drawPath(
        path = path,
        color = PathGlow.copy(alpha = 0.4f),
        style = Stroke(
            width = 10f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    // Draw main path with gradient
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(CyanPrimary, MagentaSecondary),
            start = screenPoints.first(),
            end = screenPoints.last()
        ),
        style = Stroke(
            width = 4f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    // Draw start point
    drawCircle(
        color = CyanPrimary,
        radius = 8f * scale,
        center = screenPoints.first()
    )
    drawCircle(
        color = Color.White,
        radius = 4f * scale,
        center = screenPoints.first()
    )
}

private fun DrawScope.drawPositionMarker(x: Float, y: Float, heading: Float, scale: Float) {
    val markerSize = 15f * scale
    
    // Draw glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SuccessGreen.copy(alpha = 0.5f),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = markerSize * 3
        ),
        radius = markerSize * 3,
        center = Offset(x, y)
    )
    
    // Draw direction indicator (triangle)
    val path = Path().apply {
        // Point in direction of heading
        val tipX = x + sin(heading) * markerSize * 1.5f
        val tipY = y - cos(heading) * markerSize * 1.5f
        
        val backAngle1 = heading + 2.5f
        val backAngle2 = heading - 2.5f
        
        val back1X = x + sin(backAngle1) * markerSize * 0.5f
        val back1Y = y - cos(backAngle1) * markerSize * 0.5f
        
        val back2X = x + sin(backAngle2) * markerSize * 0.5f
        val back2Y = y - cos(backAngle2) * markerSize * 0.5f
        
        moveTo(tipX, tipY)
        lineTo(back1X, back1Y)
        lineTo(back2X, back2Y)
        close()
    }
    
    drawPath(
        path = path,
        color = SuccessGreen
    )
    
    // Draw center dot
    drawCircle(
        color = Color.White,
        radius = 4f * scale,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawOriginMarker(centerX: Float, centerY: Float, scale: Float) {
    // Draw crosshair at origin
    val crossSize = 20f * scale
    
    // Horizontal line
    drawLine(
        color = CyanPrimary,
        start = Offset(centerX - crossSize, centerY),
        end = Offset(centerX + crossSize, centerY),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
    
    // Vertical line
    drawLine(
        color = MagentaSecondary,
        start = Offset(centerX, centerY - crossSize),
        end = Offset(centerX, centerY + crossSize),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
    
    // Center circle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.3f),
                Color.Transparent
            ),
            radius = 30f * scale
        ),
        radius = 30f * scale,
        center = Offset(centerX, centerY)
    )
    
    drawCircle(
        color = Color.White,
        radius = 6f * scale,
        center = Offset(centerX, centerY)
    )
}
