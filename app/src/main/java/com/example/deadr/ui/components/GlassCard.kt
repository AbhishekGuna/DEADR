package com.example.deadr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.deadr.ui.theme.GlassBorder
import com.example.deadr.ui.theme.GlassSurface
import com.example.deadr.ui.theme.GlassSurfaceLight

/**
 * Reusable glassmorphism card component with blur-like effect.
 * Creates a frosted glass appearance with subtle gradient borders.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    glowColor: Color = Color.Transparent,
    isSelected: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val animatedGlow by animateFloatAsState(
        targetValue = if (isSelected) 0.6f else 0f,
        animationSpec = tween(300),
        label = "glow"
    )
    
    val shape = RoundedCornerShape(cornerRadius)
    
    // Outer glow effect (when selected or glowColor is set)
    val glowModifier = if (glowColor != Color.Transparent || isSelected) {
        Modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = animatedGlow * 0.3f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .padding(4.dp)
    } else {
        Modifier
    }
    
    Box(
        modifier = modifier
            .then(glowModifier)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassSurfaceLight,
                        GlassSurface
                    )
                ),
                shape = shape
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassBorder,
                        GlassBorder.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

/**
 * Glassmorphism card with accent color on top border.
 */
@Composable
fun AccentGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        GlassSurface
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.5f),
                        GlassBorder.copy(alpha = 0.2f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

/**
 * Simple glass surface without border decorations.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    alpha: Float = 0.1f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = alpha)),
        content = content
    )
}
