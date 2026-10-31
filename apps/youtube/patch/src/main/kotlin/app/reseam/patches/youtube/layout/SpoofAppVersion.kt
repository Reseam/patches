// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.AdvancedSettings
import app.reseam.youtube.settings.YouTubeStrings

val spoofAppVersion = patch("Spoof app version") {
    description("Reports an older app version, which brings back some layouts YouTube has replaced.")
    compatibleWith(YOUTUBE)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Advanced, YouTubeStrings.sectionAppVersion, AdvancedSettings.spoofAppVersion, AdvancedSettings.spoofAppVersionTarget),
    )

    execute {
        val target = AdvancedSettings.spoofAppVersionTarget
        gate(AdvancedSettings.spoofAppVersion) {
            appVersionName.after {
                capture("appVersionName").assign(valueOf(target))
            }
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
