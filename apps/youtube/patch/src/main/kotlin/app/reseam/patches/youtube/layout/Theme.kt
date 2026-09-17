// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.MAIN_ACTIVITY
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.lithoColorHook
import app.reseam.patches.youtube.internal.registerLithoColorHook

private const val DARK_DEFAULT = "#FF000000"
private const val LIGHT_DEFAULT = "#FFFFFFFF"
private const val SPLASH_COLOR = "reseam_splash_background_color"

val theme = patch("Theme") {
    description("Changes YouTube's light and dark background colors and loading screen.")
    compatibleWith(YOUTUBE)
    dependsOn(lithoColorHook)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Appearance, "General", YouTubeSettings.gradientLoadingScreen, YouTubeSettings.splashScreenAnimationStyle),
    )

    val darkThemeBackgroundColor = stringOption(
        "darkThemeBackgroundColor",
        title = "Dark theme background color",
        description = "The background color used by YouTube's dark theme.",
        default = DARK_DEFAULT,
    )
    val lightThemeBackgroundColor = stringOption(
        "lightThemeBackgroundColor",
        title = "Light theme background color",
        description = "The background color used by YouTube's light theme.",
        default = LIGHT_DEFAULT,
    )

    execute {
        val dark = options[darkThemeBackgroundColor]
        val light = options[lightThemeBackgroundColor]

        listOf(
            "yt_black0", "yt_black1", "yt_black1_opacity95", "yt_black1_opacity98",
            "yt_black2", "yt_black3", "yt_black4", "yt_status_bar_background_dark",
            "material_grey_850",
        ).forEach { resources.addColor(it, dark) }
        listOf("yt_white1", "yt_white1_opacity95", "yt_white1_opacity98", "yt_white2", "yt_white3", "yt_white4")
            .forEach { resources.addColor(it, light) }
        resources.addColor(SPLASH_COLOR, light)

        // Both launchscreen configurations are file-backed even though the release APK has no
        // res/drawable path with the resource name.
        resources.paths("drawable", "quantum_launchscreen_youtube").forEach { path ->
            files.editXml(path) {
                root.children
                    .asSequence()
                    .firstOrNull { it.tag == "item" && it["android:drawable"] != null }
                    ?.set("android:drawable", "@color/$SPLASH_COLOR")
                    ?: error("drawable/$SPLASH_COLOR launchscreen item is missing in $path")
            }
        }

        resources.style("Theme.YouTube.Home", parent = "@style/Base.V27.Theme.YouTube.Home") {
            this["android:navigationBarColor"] = "@color/$SPLASH_COLOR"
            this["android:windowBackground"] = "@color/$SPLASH_COLOR"
            this["android:colorBackground"] = "@color/$SPLASH_COLOR"
            this["colorPrimaryDark"] = "@color/$SPLASH_COLOR"
            this["android:windowLightStatusBar"] = "false"
        }

        registerLithoColorHook(ThemePatch.getValue)
        useGradientLoadingScreen
            .point {
                literal(GRADIENT_LOADING_SCREEN)
                then(within = 6) { resultOf(Type.Boolean) }
            }
            .captureAs("gradient", Type.Boolean)
            .after { capture("gradient").assign(call(ThemePatch.gradientLoadingScreenEnabled, capture("gradient"))) }

        splashScreenStyle
            .point {
                literal(SPLASH_STYLE_RESOURCE)
                then(within = 6) { resultOf(Type.Int) }
            }
            .captureAs("style", Type.Int)
            .after { capture("style").assign(call(ThemePatch.getLoadingScreenType, capture("style"))) }

        splashStartupCheck.next { resultOf(Type.Boolean) }
            .captureAs("showSplash", Type.Boolean)
            .after { capture("showSplash").assign(call(ThemePatch.showSplashScreen, capture("showSplash"))) }

        splashStartupCheck.writer(0, "splashStartupState")
            .captureAs("startupState", Type.Int)
            .after { capture("startupState").assign(call(ThemePatch.splashStartupState, capture("startupState"))) }

    }
}

private const val GRADIENT_LOADING_SCREEN = 45412406L
private const val SPLASH_STYLE_RESOURCE = 1074339245L

private val useGradientLoadingScreen = method("useGradientLoadingScreen") {
    literals(GRADIENT_LOADING_SCREEN)
}

private val splashScreenStyle = method("splashScreenStyle") {
    inClass(klass(MAIN_ACTIVITY))
    name("onCreate")
    flags(AccessFlags.PROTECTED or AccessFlags.FINAL)
    params("android.os.Bundle")
    returns(Type.Void)
    literals(SPLASH_STYLE_RESOURCE)
}

// The startup-state predicate gates the splash initialization block. Follow it backward
// from the splash style rather than matching an unrelated earlier boolean in onCreate.
private val splashStartupCheck = splashScreenStyle.point { literal(SPLASH_STYLE_RESOURCE) }
    .previous { invokeStatic { params(Type.Int); returns(Type.Boolean) } }

object ThemePatch : ExtClass("app.reseam.youtube.theme.ThemePatch") {
    val getValue = static("getValue", Type.Int, returns = Type.Int)
    val gradientLoadingScreenEnabled = static("gradientLoadingScreenEnabled", Type.Boolean, returns = Type.Boolean)
    val showSplashScreen = static("showSplashScreen", Type.Boolean, returns = Type.Boolean)
    val splashStartupState = static("splashStartupState", Type.Int, returns = Type.Int)
    val getLoadingScreenType = static("getLoadingScreenType", Type.Int, returns = Type.Int)
}
