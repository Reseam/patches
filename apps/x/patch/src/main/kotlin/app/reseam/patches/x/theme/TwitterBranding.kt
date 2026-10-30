// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.theme

import app.reseam.patch.PatchRuntime

// The Twitter bird on a 24-unit grid, from Simple Icons 9.21.0 (CC0-1.0).
private const val BIRD =
    "M21.543 7.104c.015.211.015.423.015.636 0 6.507-4.954 14.01-14.01 14.01v-.003A13.94 13.94 0 0 1 0 19.539a9.88 " +
        "9.88 0 0 0 7.287-2.041 4.93 4.93 0 0 1-4.6-3.42 4.916 4.916 0 0 0 2.223-.084A4.926 4.926 0 0 1 .96 9.167v-.062a4.887 " +
        "4.887 0 0 0 2.235.616A4.928 4.928 0 0 1 1.67 3.148 13.98 13.98 0 0 0 11.82 8.292a4.929 4.929 0 0 1 8.39-4.49 9.868 " +
        "9.868 0 0 0 3.128-1.196 4.941 4.941 0 0 1-2.165 2.724A9.828 9.828 0 0 0 24 4.555a10.019 10.019 0 0 1-2.457 2.549z"
private const val BIRD_GRID = 24f
private const val TWITTER_BLUE = "#FF1D9BF0"
private const val LAUNCHER_BACKGROUND = "reseam_twitter_blue"
private const val SPLASH_ICON = "reseam_twitter_splash"

/** The Twitter name, the bird for X's logos, and Twitter blue on the launcher icon and splash screen. */
internal fun PatchRuntime.applyTwitterBranding() {
    resources.setString("APP_NAME", "Twitter")

    // X's logos, drawn as the bird at the share of the canvas the X took. Their fill stays, so
    // the app keeps tinting them as before.
    drawBird("ic_vector_twitter", coverage = 1f)
    drawBird("ic_vector_x", coverage = 1f)
    drawBird("ic_logo_x", coverage = 0.84f)
    drawBird("ic_stat_x", coverage = 0.9f)
    drawBird("ic_launcher_foreground", coverage = 0.38f)

    // Since 12.34 Home's top-bar logo is a Lottie that animates on pull to refresh; the bird keeps its markers.
    if (resources.exists("raw", "x_refresh_logo")) {
        files.write(resources.path("raw", "x_refresh_logo"), resource("twitter_refresh_logo.json"))
    }

    resources.addColor(LAUNCHER_BACKGROUND, TWITTER_BLUE)
    for (icon in listOf("ic_launcher", "ic_launcher_round")) {
        resources.editXml("mipmap", icon) {
            root.children.single { it.tag == "background" }["android:drawable"] = "@color/$LAUNCHER_BACKGROUND"
        }
    }

    resources.addFile("drawable", SPLASH_ICON, "res/drawable/$SPLASH_ICON.xml", birdVector(108f, 0.45f, TWITTER_BLUE).toByteArray())
    resources.style("Theme.X.SplashScreen") { this["windowSplashScreenAnimatedIcon"] = "@drawable/$SPLASH_ICON" }
}

private object TwitterBrandingResources

private fun resource(name: String): ByteArray =
    TwitterBrandingResources::class.java.getResourceAsStream("/reseam-x/$name")?.use { it.readBytes() }
        ?: error("reseam-x/$name missing from patch jar resources")

private fun PatchRuntime.drawBird(drawable: String, coverage: Float) {
    resources.editXml("drawable", drawable) {
        val viewport = root["android:viewportWidth"]?.toFloat() ?: error("drawable/$drawable has no viewport")
        val fill = findByTag("path").firstNotNullOfOrNull { it["android:fillColor"] } ?: error("drawable/$drawable has no fill")
        root.children.forEach { it.remove() }
        val (scale, offset) = placement(viewport, coverage)
        val group = createElement("group").apply {
            this["android:scaleX"] = "$scale"
            this["android:scaleY"] = "$scale"
            this["android:translateX"] = "$offset"
            this["android:translateY"] = "$offset"
        }
        group.appendChild(createElement("path").apply {
            this["android:fillColor"] = fill
            this["android:pathData"] = BIRD
        })
        root.appendChild(group)
    }
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
