package com.example.sleepready.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.sleepready.domain.model.ColorScheme as DomainColorScheme

// --- 旧调色板别名 ----------------------------------------------------------
@Volatile private var currentAccent: Color = Color(0xFFE2A654)
@Volatile private var currentSecondary: Color = Color(0xFFC9A87C)

internal fun setLegacyAccents(primary: Color, secondary: Color) {
    currentAccent = primary
    currentSecondary = secondary
}

val WarmOrange: Color get() = currentAccent
val MoonlightYellow: Color get() = currentSecondary

// --- 状态点专用色 ----------------------------------------------------------
val StatusIdleColor    = Color(0xFFF4C27A)
val StatusPausedColor  = Color(0xFF8BB8C8)
val StatusDoneColor    = Color(0xFF8ECB8E)
val InProgressBlue  = StatusPausedColor
val ArchivedGray    = StatusDoneColor
val DeepNight       = Color(0xFF12110F)
val NightSurface    = Color(0xFF1E1C19)
val DimText         = Color(0xFFB8B5B0)

// --- 三套预设调色板 -------------------------------------------------------
internal data class PaletteSpec(
    val light: androidx.compose.material3.ColorScheme,
    val dark: androidx.compose.material3.ColorScheme,
    val lightAccents: Pair<Color, Color>,
    val darkAccents: Pair<Color, Color>
)

// ========== 暖珀 — 温暖琥珀深色主调 ==========
private val WarmAmber = PaletteSpec(
    light = lightColorScheme(
        primary = Color(0xFF8B5E2F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE8D0),
        onPrimaryContainer = Color(0xFF2D1600),
        secondary = Color(0xFF7D6B55),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFEEBD6),
        onSecondaryContainer = Color(0xFF2A1700),
        tertiary = Color(0xFF5E6E4A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE2F6C7),
        onTertiaryContainer = Color(0xFF172600),
        background = Color(0xFFFFF8F2),
        onBackground = Color(0xFF1F1B15),
        surface = Color(0xFFFFFBF6),
        onSurface = Color(0xFF1F1B15),
        surfaceVariant = Color(0xFFF3EBE2),
        onSurfaceVariant = Color(0xFF4E4640),
        outline = Color(0xFF7F766D),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF)
    ),
    dark = darkColorScheme(
        primary = Color(0xFFE2A654),
        onPrimary = Color(0xFF3D2000),
        primaryContainer = Color(0xFF5C3A14),
        onPrimaryContainer = Color(0xFFFFE4C0),
        secondary = Color(0xFFC9A87C),
        onSecondary = Color(0xFF3B2400),
        secondaryContainer = Color(0xFF543C1A),
        onSecondaryContainer = Color(0xFFFFEBD0),
        tertiary = Color(0xFFC6DAAD),
        onTertiary = Color(0xFF204200),
        tertiaryContainer = Color(0xFF365A18),
        onTertiaryContainer = Color(0xFFE2F6C7),
        background = Color(0xFF16130E),
        onBackground = Color(0xFFEFE6DB),
        surface = Color(0xFF201D18),
        onSurface = Color(0xFFEFE6DB),
        surfaceVariant = Color(0xFF36322B),
        onSurfaceVariant = Color(0xFFCBC1B5),
        outline = Color(0xFF696258),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005)
    ),
    lightAccents = Color(0xFF8B5E2F) to Color(0xFF7D6B55),
    darkAccents = Color(0xFFE2A654) to Color(0xFFC9A87C)
)

// ========== 薄雾 — 灰蓝静谧调 ==========
private val MistBlue = PaletteSpec(
    light = lightColorScheme(
        primary = Color(0xFF4A6572),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD2EBF9),
        onPrimaryContainer = Color(0xFF001E2D),
        secondary = Color(0xFF5C6E7A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE0F0FA),
        onSecondaryContainer = Color(0xFF122230),
        tertiary = Color(0xFF706B86),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEAE3FF),
        onTertiaryContainer = Color(0xFF1E1A34),
        background = Color(0xFFF7F9FB),
        onBackground = Color(0xFF191C1F),
        surface = Color(0xFFFDFDFE),
        onSurface = Color(0xFF191C1F),
        surfaceVariant = Color(0xFFE1E4E9),
        onSurfaceVariant = Color(0xFF43474B),
        outline = Color(0xFF73777C),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF)
    ),
    dark = darkColorScheme(
        primary = Color(0xFFA4C8DA),
        onPrimary = Color(0xFF003546),
        primaryContainer = Color(0xFF324F5C),
        onPrimaryContainer = Color(0xFFD2EBF9),
        secondary = Color(0xFFBACAD6),
        onSecondary = Color(0xFF25323D),
        secondaryContainer = Color(0xFF3C4A54),
        onSecondaryContainer = Color(0xFFD8E8F4),
        tertiary = Color(0xFFD2CEE5),
        onTertiary = Color(0xFF2F2B44),
        tertiaryContainer = Color(0xFF45425B),
        onTertiaryContainer = Color(0xFFE9E3FE),
        background = Color(0xFF111416),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF1A1D20),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF2F3237),
        onSurfaceVariant = Color(0xFFC3C7CA),
        outline = Color(0xFF6B6F73),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005)
    ),
    lightAccents = Color(0xFF4A6572) to Color(0xFF5C6E7A),
    darkAccents = Color(0xFFA4C8DA) to Color(0xFFBACAD6)
)

// ========== 夜樱 — 深紫暖调 ==========
private val NightSakura = PaletteSpec(
    light = lightColorScheme(
        primary = Color(0xFF7D5268),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9E8),
        onPrimaryContainer = Color(0xFF310F24),
        secondary = Color(0xFF6E5862),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF8DAE4),
        onSecondaryContainer = Color(0xFF281620),
        tertiary = Color(0xFF795833),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDCBD),
        onTertiaryContainer = Color(0xFF2C1600),
        background = Color(0xFFFFF8F8),
        onBackground = Color(0xFF201A1C),
        surface = Color(0xFFFFFBF9),
        onSurface = Color(0xFF201A1C),
        surfaceVariant = Color(0xFFF2DFE6),
        onSurfaceVariant = Color(0xFF51444A),
        outline = Color(0xFF83737A),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF)
    ),
    dark = darkColorScheme(
        primary = Color(0xFFE2B3C6),
        onPrimary = Color(0xFF451E32),
        primaryContainer = Color(0xFF5F3A4D),
        onPrimaryContainer = Color(0xFFFFD9E8),
        secondary = Color(0xFFD4BCC5),
        onSecondary = Color(0xFF3A2831),
        secondaryContainer = Color(0xFF51414A),
        onSecondaryContainer = Color(0xFFF2D8E1),
        tertiary = Color(0xFFEEBF8E),
        onTertiary = Color(0xFF452C10),
        tertiaryContainer = Color(0xFF5E4224),
        onTertiaryContainer = Color(0xFFFFDCBD),
        background = Color(0xFF171214),
        onBackground = Color(0xFFECDFE3),
        surface = Color(0xFF201A1D),
        onSurface = Color(0xFFECDFE3),
        surfaceVariant = Color(0xFF383135),
        onSurfaceVariant = Color(0xFFCDC0C5),
        outline = Color(0xFF6D6368),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005)
    ),
    lightAccents = Color(0xFF7D5268) to Color(0xFF6E5862),
    darkAccents = Color(0xFFE2B3C6) to Color(0xFFD4BCC5)
)

internal fun paletteFor(scheme: DomainColorScheme): PaletteSpec = when (scheme) {
    DomainColorScheme.DAWN -> WarmAmber
    DomainColorScheme.FOREST -> MistBlue
    DomainColorScheme.MIDNIGHT -> NightSakura
}
