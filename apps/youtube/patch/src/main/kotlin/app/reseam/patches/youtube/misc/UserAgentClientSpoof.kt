// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.after
import app.reseam.patch.dex.stringValue
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private val userAgentPackageNameSites = methods("user-agent package name sites") {
    strings("(Linux; U; Android ")
    calls {
        owner("Landroid/content/Context;")
        name("getPackageName")
        params()
        returns("Ljava/lang/String;")
    }
    calls { owner("Ljava/lang/StringBuilder;"); name("append"); params("Ljava/lang/String;") }
    custom {
        instructions.none { it.stringValue == "android.resource://" || it.stringValue?.contains("gcore_") == true }
    }
}

val userAgentClientSpoof = patch("User-agent client spoof") {
    description("Keeps client user-agent package names compatible with YouTube's original package.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Stream compatibility", YouTubeSettings.userAgentClientSpoof))

    execute {
        userAgentPackageNameSites.forEach {
            val packageName = point("user-agent package name") {
                invokeVirtual {
                    owner("android.content.Context")
                    name("getPackageName")
                    params()
                    returns("java.lang.String")
                }
            }.next { resultOf("java.lang.String") }
                .captureAs("packageName", "java.lang.String")
            packageName.after {
                capture("packageName").assign(call(UserAgentClientSpoof.rewritePackageName, capture("packageName")))
            }
        }
    }
}
