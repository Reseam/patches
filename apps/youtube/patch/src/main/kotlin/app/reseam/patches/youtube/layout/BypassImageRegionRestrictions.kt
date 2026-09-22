// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.imageRequestConstructor

val bypassImageRegionRestrictions = patch("Bypass image region restrictions") {
    description("Loads avatars and channel images from a host that is not blocked in some countries.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, "Images", YouTubeSettings.bypassImageRegionRestrictions))

    execute {
        // The single-argument constructor delegates here, so one hook covers every image load.
        imageRequestConstructor.before {
            param(0).assign(call(ImageUrl.override, param(0)))
        }
    }
}

object ImageUrl : ExtClass("app.reseam.youtube.misc.ImageUrl") {
    val override = static("override", Type.String, returns = Type.String)
}
