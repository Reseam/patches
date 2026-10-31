// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.seekbarColor
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings

val hideSeekbar = patch {
    compatibleWith(YOUTUBE)
    dependsOn(seekbarColor)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Controls,
            YouTubeStrings.sectionSeekbar,
            PlayerControlsSettings.hideSeekbar,
            PlayerControlsSettings.hideSeekbarThumbnail,
            PlayerControlsSettings.seekbarCustomColor,
            PlayerControlsSettings.seekbarCustomColorPrimary,
            PlayerControlsSettings.seekbarCustomColorAccent,
        ),
    )

    execute {
        gate(PlayerControlsSettings.hideSeekbar) {
            seekbarOnDraw.before { returnVoid() }
        }
    }
}

internal val seekbarOnDraw = method("seekbarOnDraw") {
    name("onDraw")
    params("android.graphics.Canvas")
    returns(Type.Void)
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    strings("timed_markers_width")
}
