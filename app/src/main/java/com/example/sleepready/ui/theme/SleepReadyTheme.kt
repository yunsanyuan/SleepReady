package com.example.sleepready.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.example.sleepready.domain.model.ColorScheme
import com.example.sleepready.domain.model.ThemeMode

/**
 * 应用主题。除 [ThemeMode]（浅 / 深 / 跟随系统）外，新增 [ColorScheme] 选择
 * （晨曦 / 林间 / 墨夜）。两者组合后从 [paletteFor] 取出对应的
 * Material3 [androidx.compose.material3.ColorScheme]。
 *
 * 同时把当前调色板的两个强调色写入 [setLegacyAccents]，保持下层的呼吸光晕、
 * 倒计时数字、ActionPlanList 等仍旧引用 `WarmOrange` / `MoonlightYellow` 时
 * 也能跟随主题切换。
 */
@Composable
fun SleepReadyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorScheme: ColorScheme = ColorScheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = paletteFor(colorScheme)
    val active = if (useDark) palette.dark else palette.light
    val accents = if (useDark) palette.darkAccents else palette.lightAccents

    SideEffect {
        setLegacyAccents(primary = accents.first, secondary = accents.second)
    }

    MaterialTheme(
        colorScheme = active,
        typography = SleepReadyTypography,
        shapes = SleepReadyShapes,
        content = content
    )
}
