@file:OptIn(ExperimentalTextApi::class)

package com.abosalehg.khizana.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.abosalehg.khizana.R

/** Amiri — headings, shelf titles, generated cover titles. Bundled locally. */
val AmiriFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
    Font(R.font.amiri_italic, FontWeight.Normal, FontStyle.Italic)
)

/** Cairo — body and UI text. Variable font, instantiated per weight. */
val CairoFamily = FontFamily(
    Font(
        R.font.cairo_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.cairo_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.cairo_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.cairo_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

val KhizanaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 60.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 48.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    titleSmall = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
