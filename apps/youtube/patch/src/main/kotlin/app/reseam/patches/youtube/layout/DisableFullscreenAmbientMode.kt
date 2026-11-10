// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings

// The colour the player view falls back to when ambient mode is off, and the one the divider
// attributes call the system default.
private const val OPAQUE_BLACK = -16777216

val disableFullscreenAmbientMode = patch(YouTubePatchStrings.disableFullscreenAmbientMode) {
    description(YouTubePatchStrings.disableFullscreenAmbientModeDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, YouTubeStrings.sectionAmbientMode, PlayerSettings.disableFullscreenAmbientMode))

    execute {
        gate(PlayerSettings.disableFullscreenAmbientMode) {
            ambientBackgroundColor.after {
                capture("ambientColor").assign(int(OPAQUE_BLACK))
            }
        }
    }
}

val playerView = klass("com.google.android.apps.youtube.app.player.YouTubePlayerViewNotForReflection")

val playerViewOnLayout = playerView.method("onLayout") {
    params(Type.Boolean, Type.Int, Type.Int, Type.Int, Type.Int)
}

// The ambient arm asks the overlay for a colour and paints the view with it; the plain arm uses a
// constant and is already what this patch wants.
val ambientBackgroundColor = playerViewOnLayout
    .point { invokeInterface { returns(Type.Int) } }
    .next { opcode(Opcode.MOVE_RESULT) }
    .captureAs("ambientColor", Type.Int)
