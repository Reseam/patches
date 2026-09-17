// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.seekbarColor

val hideSeekbar = patch {
    compatibleWith(YOUTUBE)
    dependsOn(seekbarColor)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Controls,
            "Seekbar",
            YouTubeSettings.hideSeekbar,
            YouTubeSettings.hideSeekbarThumbnail,
            YouTubeSettings.seekbarCustomColor,
            YouTubeSettings.seekbarCustomColorPrimary,
            YouTubeSettings.seekbarCustomColorAccent,
        ),
    )

    execute {
        seekbarOnDraw.before(YouTubeSettings.hideSeekbar) { returnVoid() }
    }
}

internal val seekbarOnDraw = method("seekbarOnDraw") {
    name("onDraw")
    params("android.graphics.Canvas")
    returns(Type.Void)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    strings("timed_markers_width")
}
