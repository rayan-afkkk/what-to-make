package com.whattocook.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ---- Raw palette ----------------------------------------------------------

internal val Black = Color(0xFF000000)
internal val Charcoal = Color(0xFF1C1A18)
internal val CharcoalRaised = Color(0xFF2A2724)
internal val CharcoalBorder = Color(0xFF3A3631)
internal val Cream = Color(0xFFF5EFE6)
internal val Stone = Color(0xFFA8A29A)
internal val StoneDim = Color(0xFF6F6A63)

internal val OffWhite = Color(0xFFFAF6EF)
internal val White = Color(0xFFFFFFFF)
internal val Sand = Color(0xFFF0EADF)
internal val SandBorder = Color(0xFFE4DCCF)
internal val Ink = Color(0xFF1C1A18)
internal val InkSoft = Color(0xFF6E675F)
internal val InkDim = Color(0xFF9A938A)

internal val Ember = Color(0xFFFF6B35)
internal val Coral = Color(0xFFF0645F)

internal val PastelPink = Color(0xFFF4B5AB)
internal val PastelPinkInk = Color(0xFFA2342A)
internal val PastelBlue = Color(0xFFB8CCF2)
internal val PastelBlueInk = Color(0xFF2B4C8C)
internal val PastelPurple = Color(0xFFCDB4F0)
internal val PastelPurpleInk = Color(0xFF5B2E9A)
internal val PastelGreen = Color(0xFFB5DDB8)
internal val PastelGreenInk = Color(0xFF2D6A35)
internal val PastelAmber = Color(0xFFF7D9A8)
internal val PastelAmberInk = Color(0xFF8A5A10)

// ---- Semantic tokens -------------------------------------------------------

@Immutable
data class Pastel(
    val container: Color,
    val content: Color,
)

@Immutable
data class Pastels(
    val pink: Pastel,
    val blue: Pastel,
    val purple: Pastel,
    val green: Pastel,
    val amber: Pastel,
) {
    val all: List<Pastel> get() = listOf(pink, blue, purple, green, amber)
}

@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val cardRaised: Color,
    val cardBorder: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val actionButton: Color,
    val onActionButton: Color,
    val destructive: Color,
    val pastels: Pastels,
    /** Background tints for recipe cards on Home, cycled per recipe. */
    val recipeTints: List<Color>,
)

private val SharedPastels = Pastels(
    pink = Pastel(PastelPink, PastelPinkInk),
    blue = Pastel(PastelBlue, PastelBlueInk),
    purple = Pastel(PastelPurple, PastelPurpleInk),
    green = Pastel(PastelGreen, PastelGreenInk),
    amber = Pastel(PastelAmber, PastelAmberInk),
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Black,
    card = Charcoal,
    cardRaised = CharcoalRaised,
    cardBorder = CharcoalBorder,
    divider = Color(0xFF26231F),
    textPrimary = Cream,
    textSecondary = Stone,
    textMuted = StoneDim,
    accent = Ember,
    onAccent = White,
    accentSoft = Color(0xFF3B2422),
    actionButton = Cream,
    onActionButton = Ink,
    destructive = Coral,
    pastels = SharedPastels,
    recipeTints = listOf(
        Color(0xFF1E3A57),
        Charcoal,
        Color(0xFF233A2B),
        Color(0xFF34253F),
        Color(0xFF3D2B1D),
    ),
)

val LightAppColors = AppColors(
    isDark = false,
    background = OffWhite,
    card = White,
    cardRaised = Sand,
    cardBorder = SandBorder,
    divider = Color(0xFFEAE3D8),
    textPrimary = Ink,
    textSecondary = InkSoft,
    textMuted = InkDim,
    accent = Ember,
    onAccent = White,
    accentSoft = Color(0xFFFFE3D8),
    actionButton = Ink,
    onActionButton = Cream,
    destructive = Coral,
    pastels = SharedPastels,
    recipeTints = listOf(
        Color(0xFFDDE7FA),
        White,
        Color(0xFFE2F1E3),
        Color(0xFFEDE4FA),
        Color(0xFFFBEBD3),
    ),
)
