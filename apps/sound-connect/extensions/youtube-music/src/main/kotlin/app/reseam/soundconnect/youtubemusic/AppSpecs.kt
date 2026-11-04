// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("AppSpecs")

package app.reseam.soundconnect.youtubemusic

import app.reseam.runtime.Export
import org.json.JSONException
import org.json.JSONObject

// Sony's YouTube Music entry opens its Play Store page as a "deeplink"; this launch type starts the app by package.
private const val LAUNCH_BY_PACKAGE = "pkg_name"

/** Sony's linked services list, with YouTube Music's entry pointed at Reseam's YouTube Music while it is installed. */
@Export
fun rewrite(appSpecs: String?): String? {
    if (appSpecs == null || PLAY_STORE_YOUTUBE_MUSIC !in appSpecs) return appSpecs
    // Malformed input goes through unchanged, so the app's own parser reports it as it always has.
    val root = try {
        JSONObject(appSpecs)
    } catch (_: JSONException) {
        return appSpecs
    }
    val specs = root.optJSONArray("appSpecList") ?: return appSpecs
    var changed = false
    for (index in 0 until specs.length()) {
        val spec = specs.optJSONObject(index) ?: continue
        val original = spec.optString("pkgName")
        val packageName = resolve(original)
        if (packageName == original) continue
        spec.put("pkgName", packageName)
        spec.put("launchType", LAUNCH_BY_PACKAGE)
        val launchParam = spec.optString("externalAppLaunchParam")
        spec.put("externalAppLaunchParam", launchParam.replace("\"$original\"", "\"$packageName\""))
        changed = true
    }
    return if (changed) root.toString() else appSpecs
}
