// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.theme

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patches.bundle.LauncherAlias
import app.reseam.patches.bundle.addLauncherAliases
import app.reseam.patches.x.core.COMPOSER
import app.reseam.patches.x.core.MAIN_ACTIVITY
import app.reseam.patches.x.core.mainActivityOnCreate
import app.reseam.patches.x.core.composerAtEntry
import app.reseam.patches.x.core.xIcon
import app.reseam.x.theme.bindings.XTheme

// The Twitter bird on a 24-unit grid, from Simple Icons 9.21.0 (CC0-1.0).
private const val BIRD =
    "M21.543 7.104c.015.211.015.423.015.636 0 6.507-4.954 14.01-14.01 14.01v-.003A13.94 13.94 0 0 1 0 19.539a9.88 " +
        "9.88 0 0 0 7.287-2.041 4.93 4.93 0 0 1-4.6-3.42 4.916 4.916 0 0 0 2.223-.084A4.926 4.926 0 0 1 .96 9.167v-.062a4.887 " +
        "4.887 0 0 0 2.235.616A4.928 4.928 0 0 1 1.67 3.148 13.98 13.98 0 0 0 11.82 8.292a4.929 4.929 0 0 1 8.39-4.49 9.868 " +
        "9.868 0 0 0 3.128-1.196 4.941 4.941 0 0 1-2.165 2.724A9.828 9.828 0 0 0 24 4.555a10.019 10.019 0 0 1-2.457 2.549z"
private const val BIRD_GRID = 24f
private const val TWITTER_BLUE = "#FF1D9BF0"
private const val TWITTER_NAME = "reseam_twitter_name"
private const val LAUNCHER_BACKGROUND = "reseam_twitter_blue"
private const val LAUNCHER_FOREGROUND = "reseam_twitter_launcher_foreground"
private const val LAUNCHER_ICON = "reseam_twitter_launcher"
private const val SPLASH_ICON = "reseam_twitter_splash"
private const val SPLASH_THEME = "Theme.Reseam.TwitterSplash"
private const val REFRESH_LOGO = "reseam_twitter_refresh_logo"

/**
 * The Twitter branding, switched by a setting: the Twitter name and blue launcher icon through a
 * launcher alias, the bird in place of each X logo, a bird splash screen on Android 13+, and the
 * brand color on the home top-bar bird and the selected tab's underline.
 */
internal fun PatchRuntime.addTwitterBranding() {
    // Each X logo gets a bird twin drawn at the share of the canvas the X takes, in the same fill so
    // the app tints it the same way; code that loads the logo loads the twin while the setting is on.
    val twins = mutableMapOf<UInt, UInt>()
    fun twin(drawable: String, coverage: Float) {
        val id = resources.id("drawable", drawable) ?: error("drawable/$drawable missing")
        twins[id] = addBirdTwin(drawable, coverage)
    }
    twin("ic_vector_twitter", 1f)
    twin("ic_vector_x", 1f)
    twin("ic_logo_x", 0.84f)
    twin("ic_stat_x", 0.9f)
    // Since 12.34 Home's top-bar logo is a Lottie that animates on pull to refresh; the bird keeps its markers.
    resources.id("raw", "x_refresh_logo")?.let { logo ->
        twins[logo] = resources.addFile("raw", REFRESH_LOGO, "res/raw/$REFRESH_LOGO.json", resource("twitter_refresh_logo.json"))
    }
    XTheme.twitterResource.implement {
        twins.forEach { (x, bird) -> whenEqual(param(0), int(x.toInt())) { returnValue(int(bird.toInt())) } }
        returnValue(param(0))
    }
    for (x in twins.keys) {
        methods("loads:$x") { literals(x.toLong()) }.points { literal(x.toLong()) }.forEach {
            captureAs("resource", Type.Int).after { capture("resource").assign(XTheme.branded(capture("resource"))) }
        }
    }

    resources.addString(TWITTER_NAME, "Twitter")
    resources.addColor(LAUNCHER_BACKGROUND, TWITTER_BLUE)
    resources.addFile("drawable", LAUNCHER_FOREGROUND, "res/drawable/$LAUNCHER_FOREGROUND.xml", birdVector(108f, 0.38f, "#FFFFFFFF").toByteArray())
    resources.addFile(
        "mipmap", LAUNCHER_ICON, "res/mipmap-anydpi-v26/$LAUNCHER_ICON.xml",
        ("""<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">""" +
            """<background android:drawable="@color/$LAUNCHER_BACKGROUND"/><foreground android:drawable="@drawable/$LAUNCHER_FOREGROUND"/>""" +
            """<monochrome android:drawable="@drawable/$LAUNCHER_FOREGROUND"/></adaptive-icon>""").toByteArray(),
        "anydpi-v26",
    )
    // Only the launcher filter moves: the deep-link filters stay on the activity alone.
    addLauncherAliases(
        MAIN_ACTIVITY,
        MAIN_ACTIVITY,
        listOf(
            LauncherAlias(".reseam_x", label = "@string/APP_NAME", icon = "@mipmap/ic_launcher", enabled = true),
            LauncherAlias(".reseam_twitter", label = "@string/$TWITTER_NAME", icon = "@mipmap/$LAUNCHER_ICON", enabled = false),
        ),
    ) { filter -> filter.children.any { it.tag == "category" && it["android:name"] == "android.intent.category.LAUNCHER" } }

    resources.addFile("drawable", SPLASH_ICON, "res/drawable/$SPLASH_ICON.xml", birdVector(108f, 0.45f, TWITTER_BLUE).toByteArray())
    val splashTheme = resources.style(SPLASH_THEME, parent = "@style/Theme.X.SplashScreen") {
        this["windowSplashScreenAnimatedIcon"] = "@drawable/$SPLASH_ICON"
    }
    XTheme.twitterSplashTheme.implement { returnValue(int(splashTheme.toInt())) }
    mainActivityOnCreate.after { XTheme.applyLauncherBranding(thisObject) }

    addTwitterColors()
}

/**
 * Twitter's use of its brand color: the home top-bar bird and the selected tab's underline take the
 * theme's primary color, where X draws them in the text color.
 */
private fun PatchRuntime.addTwitterColors() {
    XTheme.brandColor.implement {
        returnValue(call(currentThemeColors, param(0).cast(COMPOSER), int(0)).field(themePrimary))
    }
    resources.id("raw", "x_refresh_logo")?.let { logo ->
        val homeTopBarLogo = method("homeTopBarLogo") { literals(logo.toLong()) }
        val composer = homeTopBarLogo.composerAtEntry()
        homeTopBarLogo.point { field { owner(componentColors.descriptor); type(Type.Long) } }
            .captureAs("tint")
            .after { whenTrue(XTheme.twitter()) { capture("tint").assign(XTheme.brandColor(local(composer))) } }
    }
    // Without a refresh state, or before 12.34, the logo is a plain icon in the content color.
    resources.id("string", "home_logo_scroll_to_top")?.let { label ->
        val homeTopBar = method("homeTopBar") { literals(label.toLong()) }
        val composer = homeTopBar.composerAtEntry()
        val logo = homeTopBar.point { invokeStatic { hasParam(xIcon.descriptor); hasParam(COMPOSER) } }
        val drawIcon = logo.callee("drawIcon")
        val types = drawIcon.parameterTypes
        val tint = types.indexOf(Type.Long)
        val defaults = types.lastIndex
        logo.captureArgumentAs("tint", tint).captureArgumentAs("defaults", defaults).before {
            whenTrue(XTheme.twitter()) {
                capture("tint").assign(XTheme.brandColor(local(composer)))
                capture("defaults").assign(XTheme.withoutDefault(capture("defaults"), int(1 shl tint)))
            }
        }
    }
    val composer = scrollingTabs.composerAtEntry()
    scrollingTabs.point { invokeDirect { name("<init>"); paramCount(3); hasParam(Type.Long) } }
        .captureArgumentAs("color", 3)
        .before { whenTrue(XTheme.twitter()) { capture("color").assign(XTheme.brandColor(local(composer))) } }
}

// The tab row, and X's default indicator for it, drawn in the theme's primary color.
private val tabRow = klass("tabRow") { strings("currentTabWidth", "indicatorOffset") }
private val themePrimary = method("defaultTabIndicator") {
    inClass(tabRow)
    accesses { owner(themeColors.descriptor) }
}.field("themePrimary") { owner(themeColors.descriptor); type(Type.Long) }

// The scrolling tabs used on Home and Explore construct their own indicator with the content color.
private val scrollingTabs = method("scrollingTabs") {
    calls { owner(tabRow.descriptor) }
    accesses { owner(componentColors.descriptor) }
}

private val currentThemeColors = method("currentThemeColors") {
    returns(themeColors.descriptor)
    params(COMPOSER, Type.Int)
}

private object TwitterBrandingResources

private fun resource(name: String): ByteArray =
    TwitterBrandingResources::class.java.getResourceAsStream("/reseam-x/$name")?.use { it.readBytes() }
        ?: error("reseam-x/$name missing from patch jar resources")

/** Adds a bird drawn like [drawable]: same size and fill, the bird spanning [coverage] of the canvas. */
private fun PatchRuntime.addBirdTwin(drawable: String, coverage: Float): UInt {
    val (viewport, fill) = resources.xml("drawable", drawable).use { xml ->
        val viewport = xml.root["android:viewportWidth"]?.toFloat() ?: error("drawable/$drawable has no viewport")
        viewport to (xml.findByTag("path").firstNotNullOfOrNull { it["android:fillColor"] } ?: error("drawable/$drawable has no fill"))
    }
    val name = "reseam_twitter_$drawable"
    return resources.addFile("drawable", name, "res/drawable/$name.xml", birdVector(viewport, coverage, fill).toByteArray())
}

private fun birdVector(viewport: Float, coverage: Float, fill: String): String {
    val (scale, offset) = placement(viewport, coverage)
    return """<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="${viewport}dp" android:height="${viewport}dp" android:viewportWidth="$viewport" android:viewportHeight="$viewport">""" +
        """<group android:scaleX="$scale" android:scaleY="$scale" android:translateX="$offset" android:translateY="$offset">""" +
        """<path android:fillColor="$fill" android:pathData="$BIRD"/></group></vector>"""
}

/** The scale and centering offset that make the bird span [coverage] of a square [viewport]. */
private fun placement(viewport: Float, coverage: Float): Pair<Float, Float> {
    val scale = viewport * coverage / BIRD_GRID
    return scale to (viewport - BIRD_GRID * scale) / 2
}
