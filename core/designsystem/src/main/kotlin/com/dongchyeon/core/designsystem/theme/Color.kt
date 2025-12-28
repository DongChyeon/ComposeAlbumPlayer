package com.dongchyeon.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Main Colors
val Main1 = Color(0xFF5123DF)
val Main2 = Color(0xFF7748FF)
val Main3 = Color(0xFFAC8FFF)

// Point Colors
val Point1 = Color(0xFF4B2FFF)
val Point2 = Color(0xFFE8E2FF)

// Semantic Colors
val ErrorColor = Color(0xFFFF2633)

// Background Colors
val Background1 = Color(0xFF1F2126)

// Gray Scale
val Gray50 = Color(0xFFFFFFFF)
val Gray100 = Color(0xFFFAFAFA)
val Gray200 = Color(0xFFF8F8F8)
val Gray300 = Color(0xFFF0F0F0)
val Gray400 = Color(0xFFDEDEDE)
val Gray500 = Color(0xFFBDBFC1)
val Gray600 = Color(0xFF908397)
val Gray700 = Color(0xFF7B7F83)
val Gray800 = Color(0xFF63666A)
val Gray900 = Color(0xFF3A3D40)
val Gray950 = Color(0xFF2C2F33)
val Black = Color(0xFF121212)

@Immutable
data class AlbumPlayerColorScheme(
    // Main Colors
    val main1: Color,
    val main2: Color,
    val main3: Color,
    // Point Colors
    val point1: Color,
    val point2: Color,
    // Semantic Colors
    val error: Color,
    // Background Colors
    val background: Color,
    // Gray Scale
    val gray50: Color,
    val gray100: Color,
    val gray200: Color,
    val gray300: Color,
    val gray400: Color,
    val gray500: Color,
    val gray600: Color,
    val gray700: Color,
    val gray800: Color,
    val gray900: Color,
    val gray950: Color,
    val black: Color,
)

val DarkColorScheme =
    AlbumPlayerColorScheme(
        main1 = Main1,
        main2 = Main2,
        main3 = Main3,
        point1 = Point1,
        point2 = Point2,
        error = ErrorColor,
        background = Background1,
        gray50 = Gray50,
        gray100 = Gray100,
        gray200 = Gray200,
        gray300 = Gray300,
        gray400 = Gray400,
        gray500 = Gray500,
        gray600 = Gray600,
        gray700 = Gray700,
        gray800 = Gray800,
        gray900 = Gray900,
        gray950 = Gray950,
        black = Black,
    )
