package com.pixelquest.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R

val PressStart2P = FontFamily(
    Font(R.font.press_start_2p, FontWeight.Normal)
)

val PixelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 36.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        lineHeight = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PressStart2P,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        lineHeight = 12.sp
    )
)
