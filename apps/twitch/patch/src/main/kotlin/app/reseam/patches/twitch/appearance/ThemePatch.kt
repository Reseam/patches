// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.appearance

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.bundle.moduleExports
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.resourceId
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.twitch.bindings.Theme
import app.reseam.twitch.theme.bindings.ComposeColors
import app.reseam.twitch.theme.bindings.ThemeModule
import app.reseam.twitch.theme.bindings.WebTheme

private const val DARK_DEFAULT = "#FF0E0E10"
private const val LIGHT_DEFAULT = "#FFF7F7F8"
private const val DARK_BACKGROUND = "reseam_twitch_theme_dark_background"
private const val LIGHT_BACKGROUND = "reseam_twitch_theme_light_background"
private const val GREY = "hinted_grey_"
private const val WEB_ASSETS = "assets/public/assets/"
private const val WEB_VIEW = "Landroid/webkit/WebView;"
private const val SET = "Ljava/util/Set;"

// System dynamic colors exist from Android 12.
private const val DYNAMIC_COLORS = "v31"

// Material You's surface and container colors exist from Android 14.
private const val SURFACE_COLORS = "v34"

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
        // React Native screens write Twitch's purples and greys as hex, so they find each step by its value in the APK.
        val (purples, greys) = if (options[materialYou]) {
            resources.addColor(DARK_BACKGROUND, "@android:color/system_neutral1_900", DYNAMIC_COLORS)
            resources.addColor(LIGHT_BACKGROUND, "@android:color/system_neutral1_50", DYNAMIC_COLORS)
            // On Android 14 the cards get Material You's container colors, so the background takes the surface color
            // those containers are built around.
            resources.addColor(DARK_BACKGROUND, "@android:color/system_surface_dark", SURFACE_COLORS)
            resources.addColor(LIGHT_BACKGROUND, "@android:color/system_surface_light", SURFACE_COLORS)
            val purpleSteps = purpleTones.map { (purple, tone) -> recolorStep(purple, "@android:color/system_accent1_$tone", DYNAMIC_COLORS) }
            // Twitch paints the areas behind the system bars and some lists with the ends of its grey scale, which
            // are also the backgrounds it ships. Text uses them too, so a custom background color leaves them alone.
            val greySteps = listOf(
                recolorStep("${GREY}1", "@color/$DARK_BACKGROUND", DYNAMIC_COLORS),
                recolorStep("${GREY}15", "@color/$LIGHT_BACKGROUND", DYNAMIC_COLORS),
            ) + surfaceGreys.map { (step, color) -> recolorStep("$GREY$step", "@android:color/$color", SURFACE_COLORS) }
            purpleSteps.joinToString(",") to greySteps.joinToString(",")
        } else {
            "" to ""
        }
        require(resources.exists("color", "background_body")) { "color/background_body is missing" }
        resources.addColor("background_body", "@color/$LIGHT_BACKGROUND")
        resources.addColor("background_body", "@color/$DARK_BACKGROUND", "night")

        // React Native screens read the same colors back at startup, through a Reseam module.
        systemPrefsModules.after { returnValue(ThemeModule.withThemeModule(result, param(0))) }
        themeTokens.after {
            Theme.applyTheme(moduleExports, string(purples), string(greys), string(DARK_BACKGROUND), string(LIGHT_BACKGROUND))
        }

        // Twitch's web pages read the same colors from a style that their web view clients add.
        val pageRequests = localPageRequests.all
        require(pageRequests.isNotEmpty()) { "No web view client serves Twitch's local web pages" }
        pageRequests.forEach { pageRequest ->
            pageRequest.after { returnValue(WebTheme.withTheme(result, param(0), string(DARK_BACKGROUND), string(LIGHT_BACKGROUND))) }
        }
        // WebView loads some pages, such as the notification settings, from twitch.tv, because the clients pass those
        // requests through. Each bridge also adds a script that runs at the start of every page it opens, through one
        // shared helper.
        val bridgeScriptAdder = bridgeScriptSetups.all
            .ifEmpty { error("No Capacitor bridge adds a document start script") }
            .map { it.point { invokeStatic { params(WEB_VIEW, Type.String, SET) } }.callee() }
            .distinctBy { it.descriptor }
            .singleOrNull() ?: error("Twitch's bridges no longer add their page script through one helper")
        bridgeScriptAdder.before {
            param(1).assign(WebTheme.withThemeScript(param(1), param(0), string(DARK_BACKGROUND), string(LIGHT_BACKGROUND)))
        }

        // Only Material You changes the purple and grey steps. React Native components and web stylesheets also write
        // the purple steps themselves, and Compose screens such as Settings write the grey steps.
        if (options[materialYou]) {
            normalizeColor.after { returnValue(Theme.recolorNormalized(result, string(purples))) }
            composeGreys.forEach { (step, composeGrey) ->
                val grey = resourceId("color", "$GREY$step").toInt()
                composeGrey.after { capture("greyStep$step").assign(ComposeColors.appColor(capture("greyStep$step"), int(grey))) }
            }
        }
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

// Cards and borders use the grey steps between the background and the text. Each one gets the Material You color
// that is about as far from the surface color as the step is from Twitch's background. Step 6 waits for Android 14
// with the others, so it stays lighter than step 5.
private val surfaceGreys = mapOf(
    2 to "system_surface_container_dark",
    3 to "system_surface_container_high_dark",
    4 to "system_surface_container_highest_dark",
    5 to "system_surface_bright_dark",
    6 to "system_neutral1_700",
    12 to "system_surface_container_highest_light",
    13 to "system_surface_container_high_light",
    14 to "system_surface_container_light",
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

// Twitch's Capacitor bridges hand their bridge script to AndroidX WebKit's addDocumentStartJavaScript. R8 renames
// both bridge classes, so the target finds every bridge and the patch checks that they share the helper it hooks.
private val bridgeScriptSetups = methods("bridgeScriptSetups") {
    strings("DOCUMENT_START_SCRIPT")
    calls { params(WEB_VIEW, Type.String, SET) }
}

// React Native's normalizeColor turns each color of a style into a number, 0xrrggbbaa.
private val normalizeColor = function {
    name("normalizeColor")
    strings("normalizeColorObject")
    paramCount(1)
}

// The grey steps that Twitch's Compose tokens build from their own ARGB value and keep. The class also builds steps 5
// and 11 without keeping them.
private val composeGreyArgbs = mapOf(
    1 to 0xFF0E0E10,
    2 to 0xFF18181B,
    3 to 0xFF1F1F23,
    4 to 0xFF26262C,
    6 to 0xFF3B3B44,
    12 to 0xFFDEDEE3,
    13 to 0xFFE6E6EA,
    15 to 0xFFF7F7F8,
)

// Twitch's Compose color tokens build every color once, in the token class's initializer.
private val composeColorTokens = method("composeColorTokens") {
    name("<clinit>")
    literals(composeGreyArgbs.getValue(1), composeGreyArgbs.getValue(15))
}

private val composeGreyArgbPoints = composeGreyArgbs.mapValues { (step, argb) ->
    composeColorTokens.point("greyStep${step}Argb") { literal(argb) }
}

// The class builds step 14 from a register it loaded for an earlier color, so its color is the last result before
// the ARGB value of step 15.
private val composeGreys = composeGreyArgbPoints.map { (step, argbPoint) -> step to argbPoint.next { resultOf(Type.Long) } }
    .plus(14 to composeGreyArgbPoints.getValue(15).previous { resultOf(Type.Long) })
    .map { (step, result) -> step to result.captureAs("greyStep$step", Type.Long) }

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

/**
 * Points the color [name] at [color] where the resource qualifier [qualifier] applies, and returns the value it ships
 * with and its name as `#rrggbb=name`.
 */
private fun PatchRuntime.recolorStep(name: String, color: String, qualifier: String): String {
    val shipped = requireNotNull(resources.getRaw("color", name)) { "color/$name is missing" }
    resources.addColor(name, color, qualifier)
    return "#%06x=%s".format(shipped and 0xFFFFFFL, name)
}
