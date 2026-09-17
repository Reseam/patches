// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.ReseamSettings
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val spoofAppVersion = patch("Spoof app version") {
    description("Reports an older app version, which brings back some layouts YouTube has replaced.")
    compatibleWith(YOUTUBE)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Advanced, "App version", YouTubeSettings.spoofAppVersion, YouTubeSettings.spoofAppVersionTarget),
    )

    execute {
        val target = YouTubeSettings.spoofAppVersionTarget
        appVersionName.after(YouTubeSettings.spoofAppVersion) {
            capture("appVersionName").assign(call(ReseamSettings.getString, string(target.key), string(target.default)))
        }
    }
}

// The app reads its own version once, caches it, and reports that copy everywhere.
val appVersionSource = method("appVersionSource") {
    strings("pref_override_build_version_name")
    returns(Type.String)
    params(Type.Context)
}

// The field is read twice: once to null-check it, then into the register that is returned.
val appVersionName = appVersionSource
    .point { field { owner("android.content.pm.PackageInfo"); name("versionName") } }
    .next { field { owner("android.content.pm.PackageInfo"); name("versionName") } }
    .captureAs("appVersionName", Type.String)
