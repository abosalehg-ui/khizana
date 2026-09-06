package com.abosalehg.khizana.ui.theme

import androidx.compose.ui.graphics.Color

// Core palette — warm wood + gold identity (see docs section 9.1).
val WoodDeep = Color(0xFF3E2A1B)      // dark wood: night background / shelf depth
val WoodBase = Color(0xFF6B4423)      // base wood: shelf plank
val WoodLight = Color(0xFF8B5E34)     // light wood: front edge / highlight
val Gold = Color(0xFFC9A227)          // gold: details, progress bar, borders
val GoldSoft = Color(0xFFE3C567)      // soft gold: highlights and active states
val Parchment = Color(0xFFF5EBDA)     // creamy parchment: light backgrounds
val Ink = Color(0xFF2A1F16)           // primary text

// Dark-mode gold is desaturated ~15% so it does not glare in the dark.
val GoldMuted = Color(0xFFBFA13C)
val GoldSoftMuted = Color(0xFFD4BC6E)

// Gold dark enough to be a *functional* mark on parchment: 4.2:1 on the
// background and 3.7:1 on the dimmest surface, where the identity Gold manages
// 2.05:1 and GoldSoft only 1.43:1. Used for the drop highlight and the progress
// bar in light mode — the things the reader has to see rather than merely
// enjoy. WCAG 1.4.11 asks 3:1 of both.
val GoldDeep = Color(0xFF8A6D12)

// Supporting shades derived from the core palette.
val ParchmentHigh = Color(0xFFFBF4E6) // elevated surfaces in light mode
val ParchmentDim = Color(0xFFEADDC4)  // surface variant in light mode
val WoodNight = Color(0xFF332214)     // elevated surfaces in dark mode
val WoodCavity = Color(0xFF4A3220)    // surface variant in dark mode
val InkSoft = Color(0xFF5A4632)       // secondary text on parchment
val ParchmentSoft = Color(0xFFD8C9AE) // secondary text on dark wood
val ErrorRed = Color(0xFF9B3B2E)      // warm brick red, fits the palette
val ErrorRedNight = Color(0xFFE08A7B)
