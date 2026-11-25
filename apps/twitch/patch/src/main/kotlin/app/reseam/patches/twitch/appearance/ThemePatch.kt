// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.appearance

import app.reseam.patch.PatchRuntime
import app.reseam.patch.after
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.twitch.bindings.Theme
import app.reseam.twitch.theme.bindings.ThemeModule
import app.reseam.twitch.theme.bindings.WebTheme

private const val DARK_DEFAULT = "#FF0E0E10"
private const val LIGHT_DEFAULT = "#FFF7F7F8"
private const val DARK_BACKGROUND = "reseam_twitch_theme_dark_background"
private const val LIGHT_BACKGROUND = "reseam_twitch_theme_light_background"
private const val WEB_ASSETS = "assets/public/assets/"

// System dynamic colors exist from Android 12.
private const val DYNAMIC_COLORS = "v31"

val theme = patch(TwitchPatchStrings.theme) {
    description(TwitchPatchStrings.themeDescription)
    compatibleWith(TWITCH)
    dependsOn(twitchModules)

    val darkThemeBackgroundColor = stringOption(
        "darkThemeBackgroundColor",
        title = TwitchPatchStrings.themeDarkThemeBackgroundColor,
        description = TwitchPatchStrings.themeDarkThemeBackgroundColorDescription,
        default = DARK_DEFAULT,
    )
    val lightThemeBackgroundColor = stringOption(
        "lightThemeBackgroundColor",
        title = TwitchPatchStrings.themeLightThemeBackgroundColor,
        description = TwitchPatchStrings.themeLightThemeBackgroundColorDescription,
        default = LIGHT_DEFAULT,
    )
    val materialYou = boolOption(
        "materialYou",
        title = TwitchPatchStrings.themeMaterialYou,
        description = TwitchPatchStrings.themeMaterialYouDescription,
        default = false,
    )

    execute {
        resources.addColor(DARK_BACKGROUND, options[darkThemeBackgroundColor])
        resources.addColor(LIGHT_BACKGROUND, options[lightThemeBackgroundColor])
        if (options[materialYou]) {
            resources.addColor(DARK_BACKGROUND, "@android:color/system_neutral1_900", DYNAMIC_COLORS)
            resources.addColor(LIGHT_BACKGROUND, "@android:color/system_neutral1_50", DYNAMIC_COLORS)
            purpleTones.forEach { (purple, tone) ->
                require(resources.exists("color", purple)) { "color/$purple is missing" }
                resources.addColor(purple, "@android:color/system_accent1_$tone", DYNAMIC_COLORS)
            }
        }
        require(resources.exists("color", "background_body")) { "color/background_body is missing" }
        resources.addColor("background_body", "@color/$LIGHT_BACKGROUND")
        resources.addColor("background_body", "@color/$DARK_BACKGROUND", "night")

        // React Native screens read the same colors back at startup, through a Reseam module.
        systemPrefsModules.after { returnValue(ThemeModule.withThemeModule(result, param(0))) }
        // A Metro factory's sixth parameter is the module's exports.
        themeTokens.after { Theme.applyTheme(param(5), string(DARK_BACKGROUND), string(LIGHT_BACKGROUND)) }

        // Twitch's web pages read the same colors from a style that their web view clients add.
        val pageRequests = localPageRequests.all
        require(pageRequests.isNotEmpty()) { "No web view client serves Twitch's local web pages" }
        pageRequests.forEach { pageRequest ->
            pageRequest.after { returnValue(WebTheme.withTheme(result, param(0), string(DARK_BACKGROUND), string(LIGHT_BACKGROUND))) }
        }

        // Only Material You changes the purple steps, which some React Native components and web stylesheets write themselves.
        if (options[materialYou]) normalizeColor.after { returnValue(Theme.recolorNormalized(result)) }
        val stylesheetPurples = if (options[materialYou]) pointStylesheetPurplesAtVariables() else 0
        log.info("Themed the web pages of ${pageRequests.size} web view clients, and $stylesheetPurples purple colors in their stylesheets")
    }
}

// Native semantic colors point at these steps of Twitch's purple scale. Each step gets the
// system accent tone closest to its lightness.
private val purpleTones = mapOf(
    "twitch_purple" to 500,
    "twitch_purple_2" to 1000,
    "twitch_purple_3" to 1000,
    "twitch_purple_4" to 900,
    "twitch_purple_5" to 800,
    "twitch_purple_6" to 800,
    "twitch_purple_7" to 700,
    "twitch_purple_8" to 600,
    "twitch_purple_9" to 500,
    "twitch_purple_10" to 400,
    "twitch_purple_11" to 300,
    "twitch_purple_12" to 200,
    "twitch_purple_13" to 100,
    "twitch_purple_14" to 100,
    "twitch_purple_15" to 50,
)

// Twitch's React Native libraries keep their names. R8 renames React Native's package types,
// so the Reseam module joins the module list of a package that Twitch already registers.
private val systemPrefsModules = klass("com.twitchrn.systemprefs.TwitchSystemPrefsPackage")
    .method("createNativeModules")

// @twitch-rn/tokens builds the dark, light and overlay themes of the React Native screens once,
// when the app first loads it.
private val themeTokens = function {
    name("")
    strings("darkTheme", "lightTheme", "overlayTheme", "twitchPurple")
    paramCount(7)
}

// Both web view clients of Twitch's portals serve /wv/ paths from local portals-<path>.html or portals.html files.
private val localPageRequests = methods("localPageRequests") {
    name("shouldInterceptRequest")
    strings("/wv/", "portals-")
}

// React Native's normalizeColor turns each color of a style into a number, 0xrrggbbaa.
private val normalizeColor = function {
    name("normalizeColor")
    strings("normalizeColorObject")
    paramCount(1)
}

// Twitch's purple scale as its web stylesheets write it, by step. Step 1 has no app color.
private val stylesheetPurpleSteps = mapOf(
    "0d031c" to 2,
    "15052e" to 3,
    "23094e" to 4,
    "330c6e" to 5,
    "451093" to 6,
    "5c16c5" to 7,
    "772ce8" to 8,
    "9147ff" to 9,
    "a970ff" to 10,
    "bf94ff" to 11,
    "d1b3ff" to 12,
    "e3d1ff" to 13,
    "ede0ff" to 14,
    "f3ebff" to 15,
)

// The lookahead skips translucent colors such as #9147ff40, which start with a step.
private val stylesheetPurple = Regex(
    "#(${stylesheetPurpleSteps.keys.joinToString("|")})(?![0-9a-f])",
    RegexOption.IGNORE_CASE,
)

/** Points the purple steps that the web stylesheets hard-code at the pages' purple variables, and returns how many it changed. */
private fun PatchRuntime.pointStylesheetPurplesAtVariables(): Int {
    var changed = 0
    for (path in files.list()) {
        if (!path.startsWith(WEB_ASSETS) || !path.endsWith(".css")) continue
        val stylesheet = files.read(path)?.decodeToString() ?: error("Cannot read $path")
        var purples = 0
        // The hard-coded color stays as the fallback.
        val themed = stylesheetPurple.replace(stylesheet) { purple ->
            purples++
            "var(--color-twitch-purple-${stylesheetPurpleSteps.getValue(purple.groupValues[1].lowercase())},${purple.value})"
        }
        if (purples == 0) continue
        files.write(path, themed.encodeToByteArray())
        changed += purples
    }
    require(changed > 0) { "The web stylesheets no longer hard-code Twitch purple" }
    return changed
}
