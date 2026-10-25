// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.literal
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.MAIN_ACTIVITY
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtubecommon.lithoColorHook
import app.reseam.patches.youtubecommon.registerLithoColorHook
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.theme.bindings.ThemePatch

private const val DARK_DEFAULT = "#FF000000"
private const val LIGHT_DEFAULT = "#FFFFFFFF"
private const val SPLASH_COLOR = "reseam_splash_background_color"
private const val NAVIGATION_BAR_COLOR = "reseam_navigation_bar_color"

val theme = patch("Theme") {
    description("Changes YouTube's light and dark background colors and loading screen.")
    compatibleWith(YOUTUBE)
    dependsOn(lithoColorHook)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Appearance, YouTubeStrings.sectionGeneral, AppearanceSettings.gradientLoadingScreen, AppearanceSettings.splashScreenAnimationStyle),
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
    val materialYou = boolOption(
        "materialYou",
        title = "Material You",
        description = "Uses the system's wallpaper-based background colors on Android 12 and newer, in place of the colors above.",
        default = false,
    )

    execute {
        val materialYouColors = options[materialYou] == true
        mapOf(DARK to options[darkThemeBackgroundColor], LIGHT to options[lightThemeBackgroundColor])
            .forEach { (background, color) ->
                resources.addColor(background.color, color)
                if (materialYouColors) resources.addColor(background.color, "@android:color/${background.systemColor}", DYNAMIC_COLORS)
                // Older releases predate some palette styles, and the oldest predate the palette.
                val paletteStyles = background.paletteStyles.filter { resources.exists("style", it) }
                paletteStyles.forEach { style ->
                    resources.style(style) { PALETTE_BACKGROUNDS.forEach { this[it] = "@color/${background.color}" } }
                }
                if (paletteStyles.isEmpty()) background.legacyColors.forEach { resources.addColor(it, "@color/${background.color}") }
            }

        // File-backed color selectors let Android select the night variant before app startup.
        // A default-only splash color produces a white flash even when the app uses a dark theme.
        listOf("" to LIGHT.color, "night" to DARK.color).forEach { (qualifier, color) ->
            resources.addColor(SPLASH_COLOR, "@color/$color", qualifier)
            val selector = if (qualifier.isEmpty()) SELECTOR_DRAWABLES else "$qualifier-$SELECTOR_DRAWABLES"
            resources.addFile("color", SPLASH_COLOR, "res/color-$selector/$SPLASH_COLOR.xml",
                """<selector xmlns:android="http://schemas.android.com/apk/res/android"><item android:color="@color/$color" /></selector>""".toByteArray(),
                selector)
        }

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

        resources.addColor(NAVIGATION_BAR_COLOR, "@android:color/black")
        resources.addColor(NAVIGATION_BAR_COLOR, "@color/$SPLASH_COLOR", LIGHT_NAVIGATION_BAR)
        resources.style("Theme.YouTube.Home") {
            this["android:navigationBarColor"] = "@color/$NAVIGATION_BAR_COLOR"
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
            .after { capture("gradient").assign(ThemePatch.gradientLoadingScreenEnabled(capture("gradient"))) }

        splashScreenStyle
            .point {
                where { this.literal in SPLASH_STYLE_RESOURCES }
                then(within = 6) { resultOf(Type.Int) }
            }
            .captureAs("style", Type.Int)
            .after { capture("style").assign(ThemePatch.getLoadingScreenType(capture("style"))) }

        splashStartupCheck.next { resultOf(Type.Boolean) }
            .captureAs("showSplash", Type.Boolean)
            .after { capture("showSplash").assign(ThemePatch.showSplashScreen(capture("showSplash"))) }

        splashStartupCheck.writer(0, "splashStartupState")
            .captureAs("startupState", Type.Int)
            .after { capture("startupState").assign(ThemePatch.splashStartupState(capture("startupState"))) }

        // On Android 9 and older a server experiment makes the app read a stale copy of the Dark theme
        // switch. Reading the copy as unset makes the app read the switch again and refresh the copy.
        darkThemeCopyReads.forEach {
            next { resultOf(Type.Int) }.captureAs("darkThemeCopy", Type.Int).after { capture("darkThemeCopy").assign(int(0)) }
        }
    }
}

private class Background(
    val color: String,
    val systemColor: String,
    val paletteStyles: List<String>,
    val legacyColors: List<String>,
)

// Releases without the palette use the legacy colors, whose values the extension also replaces.
private val DARK = Background(
    color = "reseam_theme_dark_background",
    systemColor = "system_neutral1_900",
    paletteStyles = listOf("yt.sys.color.baseline.dark", "yt.sys.color.baseline.dark.cooler"),
    legacyColors = listOf("yt_black0", "yt_black1", "yt_black2", "yt_black3", "yt_black1_opacity98"),
)

private val LIGHT = Background(
    color = "reseam_theme_light_background",
    systemColor = "system_neutral1_50",
    paletteStyles = listOf("yt.sys.color.baseline"),
    legacyColors = listOf("yt_white1", "yt_white2", "yt_white1_opacity98"),
)

// The palette styles feed the theme's surface attributes.
private val PALETTE_BACKGROUNDS = listOf(
    "yt_sys_color_baseline_base_background",
    "yt_sys_color_baseline_raised_background",
    "yt_sys_color_baseline_menu_background",
)

// System dynamic colors exist from Android 12. A plain value, unlike a selector file, also reaches
// views that read the theme attribute's raw color.
private const val DYNAMIC_COLORS = "v31"

// A selector stands in for a drawable from Android 10; earlier versions take the plain color.
private const val SELECTOR_DRAWABLES = "v29"

// Dark navigation bar icons exist from Android 8.1; before that a light bar hides the white icons.
private const val LIGHT_NAVIGATION_BAR = "v27"

private const val GRADIENT_LOADING_SCREEN = 45412406L
// Older releases read the splash style under the second id.
private val SPLASH_STYLE_RESOURCES = listOf(1074339245L, 269032877L)

private val useGradientLoadingScreen = method("useGradientLoadingScreen") {
    literals(GRADIENT_LOADING_SCREEN)
}

private val splashScreenStyle = method("splashScreenStyle") {
    inClass(klass(MAIN_ACTIVITY))
    name("onCreate")
    flags(AccessFlags.PROTECTED or AccessFlags.FINAL)
    params("android.os.Bundle")
    returns(Type.Void)
}

// The flag holding the app's copy of the Dark theme switch.
private const val DARK_THEME_COPY = 269735L

// The setter that writes the copy takes the new theme; only the reader takes no arguments.
private val darkThemeCopyReads = methods("darkThemeReader") {
    params()
    literals(DARK_THEME_COPY)
}.points("darkThemeCopyRead") {
    invoke { params(Type.Int); returns(Type.Int) }
    argument(1) { literal(DARK_THEME_COPY) }
}

// The startup-state predicate gates the splash initialization block. Follow it backward
// from the splash style rather than matching an unrelated earlier boolean in onCreate.
private val splashStartupCheck = splashScreenStyle.point { where { this.literal in SPLASH_STYLE_RESOURCES } }
    .previous { invokeStatic { params(Type.Int); returns(Type.Boolean) } }
