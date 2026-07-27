package com.abosalehg.khizana.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the wooden shelf rendering. Kept outside Material's
 * ColorScheme because they describe the shelf illustration, not the UI chrome.
 *
 * Every token here is read by the shelf UI — tokens that nothing consumes are
 * not a design system, they are decoration, so they get deleted instead.
 */
@Immutable
data class WoodTokens(
    val plankBase: Color,
    val plankDeep: Color,
    val plankLight: Color,
    val plankGrainDark: Color,
    val plankGrainLight: Color,
    val gold: Color,
    val goldSoft: Color,
    /** Height of the shelf plank under each book row. */
    val plankHeight: Dp = 14.dp,
    /** Cover aspect ratio: width / height, locked to 2:3. */
    val coverAspectRatio: Float = 2f / 3f,
    /** Golden progress bar under a cover. */
    val progressBarHeight: Dp = 3.dp
)

val LightWoodTokens = WoodTokens(
    plankBase = WoodBase,
    plankDeep = WoodDeep,
    plankLight = WoodLight,
    plankGrainDark = Color(0x14000000),
    plankGrainLight = Color(0x0FFFFFFF),
    gold = Gold,
    goldSoft = GoldSoft
)

val DarkWoodTokens = WoodTokens(
    plankBase = Color(0xFF56371C),
    plankDeep = Color(0xFF2A1C11),
    plankLight = Color(0xFF744E2B),
    plankGrainDark = Color(0x1A000000),
    plankGrainLight = Color(0x0AFFFFFF),
    gold = GoldMuted,
    goldSoft = GoldSoftMuted
)

val LocalWoodTokens = staticCompositionLocalOf { LightWoodTokens }
