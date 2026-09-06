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
    /**
     * Border drawn round whatever a dragged book would land on.
     *
     * Split from [goldSoft] because the two answer different questions. The
     * identity gold sits at 1.4:1 on parchment, which is fine for decoration
     * and useless for the only feedback a drag gives: WCAG asks 3:1 of a
     * functional non-text element, so the light theme uses a darker gold that
     * clears it while staying the same hue.
     */
    val dropHighlight: Color,
    /** Filled part of the reading-progress bar under a cover. */
    val progressFill: Color,
    /** The groove the progress bar is read against — without it there is no scale. */
    val progressTrack: Color,
    /** Height of the shelf plank under each book row. */
    val plankHeight: Dp = 14.dp,
    /** Cover aspect ratio: width / height, locked to 2:3. */
    val coverAspectRatio: Float = 2f / 3f,
    /** Golden progress bar under a cover. */
    val progressBarHeight: Dp = 3.dp,
    /**
     * The reader is deliberately dark in both themes — a page is read as paper
     * on a dark surround — so these four carry the same values either way. They
     * are tokens rather than literals because page-colour inversion is on the
     * roadmap, and it should be one edit here rather than a hunt through
     * `ReaderScreen`.
     */
    val readerBackground: Color = Color(0xFF141210),
    val readerChrome: Color = Color(0xCC141210),
    val readerOnSurface: Color = Color(0xFFFFFFFF),
    /** Scrim behind the overflow glyph, so it survives a pale cover. */
    val coverScrim: Color = Color(0x99000000)
)

val LightWoodTokens = WoodTokens(
    plankBase = WoodBase,
    plankDeep = WoodDeep,
    plankLight = WoodLight,
    plankGrainDark = Color(0x14000000),
    plankGrainLight = Color(0x0FFFFFFF),
    gold = Gold,
    goldSoft = GoldSoft,
    dropHighlight = GoldDeep,
    progressFill = GoldDeep,
    progressTrack = WoodDeep.copy(alpha = 0.25f)
)

val DarkWoodTokens = WoodTokens(
    plankBase = Color(0xFF56371C),
    plankDeep = Color(0xFF2A1C11),
    plankLight = Color(0xFF744E2B),
    plankGrainDark = Color(0x1A000000),
    plankGrainLight = Color(0x0AFFFFFF),
    gold = GoldMuted,
    goldSoft = GoldSoftMuted,
    // Dark mode already clears 3:1 with the identity golds (7.2:1 and 5.4:1),
    // so there is nothing to correct here.
    dropHighlight = GoldSoftMuted,
    progressFill = GoldMuted,
    progressTrack = Parchment.copy(alpha = 0.20f)
)

val LocalWoodTokens = staticCompositionLocalOf { LightWoodTokens }
