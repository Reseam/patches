// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("ThemePatch")

package app.reseam.youtube.theme

import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.AppearanceSettings

private enum class ThemeColor(val resourceName: String, vararg val originals: Int) {
    DARK(
        "reseam_theme_dark_background",
        0xFF282828.toInt(),
        0xFF212121.toInt(),
        0xFF181818.toInt(),
        0xFF0F0F0F.toInt(),
        0xFA212121.toInt(),
    ),
    LIGHT("reseam_theme_light_background", 0xFFFFFFFF.toInt(), 0xFFF9F9F9.toInt(), 0xFAFFFFFF.toInt());

    @Volatile private var resourceId = 0

    fun resolve(fallback: Int): Int =
        try {
            val context = YouTubeContext.applicationContext
            val id =
                resourceId.takeIf { it != 0 }
                    ?: context.identifier(resourceName, "color").also { resourceId = it }
            if (id == 0) fallback else context.resources.getColor(id)
        } catch (_: Throwable) {
            fallback
        }
}

@Export
fun getValue(originalValue: Int): Int =
    ThemeColor.entries.firstOrNull { originalValue in it.originals }?.resolve(originalValue)
        ?: originalValue

@Export
fun gradientLoadingScreenEnabled(original: Boolean): Boolean =
    AppearanceSettings.gradientLoadingScreen.value || original

@Export
fun showSplashScreen(original: Boolean): Boolean =
    AppearanceSettings.splashScreenAnimationStyle.value != "0" && original

// State 5 falls back to creating the splash when the startup predicate is false.
@Export
fun splashStartupState(original: Int): Int =
    if (original == 5 && AppearanceSettings.splashScreenAnimationStyle.value == "0") 4 else original

@Export
fun getLoadingScreenType(original: Int): Int {
    val replacement =
        AppearanceSettings.splashScreenAnimationStyle.value.takeUnless { it == "0" }?.toIntOrNull()
            ?: return original
    if (replacement != original) Logger.debug { "Loading screen type: $original -> $replacement" }
    return replacement
}
