package com.dongchyeon.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalAlbumPlayerColorScheme = staticCompositionLocalOf { DarkColorScheme }
private val LocalAlbumPlayerTypography = staticCompositionLocalOf { AlbumPlayerTypography }

private val MaterialDarkColorScheme = darkColorScheme(
    primary = Main2,
    onPrimary = Gray50,
    primaryContainer = Main1,
    onPrimaryContainer = Gray50,
    background = Background1,
    onBackground = Gray50,
    surface = Gray950,
    onSurface = Gray50,
    error = ErrorColor,
    onError = Gray50,
)

@Composable
fun AlbumPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAlbumPlayerColorScheme provides DarkColorScheme,
        LocalAlbumPlayerTypography provides AlbumPlayerTypography
    ) {
        MaterialTheme(
            colorScheme = MaterialDarkColorScheme,
            typography = AlbumPlayerTypography,
            content = content
        )
    }
}

object AlbumPlayerTheme {
    val colorScheme: AlbumPlayerColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAlbumPlayerColorScheme.current
    
    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalAlbumPlayerTypography.current
}
