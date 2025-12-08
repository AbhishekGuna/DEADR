package com.example.deadr.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom dark color scheme for DEADR
private val DEADRDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DeepSpace,
    primaryContainer = CyanDark,
    onPrimaryContainer = CyanLight,
    
    secondary = MagentaSecondary,
    onSecondary = DeepSpace,
    secondaryContainer = MagentaDark,
    onSecondaryContainer = MagentaLight,
    
    tertiary = SuccessGreen,
    onTertiary = DeepSpace,
    tertiaryContainer = Color(0xFF00CC66),
    onTertiaryContainer = Color(0xFFAAFFDD),
    
    background = DeepSpace,
    onBackground = TextPrimary,
    
    surface = DeepSpaceLight,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    
    error = ErrorRed,
    onError = DeepSpace,
    errorContainer = Color(0xFFCC2244),
    onErrorContainer = Color(0xFFFFAABB),
    
    outline = GlassBorder,
    outlineVariant = GridLine
)

// Light color scheme (optional, but app is primarily dark-themed)
private val DEADRLightColorScheme = lightColorScheme(
    primary = CyanDark,
    onPrimary = Color.White,
    primaryContainer = CyanLight,
    onPrimaryContainer = DeepSpace,
    
    secondary = MagentaDark,
    onSecondary = Color.White,
    secondaryContainer = MagentaLight,
    onSecondaryContainer = DeepSpace,
    
    tertiary = Color(0xFF00AA66),
    onTertiary = Color.White,
    
    background = Color(0xFFF5F8FF),
    onBackground = DeepSpace,
    
    surface = Color.White,
    onSurface = DeepSpace
)

@Composable
fun DEADRTheme(
    darkTheme: Boolean = true,  // Default to dark theme for this app
    dynamicColor: Boolean = false,  // Disable dynamic color to use our custom palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DEADRDarkColorScheme else DEADRLightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepSpace.toArgb()
            window.navigationBarColor = DeepSpace.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}