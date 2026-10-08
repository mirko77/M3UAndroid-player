package com.m3u.smartphone.ui.business.setting.fragments

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.m3u.core.foundation.architecture.preferences.ClipMode
import com.m3u.core.foundation.architecture.preferences.PreferencesKeys
import com.m3u.core.foundation.architecture.preferences.ThemePreference
import com.m3u.core.foundation.architecture.preferences.ThemePreset
import com.m3u.core.foundation.architecture.preferences.ThemeStyle
import com.m3u.core.foundation.architecture.preferences.mutablePreferenceOf
import com.m3u.core.foundation.architecture.preferences.themePreferencesOf
import com.m3u.core.foundation.util.basic.title
import com.m3u.data.database.model.ColorScheme
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.business.setting.components.SwitchSharedPreference
import com.m3u.smartphone.ui.material.components.Preference
import com.m3u.smartphone.ui.material.components.TextPreference
import com.m3u.smartphone.ui.material.ktx.plus
import com.m3u.smartphone.ui.material.model.LocalSpacing

@Composable
internal fun AppearanceFragment(
    colorSchemes: List<ColorScheme>,
    onSelectTheme: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val spacing = LocalSpacing.current

    val themePreferences by themePreferencesOf()
    val dynamicColorsPreference =
        mutablePreferenceOf(PreferencesKeys.USE_DYNAMIC_COLORS)
    val followSystemThemePreference =
        mutablePreferenceOf(PreferencesKeys.FOLLOW_SYSTEM_THEME)
    val selectedTheme = themePreferences.selection
    val isDarkMode = selectedTheme.isDark
    val useDynamicColors = themePreferences.useDynamicColors
    val themeStyle = selectedTheme.style
    val themePresetId = selectedTheme.presetId
    val colorArgb = selectedTheme.argb
    var clipMode by mutablePreferenceOf(PreferencesKeys.CLIP_MODE)
    var compactDimension by mutablePreferenceOf(PreferencesKeys.COMPACT_DIMENSION)
    val followSystemTheme = themePreferences.followSystemTheme

    val useDynamicColorsAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicColorsActive = areDynamicColorsActive(
        requested = useDynamicColors,
        supported = useDynamicColorsAvailable,
    )
    val systemUsesDarkTheme = isSystemInDarkTheme()
    val themeOptions = buildList {
        selectThemeSchemeVariants(
            schemes = colorSchemes,
            followSystemTheme = followSystemTheme,
            systemUsesDarkTheme = systemUsesDarkTheme,
        ).forEach { stored ->
            add(
                AppearanceThemeOption(
                    presetId = ThemePreset.MATERIAL,
                    name = stored.name,
                    argb = stored.argb,
                    isDark = if (followSystemTheme) {
                        systemUsesDarkTheme
                    } else {
                        stored.isDark
                    },
                    style = ThemeStyle.MATERIAL,
                )
            )
        }
        add(
            AppearanceThemeOption.warmEditorial(
                isDark = followSystemTheme && systemUsesDarkTheme,
            )
        )
        if (!followSystemTheme) {
            add(AppearanceThemeOption.warmEditorial(isDark = true))
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        contentPadding = contentPadding + PaddingValues(spacing.medium),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                themeOptions.forEach { option ->
                    val selected =
                        !dynamicColorsActive &&
                            themePresetId == option.presetId &&
                            colorArgb == option.argb &&
                            themeStyle == option.style &&
                            (followSystemTheme || isDarkMode == option.isDark)
                    Preference(
                        title = option.localizedName(),
                        selected = selected,
                        onClick = { onSelectTheme(option.toPreference()) },
                    )
                }
            }
        }
        item {
            TextPreference(
                title = stringResource(string.feat_setting_clip_mode).title(),
                icon = Icons.Rounded.FitScreen,
                trailing = when (clipMode) {
                    ClipMode.ADAPTIVE -> stringResource(string.feat_setting_clip_mode_adaptive)
                    ClipMode.CLIP -> stringResource(string.feat_setting_clip_mode_clip)
                    ClipMode.STRETCHED -> stringResource(string.feat_setting_clip_mode_stretched)
                    else -> ""
                }.title(),
                onClick = {
                    clipMode = when (clipMode) {
                        ClipMode.ADAPTIVE -> ClipMode.CLIP
                        ClipMode.CLIP -> ClipMode.STRETCHED
                        ClipMode.STRETCHED -> ClipMode.ADAPTIVE
                        else -> ClipMode.ADAPTIVE
                    }
                }
            )
        }
        item {
            SwitchSharedPreference(
                title = string.feat_setting_compact_dimension,
                icon = Icons.Rounded.FormatSize,
                checked = compactDimension,
                onChanged = { compactDimension = !compactDimension }
            )
        }
        item {
            SwitchSharedPreference(
                title = string.feat_setting_follow_system_theme,
                icon = Icons.Rounded.DarkMode,
                checked = followSystemTheme,
                onChanged = {
                    followSystemThemePreference.value = !followSystemTheme
                },
            )
        }
        item {
            SwitchSharedPreference(
                title = string.feat_setting_use_dynamic_colors,
                content = string.feat_setting_use_dynamic_colors_unavailable.takeUnless { useDynamicColorsAvailable },
                icon = Icons.Rounded.ColorLens,
                checked = useDynamicColors && useDynamicColorsAvailable,
                onChanged = {
                    dynamicColorsPreference.value = !useDynamicColors
                },
                enabled = useDynamicColorsAvailable
            )
        }
    }
}

private data class AppearanceThemeOption(
    val presetId: String,
    val name: String,
    val argb: Int,
    val isDark: Boolean,
    val style: Int,
) {
    val key: String
        get() = "$presetId:$argb:$isDark:$name"

    fun toPreference(): ThemePreference = ThemePreference(
        presetId = presetId,
        argb = argb,
        isDark = isDark,
        style = style,
    )

    companion object {
        fun warmEditorial(isDark: Boolean) = AppearanceThemeOption(
            presetId = ThemePreset.WARM_EDITORIAL,
            name = "",
            argb = ThemePreset.WARM_EDITORIAL_SEED,
            isDark = isDark,
            style = ThemeStyle.WARM_EDITORIAL,
        )
    }
}

internal fun selectThemeSchemeVariants(
    schemes: List<ColorScheme>,
    followSystemTheme: Boolean,
    systemUsesDarkTheme: Boolean,
): List<ColorScheme> {
    if (!followSystemTheme) return schemes

    val addedSeeds = mutableSetOf<Int>()
    return buildList {
        schemes.forEach { scheme ->
            if (addedSeeds.add(scheme.argb)) {
                add(
                    schemes.firstOrNull { candidate ->
                        candidate.argb == scheme.argb &&
                            candidate.isDark == systemUsesDarkTheme
                    } ?: scheme
                )
            }
        }
    }
}

internal fun areDynamicColorsActive(
    requested: Boolean,
    supported: Boolean,
): Boolean = requested && supported

@Composable
private fun AppearanceThemeOption.localizedName(): String = when {
    presetId == ThemePreset.WARM_EDITORIAL && isDark ->
        stringResource(string.feat_setting_theme_ink)

    presetId == ThemePreset.WARM_EDITORIAL ->
        stringResource(string.feat_setting_theme_parchment)

    else -> name.title()
}
